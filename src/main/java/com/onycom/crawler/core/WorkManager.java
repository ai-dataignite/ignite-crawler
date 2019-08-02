package com.onycom.crawler.core;

import java.util.List;
import java.util.Map;

import com.onycom.crawler.scraper.JsoupScraper;

import com.onycom.SettingBasedCrawler.Crawler;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Robots;
import com.onycom.crawler.data.Work;
import com.onycom.crawler.data.WorkResult;
import com.onycom.crawler.parser.Parser;
import com.onycom.crawler.parser.RobotsParser;
import com.onycom.crawler.scraper.Scraper;

/**
 * 쓰레드를 관리하는 매니저. 크롤링의 핵심 로직도 담고 있는데, 이를 분리하는 작업이 필요할 것으로 보임
 * */
public class WorkManager {
	private Config mConfig;
	private Thread[] mWorkThread;
	private WorkRunnable[] mWorkRunnable;
	private Parser mParser;
	private Scraper mScraper;
	private Integer mWorkingCount = 0;
	private int mThreadPoolSize = 1;
	private float mWorkDelay = 5; // sec
	private WorkManagerListener mManagerListener;
	private WorkDeque mDeque;
	private WorkResultQueue mResultQueue;
	
//	private long mSuccessCount = 0;
//	private long mFailCount = 0;
//
//	public boolean mIsFallowRobots = false;
//
	public WorkManager(){
		this(1);
	}
	
	public WorkManager(int size){
		mThreadPoolSize = size;
		mWorkThread = new Thread[mThreadPoolSize];
		mWorkRunnable = new WorkRunnable[mThreadPoolSize];
		mDeque = new WorkDeque();
		mResultQueue = new WorkResultQueue();
	}
	
	public void setManagerListener(WorkManagerListener managerListener){
		mManagerListener = managerListener;
	}
	
	public void setWorkDelay(int delay){
		mWorkDelay = delay;
	}
	
	public WorkManager setParser(Parser parser){
		mParser = parser;
		return this;
	}

	public WorkManager setScraper(Scraper scraper) {
		mScraper = scraper;
		return this;
	}
	
	public boolean addWork(Work info){
		return mDeque.offerURL(info);
	}
	
