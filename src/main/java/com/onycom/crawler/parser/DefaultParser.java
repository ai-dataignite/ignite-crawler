package com.onycom.crawler.parser;

import java.io.File;
import java.sql.SQLNonTransientConnectionException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.google.gdata.util.common.html.HtmlToText;
import com.onycom.SettingBasedCrawler.Crawler;
import com.onycom.common.CrawlerLog;
import com.onycom.common.Util;
import com.onycom.crawler.data.Action;
import com.onycom.crawler.data.CollectRecode;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.Dictionary;
import com.onycom.crawler.data.FilterModify;
import com.onycom.crawler.data.Work;

/**
 * 웹페이지 파싱 인터페이스를 위한 추상 클래스
 * <p>
 * <b>T</b> : 스크랩해야할 사이트 객체
 * <p>
 * <b>C</b> : 저장할 콘텐츠 객체
 */
public abstract class DefaultParser implements Parser<Work, Contents> {
	static Logger mLogger = CrawlerLog.GetInstance(DefaultParser.class);

	Config mConfig;

	/**
	 * http 로 시작하는 패턴. (세팅파일에 정규식을 넣기 때문에, 정확한 연산을 위해 정규식을 제외하는 로직 필요하지 않을까 생각됨)
	 */
	public static final String REGEX_START_HTTP = "^.*(http).*$";

	/**
	 * URL을 수집하는 메서드. 설정된 정규식에 의해 불필요한 URL들을 필터링하는 로직 구현
	 * <p>
	 * 
	 * @param work 처리할 웹페이지의 URL 정보
	 * @param dom  처리할 HTML 문서
	 * @return 중복이 제거된 URL 목록
	 */
	public abstract List<Work> parseURL(Work work, Document dom);

	/**
	 * 설정 적용
	 * <p>
	 * 
	 * @param config 설정 정보를 담고 있는 객체
	 */
	public void setConfig(Config config) {
		mConfig = config;
	}

	public Config getConfig() {
		return mConfig;
	}

	/**
	 * 기본 파싱 절차 콘텐츠 파싱 -> 콘텐츠 저장 -> URL 파싱 -> URL 중복처리
	 * <p>
	 * 
	 * @param history  현재까지 수집된 모든 URL 목록 (중복된 URL에 접근하지 않기 위해서 비교하기위해 가져옴)
	 * @param work     처리할 웹페이지의 URL 정보
	 * @param document 처리할 html 페이지
	 * @return 스크랩할 URL 목록. Q 에 전달됨
	 */
	public List<Work> parse(Work[] history, Work work, Document document) {
		List<Work> retWorks = null;
		int retCnt;
		if (document != null) {
			Date startTime = new Date();
			List<Contents> contents = parseContents(work, document);
			// System.out.println("[parseContents expire : ] " +
			// Util.CalcExpiredTime(startTime));

			startTime = new Date();
			retCnt = saveContents(work, contents);
			work.result().setSaveCount(retCnt);
			// System.out.println("[saveContents expire : ] " +
			// Util.CalcExpiredTime(startTime));

			startTime = new Date();
			retWorks = parseURL(work, document);
			// System.out.println("[parseURL expire : ] " +
			// Util.CalcExpiredTime(startTime));

			startTime = new Date();
			retWorks = checkDupliate(history, retWorks);
			// mLogger.debug("[checkDupliate expire : ] " +
			// Util.CalcExpiredTime(startTime));
			work.result().success();
		}
		return retWorks;
	}

	/**
	 * 콘텐츠가 동일한 웹페이지들을 필터링 하기 위한 메서드. URL이 동일하지 않더라도 콘텐츠는 동일한 경우가 존재하며, 그 외의 다양한
	 * 케이스들을 고려한 구현 필요
	 * <p>
	 * 가장 로드가 많이 걸리는 메서드기도 함. 크롤링된 웹페이지가 1,000건 이상일 때부터 평균 처리속도가 1초를 넘어가기 시작하며 최적화가
	 * 필요해 보임
	 * <p>
	 * 
	 * @param history 현재까지 수집된 URL 목록. Q 에서 전달받음.
	 * @param newList 새로 수집된 URL 목록.
	 * @return 중복이 제거된 URL 목록
	 */
	public List<Work> checkDupliate(Work[] history, List<Work> newList) {
		return newList;
	}

