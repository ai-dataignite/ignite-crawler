package com.hdone.crawler.data;

import java.beans.Encoder;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hdone.common.Util;
import com.hdone.crawler.parser.Parser;
import com.hdone.crawler.scraper.Scraper;

/**
 * 크롤링 작업 오브젝트
 * 
 * url + aciton + 작업의 결과까지 저장함 </p>
 * url : 방문할 URL 및 request 값 저장</p>
 * action : selenium 에 넘길 동작 값 저장</p>
 * result : 크롤링 결과를 저장
 * */ 
public class Work {
	public static final byte POST = 0x0;
	public static final byte GET = 0x1;
	
	public static final int STATE_IDLE = 0x0;
	public static final int STATE_WORKING = 0x1;
	public static final int STATE_FAIL = 0x2;
	public static final int STATE_SUCCESS = 0x3;
	
	public static final byte PARSE_NORMAL = 0x0;
	public static final byte PARSE_SCENARIO = 0x1;
	public static final byte PARSE_FIND_ACTION =  0x2;
	
	int mState = STATE_IDLE;

	boolean mHighPriority = false;
	
	byte mContentType;
	URL mURL = null;
	String mDomainURL = null;
	String mSubUrl = null;
	String mEncodingType = "UTF-8";
	
	byte mParseType = PARSE_NORMAL;
	Action mAction;
	String mParentWindow;
	
	List<String> mLoadCheckSelector;
	Result mResult;
	
	// 현재는 쓰지 않지만 향후에 써야할 가능성도 있을것으로 보임
	// Map<String, String> mHeader;
	
	Map<String, String> mDataMap;
	
//	char[] URL_SPLIT_CHAR = {'/','?','#'};
	
	/**
	 * 시드 URL으로부터 몆 번 링크 타고 들어갔는지 횟수
	 * 크롤러의 검색 범위를 제한하고자 할때 사용한다. 
	 * 0 은 시드 URL을 의미
	 * */ 
	int mDepth = 0;

	Parser mParser;
	Scraper mScraper;

	
	/**
	 * url 값은 반드시 http와 루트 도메인 주소가 포함된 전체 주소여야 한다.
	 * 그 외의 형식이라면 NULL 반환
	 * */
	public Work(String url, String encodingType){
		setURL(url, encodingType);
	}

	public Work(String url){
		setURL(url, mEncodingType);
	}	
	
	public Work setParser(Parser p){
		mParser = p;
		return this;
	}

	public Work setScraper(Scraper s){
		mScraper = s;
		return this;
	}

	public Parser getParser(){
		return mParser;
	}

	public Scraper getScraper() {
		return mScraper;
	}
	
	public void setURL(String strURL){
		setURL(strURL, mEncodingType);
	}

