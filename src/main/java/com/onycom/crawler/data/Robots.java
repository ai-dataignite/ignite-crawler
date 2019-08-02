package com.onycom.crawler.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Robots {
	// "User-agent: "
	// "Allow: "
	// "Disallow: "
	String RootURL;
	
	Map<String, User> mMapUsers;
	
	public Robots(String page){ 
		if(page == null) return;
		mMapUsers = new HashMap<String, User>();
	}
	
	public void add(String userAgent, String path, boolean isAllow){
		if(path == null) return;
		User user = mMapUsers.get(userAgent);
		if(user == null){
			user = new User(userAgent);
			mMapUsers.put(userAgent, user);
		}
		user.allows.put(path, isAllow);
	}
	
	public void add(String userAgent, int delay){
		if (userAgent == null) userAgent = "*"; 
		User user = mMapUsers.get(userAgent);
		if(user == null){
			user = new User(userAgent);
			mMapUsers.put(userAgent, user);
		}
		user.delay = delay;
	}
	
	public boolean isAllow(String userAgent, String path){
		if(userAgent == null) userAgent = "*";
		User user = mMapUsers.get(userAgent);
		if(user == null){
			user = mMapUsers.get("*");
		}
		if(user == null){
			return true;
		}
		
		Iterator<String> iterator = user.allows.keySet().iterator();
		String key;
		String temp;
		boolean ret = true;
		int pre_grade = 0;
		int key_grade = 0;
		while(iterator.hasNext()){
			key = iterator.next();
			
			temp = key.replace("/*", "/.*");
			if(path.matches("^(" + temp + ").*$")){
				// 문자열이 긴것을 우선순위로 하며,
				// 경로의 댑스가 깊을 것으로 추측한다.
				// 제일 정확한것은 "/" 의 개수로 체크하는게 나을듯
				key_grade = key.length();
				if(pre_grade < key_grade){
					ret = Boolean.valueOf(user.allows.get(key));
					pre_grade = key_grade;
				}
			}
			//path = String.valueOf(user.allows.get(key));
		}
		
		return ret;
	}
	
	public int getDelay(String userAgent){
		if(userAgent == null) userAgent = "*";
		User user = mMapUsers.get(userAgent);
		if(user == null){
			user = mMapUsers.get("*");
		}
		return user.delay;
	}
	
	class User{
		String userAgent;
		int delay;
		Map<String, Boolean> allows;
		
		public User(String name){
			this.userAgent = name;
			this.delay = 0;
			this.allows = new HashMap<String, Boolean>();
		}
	}
}