	/**
	 * 콘텐츠를 수집하는 메서드. 설정된 CSS 패턴에 의해서 데이터를 찾고 데이터 객체에 저장하는 로직 구현
	 * <p>
	 * 
	 * 콘텐츠를 파싱한 실패 시 NULL 을 반환하지만 이를 처리하는 로직 없음 해당 로직은 이 메서드를 호출하는 parser 에서 처리해야 할
	 * 것
	 * 
	 * @param work     처리할 웹페이지의 URL 정보
	 * @param document 처리할 HTML 문서
	 * @return 콘텐츠 데이터 객체 목록
	 */
	public List<Contents> parseContents(Work work, Document document) {
		String currentURL = work.getURL();
		String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String txt, type, attr_name, element_type, tag_type, data_type, data_name, value = null;
		String[] regexs;
		String regex_replace;
		// boolean isAllow = false;
		Elements recodeEls, colEls;
		List<Contents> aryContents = null;
		Contents contents;

		List<CollectRecode> aryRecode = mConfig.getCollects();
		Dictionary dict = mConfig.getDictionary();
		HashMap<String, Boolean> mapKeyword = null;

		if (dict != null && dict.hasDict()) {
			mapKeyword = new HashMap<String, Boolean>();
			for (int i = 0; i < dict.getKeyWordList().size(); i++) {
				mapKeyword.put(dict.getKeyWordList().get(i), false);
			}
		}

		Pattern pattern;
		Matcher matcher;
		boolean isParsingDocument = false;
		boolean inDict = false;
		int contentsDepth = 0;
		aryContents = null;
		CollectRecode.Column keywordColumn = null;
		for (CollectRecode recode : aryRecode) {
			// mLogger.debug("[Visiting page] " + document.title() + " @ " + work.getURL());
			contents = null;
			if (work.getAction() != null) {
				contentsDepth = work.getAction().getContentsDepth();
			}
			// || (work.getAction() != null &&
			// work.getAction().getType().equalsIgnoreCase(Action.TYPE_PARSE_CONTENTS))
			
			if (recode.getDepth() == contentsDepth
					&& (work.getURL().matches(recode.getUrl()) || (work.getAction() != null
							&& work.getAction().getType().equalsIgnoreCase(Action.TYPE_PARSE_CONTENTS)))) {
				isParsingDocument = true;
				/* N건 배열 데이터 파싱 */
				if (recode.getRecodeSelector() != null && !recode.getRecodeSelector().isEmpty()) {
					recodeEls = document.select(recode.getRecodeSelector());
					if (recodeEls.size() > 0)
						aryContents = new ArrayList<Contents>(recodeEls.size());
					for (Element recodeEl : recodeEls) {
						contents = new Contents(recode.getName(), recode.getColumns().size());
						contents.setInsertOrUpdate(recode.getInsertORUpdate());
						for (CollectRecode.Column collectCol : recode.getColumns()) {
							type = collectCol.getType();
							data_type = collectCol.getDataType();
							data_name = collectCol.getDataName();
							regexs = collectCol.getRegexFilter();
							regex_replace = collectCol.getRegexReplace();
							value = null;
							colEls = null;
							if (type.equalsIgnoreCase(Config.COLLECT_COLUMN_TYPE_URL)) {
								value = currentURL;
								value = filterRegex(regexs, regex_replace, value);
							} else if (type.equalsIgnoreCase(Config.COLLECT_COLUMN_TYPE_DATETIME)) {
								value = currentDateTime;
								value = filterRegex(regexs, regex_replace, value);
							} else if (type.equalsIgnoreCase(Config.COLLECT_COLUMN_TYPE_TEXT)) {
								value = collectCol.getValue();
								value = filterRegex(regexs, regex_replace, value);
							} else if (type.equalsIgnoreCase(Config.COLLECT_COLUMN_TYPE_ELEMENT)) {
								if (collectCol.getElements() != null && collectCol.getElements().length > 0) {
									/* Column 찾는 selector 가 여러개 일 때, 가장 먼저 나오는 것만 파싱 */
									for (CollectRecode.Column.Element collectElement : collectCol.getElements()) {
										if (collectElement.isFromRoot()) {
											colEls = document.select(collectElement.getSelector());
										} else {
											colEls = recodeEl.select(collectElement.getSelector());
										}
										attr_name = collectElement.getAttrName();
										element_type = collectElement.getType();
										txt = null;
										/* 찾는데이터가 한개가 아니라 여러개 일때, 쉼표로 구분해서 저장 */
										for (Element colEl : colEls) {
											if (element_type.equalsIgnoreCase("attr")) {
												txt = colEl.attr(attr_name);
											} else if (element_type.equalsIgnoreCase("html")) {
												txt = colEl.html();
											} else { // text
												txt = Util.htmlToPlainText(colEl.html());
//												txt = colEl.text();
											}
											if (txt != null && !txt.isEmpty()) {
												if (value != null) {
													value += "," + txt;
												} else {
													value = txt;
												}
											}
										}
										/* 데이터를 찾았으면 중단 */
										if (value != null) {
											System.out.println(value);
											if (dict != null && dict.hasDict()) {
												checkInDict(mapKeyword, value);
											}

											/* 데이터 전처리 */
											value = Util.Remove4ByteEmoji(value);
											/* regex 필터링 */

											value = filterRegex(regexs, regex_replace, value);
											break;
										}

									}
								}
							} else if (type.equalsIgnoreCase(Config.COLLECT_COLUMN_TYPE_KEYWORD)) {
								keywordColumn = collectCol;
								continue;
							}
							if (value != null) {
								System.out.println("=============");
								System.out.println(value);
								contents.add(data_name, data_type, value);
							} else {
								if (colEls != null && colEls.size() > 0) {
									/* 엘리먼트를 찾았는데 데이터가 empty 거나 필터링에 걸린 것 */
								} else {
									/* 엘리먼트를 찾을 수 없음 : 오류 */
									if (!collectCol.isAllowNull()) {

										if (collectCol.getElements() != null) {
											for (CollectRecode.Column.Element collectElement : collectCol
													.getElements()) {
												work.result().addError(Work.Error.ERR_CONTENTS_COL,
														collectCol.getDataName() + ", " + collectElement.getSelector()
																+ ", " + collectElement.getType(),
														null);
												// mLogger.error("-> " + collectElement.getSelector() +" @ " +
												// collectElement.getType());
											}
										} else {
											work.result().addError(Work.Error.ERR_CONTENTS_COL,
													collectCol.getDataName() + ", " + collectCol.getType(), null);
										}
									}
								}
							}
						}
						if (contents.size() > 0) {
							if (dict != null && dict.hasDict()) {
								String strDicts = dictToString(mapKeyword);
								if (strDicts != null) {
									System.out.println("[Dict] " + strDicts);
									if (keywordColumn != null) {
										contents.add(keywordColumn.getDataName(), keywordColumn.getType(), strDicts);
									}
									aryContents.add(contents);
								}
							} else {
								aryContents.add(contents);
							}
						}

					}
				}
			}

			if (getConfig().SAVE_HTML && isParsingDocument) {
				File f = new File(Config.DEAULT_HTML_FILE_PATH + "/" + mConfig.CRAWLING_NAME_AND_TIME);
				if (!f.exists()) {
					f.mkdirs();
				}
				String strTime = new SimpleDateFormat(Config.DATETIME_FORMAT)
						.format(new Date(System.currentTimeMillis()));
				strTime = f.getPath() + "/" + mConfig.CRAWLING_NAME + "_" + strTime + ".html";
				if (!Util.WriteFile(strTime, document.html())) {
					work.result().addError(Work.Error.ERR, "Can't save the html file.", null);
				}
			}
		}
		return aryContents;
	}

