package com.onycom.crawler.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterModify {
	String url_regex = null;
	String replace = null;
	
	public FilterModify(String url_regex , String replace) {
		this.url_regex = url_regex;
		this.replace = replace;
	}
	
	public String regex(String url) {
		Pattern pattern;
		Matcher matcher;
		
		pattern = Pattern.compile(this.url_regex);
		matcher = pattern.matcher(url);
		String value = "";
		while (matcher.find()) {
			value += matcher.group().replaceAll(this.url_regex, this.replace);
		}
		
		if (value.isEmpty()) {
			return url;
		}else {
			return value;
		}
	}
}