	public void setURL(String strURL, String encodingType){
		strURL = strURL.trim();
		
		/* 마지막 문자가 "/" 이면 삭제할 것
		 * 모든 URL 연산시 마지막 / 은 없다고 가정하고 진행하기 위해서
		 * */
		String lastChar = strURL.substring(strURL.length()-1);
		while(true){
			if(lastChar.equalsIgnoreCase("/")){
				strURL = strURL.substring(0, strURL.length()-1);
				lastChar = strURL.substring(strURL.length()-1);
			}else{
				break;
			}
		}
		String host;
		String path;
		String query;
		String protocol;
		int port;
		
		try {
			URL url = new URL(strURL);
			
			port = url.getPort();
			protocol = url.getProtocol();
			host = url.getHost();
			path = url.getPath();
			query = url.getQuery();
			
			if(!protocol.isEmpty() && !host.isEmpty()){
				mDomainURL = protocol + "://" + host;
			}
			
			if(port != -1){
				mDomainURL += (":" + port);
			}
			if(path != null && !path.isEmpty() && !path.contentEquals("/")){
				mSubUrl = path;
				
			}
			
			if(query!= null && !query.isEmpty()){
				/* ? 쿼리스트링 데이터 파싱 */
				String[] row = query.split("&");
				String[] data;
				for(String set : row){
					data = set.split("=");
					if(data.length == 2){
						setData(data[0], data[1], encodingType);
					}
				}
			}
			mURL = url;
		} catch (MalformedURLException e) {
			mURL = null;
		}
		
		/* 8 의미 - http:// 또는  htts:// 를 제외하기 위한 길이 */
		/* 추가 구현 고려 사항 : 주소 인코딩에 대한 고려도 있어야 할 것으로 보임 */
//		if(url != null && url.length() > 8 && url.matches("http(.*)")){
//			/* # 처리 : 페이지 내에 인덱싱 기능이므로 삭제해버린다 */
//			int idx = url.indexOf('#', 8);
//			if(idx != -1){
//				url = url.substring(0, idx);
//			}
//			
//			/* 도메인과 서브 주소를 분류 */
//			idx = url.indexOf('/', 8);
//			if(idx != -1){
//				mDomainURL = url.substring(0, idx);
//				mSubUrl = url.substring(idx);
//			}else{
//				mDomainURL = url;
//				mSubUrl = "/";
//			}
//			
//			/* 쿼리스트링 분류 */
//			String query = null;
//			if (mSubUrl.contentEquals("/")){
//				idx = mDomainURL.indexOf('?', 8);
//				if(idx != -1){
//					if(mDomainURL.length() >= (idx+1)){
//						query = mDomainURL.substring(idx+1);
//					}
//					mDomainURL = mDomainURL.substring(0, idx);
//				}
//			}else{
//				idx = mSubUrl.indexOf('?', 8);
//				if(idx != -1){
//					query = mSubUrl.substring(idx+1);
//					mSubUrl = mSubUrl.substring(0, idx);
//				}
//			}
			
			
			/* 주소 인코딩에 대한 고려도 있어야 할 것으로 보임 */
//			try {
//				//mUrl = URLEncoder.encode(url, "UTF-8");
//				// 이미 인코딩된 주소라면  JSOUP이 
//				// 다시 인코딩에서 사용하기 떄문에 디코딩해서 저장함 
//				url = URLDecoder.decode(url, "UTF-8");
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//				mUrl = null;
//			}
//			
//		}else{
//			mDomainURL = null;
//		}
		
		mContentType = Work.GET;
	}
	
	public void setAction(Action action){	
		mAction = action;
	}
	public Action getAction(){
		return mAction;
	}
	
	public void setParentWindow(String id){
		mParentWindow = id;
	}
	
	public String getParentWindow(){
		return mParentWindow;
	}
	
	public void setLoadCheckSelectors(List<String> selectors){
		mLoadCheckSelector = new ArrayList<String>();
		for(String str : selectors){
			mLoadCheckSelector.add(str);
		}
	}
	
	public List<String> getLoadCheckSelectors(){
		return mLoadCheckSelector;
	}
	
	public String getSubURL(){
		return mURL.getPath();
	}
	
	public byte getContentType(){
		return mContentType;
	}
	
	public void setContentType(byte type){
		mContentType = type;
	}
	
	public String getDomainURL(){
		return mDomainURL;
	}
	
	public String getURL(){
		return toString();
	}
	
	public int getState(){
		return mState;
	}
	
	public void updateState(int state){
		mState = state;
	}
	
	public Map<String, String> getData(){
		return mDataMap;
	}
	
	public String getData(String key){
		return mDataMap.get(key);
	}
	
	public void setHighPriority(boolean isHighPriority){
		mHighPriority = isHighPriority;
	}
	
	public boolean isHighPriority(){
		return mHighPriority;
	}
	
	public void setParseType(byte type){
		mParseType = type;
	}
	
	public byte getParseType(){
		return mParseType;
	}
	
	public void setData(String key, String value, String encodeType){
		if(mDataMap == null) mDataMap = new HashMap();
		value = value.trim();
		if(value == null || value.isEmpty()){
			if(mDataMap.get(key) == null){
				return;
			}
		}
		int len = value.length();
		String tmp;
		StringBuilder newValue = new StringBuilder();
		for(int i = 0 ; i < len ; i ++){
			if(Util.CheckHangul(value.charAt(i))){
				tmp = Util.Encoding(String.valueOf(value.charAt(i)), encodeType);
				if(tmp == null) {
					newValue = new StringBuilder(value);
					break;
				}
				newValue.append(tmp);
			}else{
				newValue.append(String.valueOf(value.charAt(i)));
			}
		}
		mDataMap.put(key, newValue.toString());
	}
	