	/**
	 * 수집된 콘텐츠를 저장하는 메서드. DB, CSV, XML에 따라 저장하는 로직 구현
	 * <p>
	 * 
	 * @param urlInfo     처리할 웹페이지의 URL 정보
	 * @param aryContents 저장할 콘텐츠 배열
	 * @return 저장된 콘텐츠 개수
	 */
	public int saveContents(Work urlInfo, List<Contents> aryContents) {
		int idx = 0;
		if (aryContents != null) {
			int try_connect = 0;
			boolean loop = true;
			for (Contents contents : aryContents) {
				if (contents != null) {
					while(loop) {
						try {
							idx += Crawler.Writer.write(contents);
							loop = false;
						} catch (Exception e) {
							if (e.getClass().getName().equalsIgnoreCase("java.sql.SQLNonTransientConnectionException") ||
									e.getClass().getName().equalsIgnoreCase("java.net.ConnectException")) {	
								try_connect++;
								mLogger.info("retry to conn writer.. (" + try_connect + "/3)");
								if(try_connect == 3) {
									urlInfo.result().addError(Work.Error.ERR_WRITE, "Contents write failed : " + e.getMessage(), e);
									break;
								}
								try {
									Thread.sleep(5000 * (try_connect));
								} catch (InterruptedException e1) { }
							}else {
								urlInfo.result().addError(Work.Error.ERR_WRITE, "Contents write failed : " + e.getMessage(), e);
								break;
							}
						}
					}
				}
			}
		}
		return idx;
	}

