package com.onycom.crawler.data;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;

import com.onycom.common.Util;

/**
 * 설정파일에서 파싱된 중복 필터링 정보를 담는 객체
 * */
public class Duplicate {
	String mRegex;
	String[] mKeys;
	
	public Duplicate(JSONArray ja){
		int len = ja.length();
		if(len - 1 > 0){
			mKeys = new String[len-1];
		}
		String value;
		for(int i = 0 ; i < len ; i ++){
			value = ja.getString(i);
			if(i > 0){
				mKeys[i-1] = value;
			}else{ // i == 0
				mRegex = value;
			}
		}
	}
	
	public String regex(Work info){
		Map<String,String> data = info.getData();
		if(data == null) return null;
		String ret = null;
		String value;
		ret = mRegex.replace("<%=URL%>", Util.ConvertForRegex(info.getURL()));
		
		int sIdx,eIdx = -1;
		String key;
		while((sIdx = ret.indexOf("<%=")) != -1){
			eIdx = ret.indexOf("%>", sIdx+2);
			if(eIdx != -1){
				key = ret.substring(sIdx+2, eIdx);
				value = data.get(key);
				ret = ret.substring(0,sIdx) + value + ret.substring(eIdx+1);
			}else{
				break;
			}
		}
		
		return ret;
	}
}
