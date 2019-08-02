package com.onycom.crawler.scraper;

import org.jsoup.nodes.Document;
import com.onycom.crawler.data.Work;

/**
 * 웹페이지의 데이터를 가져오는 역할을 수행 (AJAX 와 같은 동적 스크립트는 불러오지 못함 : 해당 케이스의 경우 Selenium 같은
 * 콘솔 브라우저를 이용해야함)
 */
public interface Scraper {
	boolean open();
	void close();
	//void setConfig(Config config);

	Document getDocument(Work work) throws Exception;
	//void setConfig(Config config);
	
	void clear();
}
