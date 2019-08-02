package com.onycom.crawler.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 쿠기 정보를 담는 개체. 현재는 미사용
 * */
public class Cookie {
	Map<String, CookieData> mMap;
	
	public Cookie(){
		mMap = new HashMap<String, CookieData>();
	}
	
	public void put(String url, Map<String,String> data){
		if(data == null) return;
		CookieData cookieData = mMap.get(url);
		if(cookieData == null){
			cookieData = new CookieData();
			mMap.put(url, cookieData);
		}
		cookieData.setAllData(data);
	}
	
	public Map<String, String> get(String url){
		CookieData cookieData = mMap.get(url);
		if(cookieData != null)
			return mMap.get(url).getData();
		else
			return null;
	}
	
	class CookieData {
		Map<String, String> data;
		
		CookieData(){
			this.data = new HashMap<String,String>(); 
		}
		
		void setAllData(Map<String, String> cookies){
			this.data.putAll(cookies);
		}
		
		Map<String,String> getData(){
			return this.data;
		}
	}
}
