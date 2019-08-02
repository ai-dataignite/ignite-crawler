package com.onycom.crawler.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.onycom.SettingBasedCrawler.Crawler;
import com.onycom.common.Util;
import com.onycom.crawler.data.Duplicate;
import com.onycom.crawler.data.Work;

/**
 * Get URL 방식의 정적 웹페이지를 파싱하는 구현체
 */
public class StaticParser extends DefaultParser {
	
	public StaticParser(){
		
	}
	
	@Override
	public List<Work> parseURL(Work urlInfo, Document document) {
		Map<String, Work> map = new HashMap<String, Work>();
		List<Work> ret = new ArrayList<Work>();
		boolean allow = false;
		Work newInfo;
		String href, domain_url, sub_url, url;
		String[] tmp;
		
		int curDepth = urlInfo.getDepth(); 
		
		if(ifLeaf(urlInfo)){
			return ret;
		}
		// ****************************
		// url 중복에 대한 커스텀도 필요할 것으로 보임
		// ****************************
		Elements els = document.select("a[href]");
		for(Element e : els){
			href = e.attr("href").trim();
			if(href.length() == 0) continue;
			tmp = Util.SplitDomainAndSubURL(urlInfo, href);
			domain_url = tmp[0];
			sub_url = tmp[1];
			url = domain_url + sub_url;
			allow = isAllow(urlInfo, domain_url, sub_url);
			
			if(allow){
				newInfo = map.get(url); 
				if(newInfo == null){
					url = super.filterModify(url);
					newInfo = new Work(url, mConfig.CHARACTER_SET).setDepth(++curDepth);
					map.put(url, newInfo);
					ret.add(newInfo.setScraper(urlInfo.getScraper()).setParser(urlInfo.getParser()));
				}
			}else{
//				System.err.println(">> href - " + href);
				//Crawler.DB.insertFilter(urlInfo.getURL(), Util.ConvertForString(href));
//				System.err.println(">> href - " + href);
//				System.err.println(domain_url +" - "+ sub_url);
				//ret.add(new URLInfo(url));
			}
		}
		return ret;
	}

	@Override
	public List<Work> checkDupliate(Work[] aryHistory, List<Work> aryNewUrl) {
		/* 새로운 URL 내에서의 중복 체크 */
		if(aryNewUrl != null && aryNewUrl.size() > 0){
			aryNewUrl = removeDupliacateInNewURL(aryNewUrl);
		}
		
		/* Q history 와 새로운 URL의 중복 체크 */
		if(aryNewUrl != null && aryNewUrl.size() > 0){
			aryNewUrl = filterDuplicate(aryHistory, aryNewUrl);
		}
		
		return aryNewUrl;
	}
	
	private List<Work> removeDupliacateInNewURL(List<Work> aryNewUrl){
		Work src, des;
		String regex;
		List<Duplicate> aryFilterDuplicate = mConfig.getFilterDuplicate();
		//System.out.println("before remove : " + aryNewUrl.size());
		for(int i = 0 ; i < aryNewUrl.size() ; i++){
			src = aryNewUrl.get(i);
			for(Duplicate dup : aryFilterDuplicate){
				regex = dup.regex(src);
				if(regex == null) continue;
				for(int j = i+1 ; j < aryNewUrl.size() ; j++){
					des = aryNewUrl.get(j);
					if(des.getURL().matches(regex)){
						aryNewUrl.remove(j);
						j--;
					}
				}
			}
		}
		//System.out.println("after remove : " + aryNewUrl.size());
		return aryNewUrl;
	}

	private List<Work> filterDuplicate(Work[] aryHistory, List<Work> aryNewUrl){ 
		String historyUrl;
		boolean isDuplicate = false;
		Work newURLInfo;
		String regex;
		int cut = 0;
		List<Duplicate> aryFilterDuplicate = mConfig.getFilterDuplicate();
		//mFilterDuplicate
		
		/* History 와 새로운 URL 중복 체크 */
		for(int i = aryNewUrl.size()-1 ; i >= 0 ; i--){
			newURLInfo = aryNewUrl.get(i);
			isDuplicate = false;
			for(Work history: aryHistory){
//				
				historyUrl = history.getURL();
				for(Duplicate dup : aryFilterDuplicate){
					regex = dup.regex(newURLInfo);
					if(regex == null) continue;
					if(historyUrl.matches(regex)){
						isDuplicate = true;
						break;
					}
				}
				if(isDuplicate){
					break;
				}
			}
			if(isDuplicate){
				cut ++;
				//System.out.println(newURLInfo.getURL());
				aryNewUrl.remove(i);
			}else{
				//Crawler.DB.writeHistory(newURLInfo.getURL());
			}
		}
		return aryNewUrl;
	}
}