	public void start(){
		int length = mWorkThread.length;
		WorkResult workResult;
		Map<String, Robots> mapRobots = mConfig.getRobots();
		Robots robots;
		Work work;
		List<Work> aryNewWork;
		int robotDelay = 0;
		long delay = (long)(mWorkDelay * 1000);
		//System.err.println("start notifyWorker() " + url + " / " + mThreadPoolSize);
		if(mManagerListener!= null)	{
			if(!mManagerListener.start()){
				return;
			}
		}
		while(true){
			for(int i = 0; i < length ; i ++){
				if(mDeque.getSize() > 0){
					work = mDeque.pollWork();
					if(work != null){
						// info 의 root URL 의 robot 파싱이 있는지 확인하고 없으면 파싱 시작
						if(!mConfig.IGNORE_ROBOTS){
							robots = mapRobots.get(work.getDomainURL());
							if(robots == null) {
								// 현재 작업 중인 쓰레드들은 작업하도록 나두고
								// 새로운 작업을 수행은 일시정지하기 위하여 쓰기전용 모드로 변경
								// q 를 읽었을때 null 이면 자동으로 알아서 쓰레드들이 멈출테니까.
								try {
									work.setHighPriority(true);
									mDeque.offerURL(work);

									mDeque.setAccessMode(WorkDeque.WRITE);
									Work robotsWork = new Work(work.getDomainURL() + Crawler.FILE_NAME_ROBOTS, mConfig.CHARACTER_SET);
									robotsWork.setScraper(new JsoupScraper()).setParser(new RobotsParser(mConfig));
									robotsWork.setHighPriority(true);
									//mDeque.offerURL(robotsWork);
									work = robotsWork;
								} catch (Exception e) {
									e.printStackTrace();
								} finally {
									mDeque.setAccessMode(WorkDeque.READ_AND_WRINE);
								}
							}else{
								// 로봇에 의한 차단 판단 로직 추가
								if(!robots.isAllow("*", work.getSubURL())){
									System.out.println("[deny robots] " +work.getSubURL());
									continue;
								}
								
								robotDelay = robots.getDelay("*");
								if(mWorkDelay > robotDelay){
									delay = (long)(mWorkDelay * 1000);
								}else{
									delay = robotDelay * 1000;
								}
							}
						}

						/**
						 * 쓰레드 시작
						 * */
						synchronized (mWorkingCount){
							if(delay > 0){
								try {
									Thread.sleep(delay);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							if((mThreadPoolSize - mWorkingCount) > 0){
								if(work.getScraper() == null || work.getParser() == null){
									work.setScraper(mScraper).setParser(mParser);
								}

								if(mWorkRunnable[i] != null && !mWorkRunnable[i].isRunning()){
									mWorkThread[i] = new Thread(mWorkRunnable[i].setWork(work));
								}else {
									// work 만의 파서와 스크래퍼를 사용한다면 변경 작업 수행
									mWorkRunnable[i] = new WorkRunnable(i, mDeque, work);
									mWorkRunnable[i].setWorkResultQueue(mResultQueue);
									mWorkThread[i] = new Thread(mWorkRunnable[i]);
								}
								mWorkingCount++;
								mWorkThread[i].start();
							}
						}
					}
				}
			}
			
			/**
			 * 쓰레드 result 관리
			 * 쓰레드가 종료되면 mResultQueue 에 작업한 내용과 결과를 저장함
			 * */
			if(mWorkingCount > 0){
				mResultQueue.resultWait();
				while((workResult = mResultQueue.pollResult()) != null){
					mWorkingCount --;
					work = workResult.getCurWork();
					aryNewWork = workResult.getNewWorks();
					if(aryNewWork != null){
						for(Work newWork : aryNewWork){
							if(isAllowURL(newWork)){
								mDeque.offerURL(newWork);
							}
						}
					}
					if(mManagerListener!= null)	mManagerListener.progress(work, mDeque);
				}
			}
			/**
			 * 크롤러 종료 조건
			 * 실행할 작업이 없고 + 현재 동작하고 있는 작업도 없으면 종료
			 * */
			if(mDeque.getSize() == 0 && mWorkingCount == 0){
				break;
			}
		}
		mManagerListener.finish(mDeque);
	}
	
	public boolean isAllowURL(Work info){
		Robots robots = mConfig.getRobots().get(info.getDomainURL());
		if(!mConfig.IGNORE_ROBOTS && robots!= null){
			boolean ret = robots.isAllow(Crawler.USER_AGENT_NAME, info.getSubURL());
			
			if(!ret){
				try {
					//mDB.open();
//					mDB.insertErr(info.getURL(), "deny" ).close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return ret;
//			if(!mDeque.contains(info)){
//				boolean ret = robots.isAllow(Crawler.USER_AGENT_NAME, info.getSubURL());
//				
//				if(!ret){
//					try {
//						//mDB.open();
////						mDB.insertErr(info.getURL(), "deny" ).close();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//				return ret;
//			}else{
//				try {
//					//mDB.connect().insertErr(info.getURL(), "dup" ).close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				return false;
//			}
		}else{
			return true;
		}
	}
	
//	private int getIdleWorkerCount(){ 
//		return mThreadPoolSize - mWorkingCount;
//	}

	public int upWorkingThread() {
		return ++mWorkingCount;
	}

	public int downWorkingThread() {
		return --mWorkingCount;
	}

	public void setConfig(Config config) {
		mConfig = config;
		mWorkDelay = mConfig.CRAWLING_DELAY;
	}
}
