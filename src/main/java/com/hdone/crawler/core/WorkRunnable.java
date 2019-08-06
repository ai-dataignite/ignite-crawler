package com.hdone.crawler.core;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.jsoup.nodes.Document;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;

import com.hdone.SettingBasedCrawler.Crawler;
import com.hdone.common.CrawlerLog;
import com.hdone.crawler.data.Work;
import com.hdone.crawler.data.WorkResult;
import com.hdone.crawler.parser.Parser;
import com.hdone.crawler.scraper.Scraper;

/**
 * 워크 쓰레드의 Runnable 구현체. 여기서 parser 를 호출하고 parser 의 결과를 모니터링하는 리스너 호출함 <p>
 * 리스너의 결과 정보 값이 부실하므로 앞으로 추가 구현 해야할 것으로 보임. 크롤링 유지보수를 위한 파싱 에러의 세분화 필요
 * */
class WorkRunnable implements Runnable{
	private Logger mLogger  = CrawlerLog.GetInstance(getClass());
	
	boolean mIsRunning = false;
	int mId;
	Crawler mCrawler;
	Scraper mScraper;
	Parser mParser;
	WorkDeque mWorkDeque;
	WorkResultQueue mResultQueue;
	Work mWork;
	
	public WorkRunnable(int id, WorkDeque deque, Work work){
		mId = id;
		//mCrawler = crawler;
		mWork = work;

		mWorkDeque = deque;
		mScraper = work.getScraper();
		mParser = work.getParser();
	}
	
	public void setWorkResultQueue(WorkResultQueue queue){
		mResultQueue = queue;
	}
	
	public WorkRunnable setWork(Work work){
		mScraper = work.getScraper();
		mParser = work.getParser();
		mWork = work;
		return this;
	}
	
	public void run() {
		mIsRunning = true;
		boolean isSuccess = false;
		Work info = mWork;
		Document doc = null;
		List<Work> results = null;
		//mIsRunning = true;
		if(info != null){
			try {
				doc = mScraper.getDocument(info);
				if(mParser != null && doc != null){
					results = mParser.parse(mWorkDeque.getHistory(), info, doc);
				}
				isSuccess = true;
			} catch (JSONException e) { // javascript 반환 파싱 오류
				mLogger.error(e.getMessage(), e.fillInStackTrace());
			} catch (WebDriverException e) { // javascript + element 못찾을때 오류
				if(e.toString().indexOf("Runtime.evaluate") != -1){
					info.result().addError(Work.Error.ERR_ACTION, "javascript syntax err - " + e.getMessage(), e);
					//mLogger.error("javascript syntax err " + e.getMessage(), e.fillInStackTrace());
				}else{
					//info.result().addError(Work.Error.ERR_SCEN_ELEMENT, "");
					mLogger.error("not found element " + e.getMessage(), e.fillInStackTrace());
					if(info.getAction() != null)
						info.result().addError(Work.Error.ERR, "not found element [" + info.getAction().getSelector() + " ] " + e.getMessage(), e);
				}
			} catch (Exception e) {
				e.printStackTrace();
				info.result().addError(Work.Error.ERR, e.getMessage() +"\n" + e.fillInStackTrace(), e);
				//mLogger.error(e.getMessage(), e.fillInStackTrace());
			}
		}

		// history 로 저장하기 때문에 필요없는 객체는 메모리에서 삭제
		info.setParser(null).setScraper(null);

		mResultQueue.offerResult(new WorkResult(info, results));
		mIsRunning = false;
		synchronized (mResultQueue) {
			mResultQueue.resultNotifyAll();
		}
	}
	
	public void stop(){
		//mIsRunning = false;
	}
	
	public boolean isRunning(){
		return mIsRunning;
	}
}