	public Work setDepth(int depth){
		mDepth = depth;
		return this;
	}
	
	public int getDepth(){
		return mDepth;
	}
	
	/**
	 * request value 를 String 으로 변환
	 * */
	public String getDataToString(){
		String ret = null;
		if(mDataMap!= null && mDataMap.size() > 0){
			for(String key: mDataMap.keySet()){
				if(ret != null){
					ret += "&";
				}else{
					ret = "";
				}
				ret += (key + "=" + mDataMap.get(key));
			}	
		}
		return ret;
	}
	
	/**
	 * 클래스의 값을 URL GET 형식으로 반환 
	 * */
	@Override
	public String toString(){
		String ret = "";
		
		if(mDomainURL != null){
			ret = mDomainURL;
			if(mSubUrl != null){
				ret += mSubUrl;
			}
		}
//		System.out.println(mDomainURL);
//		System.out.println(mSubUrl);
//		System.out.println(ret);
		String data = this.getDataToString();
		if(data != null){ 
			ret += "?" + data;
		}
		return ret;
	}

	/*
	 * 컨텐츠의 내용을 비교하기 위해서 오버라이드 함
	 * */
	@Override
	public boolean equals(Object obj) {
		if(this.getClass() == obj.getClass()){
			Work target = (Work) obj;
			if(this.toString().contentEquals(target.toString())){
				return true;
			}
		}
		return false;
	}

	/*
	 * 컨텐츠의 내용을 비교하기 위해서 오버라이드 함
	 * */
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	public Result result(){
		if(mResult == null){
			mResult = new Result();
		}
		return mResult;
	}
	
	public class Result{
		boolean isSuccess;
		int saveCnt;

		ArrayList<Error> errorList;
		
		public Result(){
			this.saveCnt = 0;
			this.isSuccess = false;
			this.errorList = new ArrayList<Error>();
		}
		
		public void setSaveCount(int saveCnt){
			this.saveCnt = saveCnt;
		}
		
		public int getSaveCount(){
			return this.saveCnt;
		}
		
		public void success(){
			this.isSuccess = true;
		}
		
		public boolean isSuccess(){
			return this.isSuccess;
		}
		
		public void addError(byte type, String msg, Exception exception){
			Error e = new Error();
			e.type = type;
			e.msg = msg;
			e.exception = exception;
			e.timeMs = System.currentTimeMillis();
			
			this.errorList.add(e);
		}
		
		public ArrayList<Error> getErrorList(){
			return this.errorList;
		}
	}

	public static class Error{
		public static final byte ERR = 0x0;
		public static final byte ERR_URL = 0x1;
		public static final byte ERR_CONN = 0x2;
		public static final byte ERR_ACTION = 0x3;
		public static final byte ERR_CONTENTS_COL = 0x4;
		public static final byte ERR_CONTENTS_RECODE = 0x5;
		public static final byte ERR_SCEN_ELEMENT = 0x6;
		public static final byte ERR_WRITE = 0x7;
		public static final byte ERR_AJAX = 0x8;
		public final String[] TYPE_STR = {"ERR", "ERR_URL","ERR_CONN","ERR_ACTION","ERR_CONTENTS_COL","ERR_CONTENTS_RECODE", "ERR_SCEN_ELEMENT", "ERR_WRITE", "ERR_AJAX" };
		static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy-MM-dd-HH:mm:ss");
		long timeMs;
		byte type;
		String msg;
		Exception exception;
		
		public Error(){
		}
		
		public String toStringTypeAndMsg(){
			return "" + TYPE_STR[type] + " : " + this.msg;
		}
		
		public String toString(){
			String date = DATE_FORMAT.format(new Date(this.timeMs));
			return date + "" + TYPE_STR[type] + " : " + this.msg;
		}
	}

	public void setIndex(int target_depth, int i) {
		// TODO Auto-generated method stub
		
	}
}
