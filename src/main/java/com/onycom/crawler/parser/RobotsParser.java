package com.onycom.crawler.parser;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.Robots;
import com.onycom.crawler.data.Work;

/**
 * robots.txt 파일을 파싱하는 구현체
 * */
public class RobotsParser implements Parser<Work, Contents> {
	private static final String KEYWORD_ALLOW = "^(Allow:).*$";
	private static final String KEYWORD_DISALLOW = "^(Disallow:).*$";
	private static final String KEYWORD_USER_AGENT = "^(User-agent:).*$";
	private static final String KEYWORD_CRAWL_DELAY = "^(Crawl-delay:).*$";
	private Config mConfig;
//	private static final String KEYWORD_ALLOW = "Allow : ";
//	private static final String KEYWORD_ALLOW = "Allow : ";

	private String getValue(String data){
		int idx = data.indexOf(":");
		if(idx != -1){
			if(data.length() > idx+1) 
				return data.substring(idx+1);
		}
		return null;
	}

	public RobotsParser(Config config){
		mConfig = config;
	}
	
	public List<Work> parse(Work[] history, Work urlInfo, Document document) {
		List<Work> ret = new ArrayList<Work>();
		Robots robots = new Robots(urlInfo.getDomainURL());
		mConfig.getRobots().put(urlInfo.getDomainURL(), robots);
		document.outputSettings(new Document.OutputSettings().prettyPrint(false));
		try {
			//System.out.println(">>"+urlInfo.getURL());
			Elements e = document.select("body");
			String data = e.get(0).html();
			String[] lines = data.split("\n");
			String userAgent = null;
			String value;
			for (String line : lines) {
				line = line.trim().replace(" ", "");
				if(line.matches(KEYWORD_ALLOW)){
					//System.out.println(line);
					robots.add(userAgent, getValue(line), true);
				}else if(line.matches(KEYWORD_DISALLOW) ){
					//System.out.println(line);
					robots.add(userAgent, getValue(line), false);
				}else if(line.matches(KEYWORD_USER_AGENT)){
					//System.out.println(line);
					userAgent = getValue(line);
				}else if(line.matches(KEYWORD_CRAWL_DELAY)){
					//System.out.println(line);
					value = getValue(line);
					if(value != null)
						robots.add(userAgent, Integer.parseInt(value));
				}else{
					if(line.replace(" ", "").length() > 0) {
						urlInfo.result().addError(Work.Error.ERR, "robots - This line can't parse commend : " + line, null);
					}
				}
			}

			//ret.add(robots);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public void setConfig(Config config) {
		mConfig = config;
	}
}