	/**
	 * 정규표현식을 처리하는 메서드 데이터안에서 정규표현식에 맞는 데이터만 필터링해서 가져오고자 할때 사용
	 */
	private String filterRegex(String[] regexs, String replace, String value) {
		/* regex 필터링 */
		Pattern pattern;
		Matcher matcher;
		String str = null;
		if (regexs != null && regexs.length > 0 && value != null && !value.isEmpty()) {
			for (String regex : regexs) {
				pattern = Pattern.compile(regex);
				matcher = pattern.matcher(value);
				value = null;
				while (matcher.find()) {
					str = matcher.group();
					if (replace != null) {
						str = str.replaceAll(regex, replace);
					}
					if (value == null && str != null) {
						value = str;
					} else {
						value += str;
					}
				}
			}
		}
		return value;
	}

	/**
	 * 추출된 데이터 안에 단어사전 키워드가 있는지 검색 구현 발단은 검색어를 사용하지 못하고 실제 데이터에서 찾아봐야할때 사용
	 */
	private boolean checkInDict(Map<String, Boolean> mapDict, String data) {
		if (mapDict == null)
			return false;
		boolean ret = false, ok = false;
		Iterator<Entry<String, Boolean>> iterator = mapDict.entrySet().iterator();
		Entry<String, Boolean> e;
		char c, k;

		while (iterator.hasNext()) {
			e = iterator.next();
			if (!e.getValue()) {
				int idx = data.toLowerCase().indexOf(e.getKey().toLowerCase());
				if (idx != -1) {{
					
				}
					ok = true;

					// 영어 단어 검색시 유효한 단어 검색을 위해서 키워드 앞,뒤자리 단어가 같은 유형의 언어면 검색에서 제외함
					// ex) daily 에서 ai 를 검색하지 "않기" 위해
					if (idx > 0) {
						c = data.charAt(idx - 1);
						k = e.getKey().charAt(0);

						// data.charAt(idx-1) key의 첫글자 한글이면 한글이 아닐때 ok
						// data.charAt(idx-1) key의 첫글자 영어면 영어이 아닐때 ok

						if (Util.CheckEng(k)) {
							if (Util.CheckEng(c)) {
								ok = false;
							}
						}
					}

					if (idx < data.length() - e.getKey().length()) {
						c = data.charAt(idx + e.getKey().length());
						k = e.getKey().charAt(e.getKey().length() - 1);
						// data.charAt(idx+e.getKey().length()) key 마지막 글자 한글이면 한글이 아닐때 ok
						// data.charAt(idx+e.getKey().length()) key 마지막 글자 영어면 영어이 아닐때 ok
						if (Util.CheckEng(k)) {
							if (Util.CheckEng(c)) {
								ok = false;
							}
						}

					}
					if (ok) {
						mapDict.put(e.getKey(), true);
						ret = true;
					}
				}
			} else {
				ret = true;
			}
		}
		return ret;
	}

	private String dictToString(Map<String, Boolean> mapDict) {
		String ret = null;
		Iterator<Entry<String, Boolean>> iterator = mapDict.entrySet().iterator();
		Entry<String, Boolean> e;
		while (iterator.hasNext()) {
			e = iterator.next();
			if (e.getValue()) {
				if (ret != null) {
					ret += "," + e.getKey();
				} else {
					ret = e.getKey();
				}
			}
		}
		return ret;
	}

	public boolean isAllow(Work curUrlInfo, String targetDomain, String targetSub) {
		boolean ret = true;
		List<String> aryFilterAllow = mConfig.getFilterAllow();
		List<String> aryFilterDisallow = mConfig.getFilterDisallow();

		String url = targetDomain + targetSub;
		for (String filter : aryFilterAllow) {
			if (filter.matches(DefaultParser.REGEX_START_HTTP)) {
				if (url.matches(filter)) {
					ret = true;
					break;
				}
			} else {
				if (targetDomain.contentEquals(curUrlInfo.getDomainURL())) {
					if (targetSub.matches(filter)) {
						ret = true;
						break;
					}
				}
			}
		}

		for (String filter : aryFilterDisallow) {
			if (filter.matches(DefaultParser.REGEX_START_HTTP)) {
				if (url.matches(filter)) {
					ret = false;
					break;
				}
			} else {
				if (targetDomain.contentEquals(curUrlInfo.getDomainURL())) {
					if (targetSub.matches(filter)) {
						ret = false;
						break;
					}
				}
			}
		}
		
		return ret;
	}
	
	public String filterModify(String url) {
		List<FilterModify> aryFtilerModify = mConfig.getFtilerModify();
		for (FilterModify filter : aryFtilerModify) {
			url = filter.regex(url);
		}
		return url;
	}

	public boolean ifLeaf(Work urlInfo) {
		if (mConfig.CRAWLING_MAX_DEPTH != -1) {
			if (mConfig.CRAWLING_MAX_DEPTH < urlInfo.getDepth()) {
				return true;
			}
		}

		List<String> aryLeafURL = mConfig.getLeafURL();
		if (aryLeafURL != null) {
			for (String filter : aryLeafURL) {
				if (filter.matches(DefaultParser.REGEX_START_HTTP)) {
					if (urlInfo.getURL().matches(filter)) {
						// System.err.println(">> leaf - " + urlInfo.getURL());
						// Crawler.DB.insertErr(urlInfo.getURL(), "leaf" );
						return true;
					}
				} else {
					if (urlInfo.getSubURL().matches(filter)) {
						// System.err.println(">> leaf - " + urlInfo.getURL());
						// Crawler.DB.insertErr(urlInfo.getURL(), "leaf" );
						return true;
					}
				}
			}
		}
		return false;
	}

	class Type {
		public byte URL_FILTER = 0x0;
		public byte URL_SCENARIO = 0x1;
		public byte ACTION_SCENARIO = 0x2;
	}
}
