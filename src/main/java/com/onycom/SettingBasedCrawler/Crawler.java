package com.onycom.SettingBasedCrawler;

import java.util.Iterator;
import java.util.Map;

import com.onycom.crawler.scraper.JsoupScraper;
import com.onycom.crawler.scraper.SeleniumScraper;
import org.apache.log4j.Logger;

import com.onycom.common.CrawlerLog;
import com.onycom.common.SendMail;
import com.onycom.common.Util;
import com.onycom.crawler.core.WorkDeque;
import com.onycom.crawler.core.WorkManager;
import com.onycom.crawler.core.WorkManagerListener;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Work;
import com.onycom.crawler.parser.Parser;
import com.onycom.crawler.parser.ScenarioDynamicParser;
import com.onycom.crawler.parser.ScenarioStasticParser;
import com.onycom.crawler.parser.StaticParser;
import com.onycom.crawler.scraper.Scraper;
import com.onycom.crawler.writer.CsvWriter;
import com.onycom.crawler.writer.DBWriter;

/**
 * 크롤러의 메인 클래스.
 * */
public class Crawler {
	public static final int STATE_IDLE = 0x0; // 대기중
	public static final int STATE_RUNNING = 0x1; // 동작중
	
	public static final String FILE_NAME_ROBOTS = "/robots.txt";
	public static final String USER_AGENT_NAME = "IM_STUDENT_TEST_FOR_A_STUDY";

	private WorkManager mWorkManager;
	
	static int cnt = 0;
	Parser mParser;
	Scraper mScraper;
	
	public static DBWriter DB;
	public static com.onycom.crawler.writer.Writer Writer;

	public static Logger mLogger;
	public static Logger mProgressLogger;
	
	public Config mConfig;
	public String[] mCrawlingArgs;
	public String mConfigPath;
	
	public static String BASE_PATH;
	
	public Crawler(int size, int delay){
		mWorkManager = new WorkManager(size);
		mWorkManager.setWorkDelay(delay);
		this.setCrawlerListener(mWMListener);
		
//		String LOG_FILE = "./log/log4j.properties";
//		Properties logProp = new Properties();
//		try {
//			logProp.load(new FileInputStream(LOG_FILE));
//			PropertyConfigurator.configure(logProp);
//			System.out.println("Logging enabled");
//		} catch (IOException e) {
//			System.out.println("Logging not enabled");
//		}
//		mLogger = CrawlerLog.GetInstance(Crawler.class);
	}
	
	public Crawler(){
		this(1, 1);
	}
	
	/**
	 * c.setConfig(ArgumentData.sConfigType, 
    				ArgumentData.sStrConfigURI, 
    				ArgumentData.sDB_id, 
    				ArgumentData.sDB_pw,
    				ArgumentData.sDB_path, 
    				ArgumentData.sMapParams);
	 **/
	public boolean setConfig(int type, String uri, String db_id, String db_pw, String db_path, Map<String, String> mapParams) {
		if(mConfig == null) {
			mConfig = new Config();
		}
		if(mConfig.setConfig(type, uri, db_id, db_pw, db_path, mapParams) == false) {
			return false;
		}
		CrawlerLog.SetName(mConfig.CRAWLING_NAME, mConfig.CRAWLING_NAME_AND_TIME);
		
		if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_STATIC)){
			mScraper = new JsoupScraper();
			mParser = new ScenarioStasticParser();
		}else if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_DYNAMIC)){
			mScraper = new SeleniumScraper(mConfig);
			mParser = new ScenarioDynamicParser();
		}else if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_STATIC)){
			mScraper = new JsoupScraper();
			mParser = new StaticParser();
		}
		
		if(mParser == null || mScraper == null){
			System.err.println("[ERROR] Not found parser.");
			return false;
		}else{
			mWorkManager.setScraper(mScraper).setParser(mParser);
		}
		//mScraper.setConfig(mConfig);
		mParser.setConfig(mConfig);
		mWorkManager.setConfig(mConfig);
		if(mConfig.OUTPUT_SAVE_TYPE.contentEquals(Config.SAVE_TYPE_DB)){
			if(DB == null){
				Writer = DB = new DBWriter();
			}
		}else{
			if(DB == null) DB = new DBWriter();
			if(Writer == null) Writer = new CsvWriter();
		}
//		if(DB == null) DB = new DBWriter();
//		DB.setConfig(mConfig);
		Writer.setConfig(mConfig);
		
		return true;
	}
	
//
//	public boolean setConfig(ArgumentData args) {
//		if(mConfig == null) {
//			mConfig = new Config();
//			mConfig.setConfig(args);
//			if(!mConfig.setConfig(filePath, argsMeta, argsCrawling)){
//				System.err.println("[ERROR] Config file parsing failed.");
//				return false;
//			}
//		}else{
//			if(!mConfig.updateNext()){
//				return false;
//			}
//		}
//		
//		if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_STATIC)){
//			mScraper = new JsoupScraper();
//			mParser = new ScenarioStasticParser();
//		}else if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_DYNAMIC)){
//			mScraper = new SeleniumScraper(mConfig);
//			mParser = new ScenarioDynamicParser();
//		}else if(mConfig.CRAWLING_TYPE.contentEquals(Config.CRAWLING_TYPE_STATIC)){
//			mScraper = new JsoupScraper();
//			mParser = new StaticParser();
//		}
//    	
//		if(mParser == null || mScraper == null){
//			System.err.println("[ERROR] Not found parser.");
//			return false;
//		}else{
//			mWorkManager.setScraper(mScraper).setParser(mParser);
//		}
//		//mScraper.setConfig(mConfig);
//		mParser.setConfig(mConfig);
//		mWorkManager.setConfig(mConfig);
//		if(mConfig.OUTPUT_SAVE_TYPE.contentEquals(Config.SAVE_TYPE_DB)){
//			if(DB == null){
//				Writer = DB = new DBWriter();
//			}
//		}else{
//			if(DB == null) DB = new DBWriter();
//			if(Writer == null) Writer = new CsvWriter();
//		}
////		if(DB == null) DB = new DBWriter();
////		DB.setConfig(mConfig);
//		Writer.setConfig(mConfig);
//		Work seed = mConfig.getSeedInfo();
//		if(seed != null){
//			seedUrl(seed);
//		}
//		return true;
//	}
	
	public Crawler seedUrl(Work info) {
		if(info == null) return this;
		if(info.getDomainURL() == null) return this;
		
//		String robots_url = info.getDomainURL() + FILE_NAME_ROBOTS;
//		
//		try {
//			URLInfo robots = new URLInfo(robots_url);
//			Document doc = Scraper.GetHttp(robots);
//			new RobotsParser().parse(robots, doc);
//		} catch (KeyManagementException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
		if(mWorkManager.addWork(info)){ // 시드 URL을 주면 robots.txt 파일을 열어서 크롤링 권한을 확인
			//notifyWorker();
//			info.getDomainURL()+FILE_NAME_ROBOTS;
//			RobotsParser
		}
		return this; 
	}
	
	public Crawler seedUrl(String url) {
		if(url == null) return this;
		Work info = new Work(url, mConfig.CHARACTER_SET);
		return seedUrl(info); 
	}

	public void start() {
		if(mParser == null){
			System.err.println("[ERROR] Can't start. Config err.");
			return;
		}
		
		if(mLogger == null){
			mLogger = CrawlerLog.GetInstance(getClass());
		}
//		mProgressLogger = CrawlerLog.GetInstanceProgress();
		
		do {
			Work seed = mConfig.getSeedInfo();
			if(seed == null){
				System.err.println("[ERROR] Config file - not found seed");
				break;
			}
			seedUrl(seed);
			mWorkManager.start();
			if(Writer.getClass() == DBWriter.class){
				String[] query = mConfig.getPostProcessingQuery();
				if (query != null) {
					for(int i = 0 ; i < query.length ; i++){
						DBWriter dbw = (DBWriter) Writer;
						mLogger.info("============== Post processing =============");
						try {
							dbw.insert(query[i]);
							mLogger.info("processing query "+ i+ " :" + query[i]);
						} catch (Exception e) {
							//e.printStackTrace();
							mLogger.error("err query : " + e.getMessage());
						}
					}
				}
			}
		}while(mConfig.nextConfig());
		if(Writer != null){
			//Writer.
			try {
				Writer.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		mLogger.info("============== Finish Crawling =============");
		mLogger.info("[total time] "+ Util.GetElapedTime(mConfig.getStartTime()));
		mLogger.info("[save contents] "+ mTotalSaveCnt);
		mLogger.info("[error work] "+ mErrCnt);
		mLogger.info("[remain work] "+ mRemainWork);
		mLogger.info("[total processed work] " + mProcessedCount);
		if(mScraper != null) mScraper.close();
				
		if(mConfig.NOTIFICATION_TYPE.equalsIgnoreCase("email")) {
			SendMail mail = new SendMail(mConfig.NOTIFICATION_FROM_EMAIL,
										 mConfig.NOTIFICATION_FROM_NAME,
										 mConfig.NOTIFICATION_FROM_EMAIL_PWD);
			mail.To(mConfig.NOTIFICATION_TO_EMAIL, mConfig.NOTIFICATION_TO_NAME);
			mLogger.info("Sending... Notification email");
			String body = "crawling name : " + mConfig.CRAWLING_NAME_AND_TIME + "\n"
							+ "[total time] "+ Util.GetElapedTime(mConfig.getStartTime()) +"\n"
							+ "[save contents]"+ mTotalSaveCnt +"\n"
							+ "[error work] "+ mErrCnt +"\n"
							+ "[remain work] "+ mRemainWork +"\n"
							+ "[total processed work] " + mProcessedCount;
			mail.setMessage("[Crawling result] " + mConfig.CRAWLING_NAME_AND_TIME, body);
			if(!mail.send()) {
				mLogger.error("Failure sending notification email");
			}else {
				mLogger.info("Notification email sent successfully");
			}
			mLogger.info("============== Done =============");
		}
	} 
	
	public void setCrawlerListener(WorkManagerListener listener){
		mWorkManager.setManagerListener(listener);
	}
	
	long mTotalSaveCnt = 0;
	long mErrCnt = 0;
	long mProcessedCount = 0;
	long mRemainWork = 0;
	long mWorkSaveCnt = 0;
	long mWorkErrCnt = 0;
	
	WorkManagerListener mWMListener = new WorkManagerListener() {
		
		public boolean start() {
			CrawlerLog.OpenProgressFile(mConfig.LOG_PROGRESS_PATH);
			
			boolean ret = false;
			Map<String, String> mapArgs = mConfig.getCurArguments();
			mWorkSaveCnt = 0;
			mWorkErrCnt = 0;
			if(mProcessedCount == 0){
				mLogger.info("============== Start Crawler =============");
				mLogger.info("CONFIG URI     : " + mConfig.getConfigURI());
				if(mapArgs != null) {
					Iterator<String> keys = mapArgs.keySet().iterator();
					mLogger.info("ARG LIST COUNT : " + mapArgs.size());
					if(mapArgs != null){
						//String[] args = mConfig.GET_ARGUMENTS_LIST[mCo];
						keys.forEachRemaining(key->{
							mLogger.info(String.format("CMD PARAM %s   : %s", key, mapArgs.get(key)));
						});
					}
				}
				mLogger.info("CRAWLER NAME   : " + mConfig.CRAWLING_NAME);
				mLogger.info("CRAWLER TYPE   : " + mConfig.CRAWLING_TYPE);
				mLogger.info("CRAWLER DELAY  : " + mConfig.CRAWLING_DELAY + " sec");
				mLogger.info("IGNORE ROBOTS  : " + mConfig.IGNORE_ROBOTS);
				mLogger.info("LIMIT_COUNT    : " + mConfig.CRAWLING_MAX_COUNT);
				mLogger.info("FILTER COUNT   : " + (mConfig.getFilterAllow().size() 
									     		    + mConfig.getFilterDisallow().size() 
									                + mConfig.getFilterDuplicate().size() 
									                + mConfig.getLeafURL().size()));
				mLogger.info("SCENARIO COUNT : " + mConfig.getScenarios().size());
				mLogger.info("COLLECT COUNT  : " + mConfig.getCollects().size());
				mLogger.info("SAVE TYPE      : " + mConfig.OUTPUT_SAVE_TYPE);
				mLogger.info("SAVE HTML      : " + mConfig.SAVE_HTML);
				mLogger.info("==========================================");
			}else{
				if(mapArgs != null) {
					Iterator<String> keys = mapArgs.keySet().iterator();
					mLogger.info("============== update params =============");
					mLogger.info("ARG LIST COUNT : " + mapArgs.size());
					if(mapArgs != null){
						//String[] args = mConfig.GET_ARGUMENTS_LIST[mCo];
						keys.forEachRemaining(key->{
							mLogger.info(String.format("CMD PARAM %s   : %s", key, mapArgs.get(key)));
						});
					}
					mLogger.info("==========================================");
				}
			}
			
			CrawlerLog.WriteProgress(String.format("%s,%s,%d,%d,%d,%d",
					"start",
					Util.GetElapedTime(mConfig.getStartTime()),
					mConfig.getCurArguementIndex()+1,
					mConfig.getArgumentsCount(),
					mTotalSaveCnt,
					mErrCnt));
			
			try {
				ret = Writer.open();
				if(ret){
					ret = mScraper.open();
				}
			} catch (Exception e) {
				ret = false;
				mLogger.error(e.getMessage(), e);
			}

			if(!ret){
				mLogger.info("============== Terminate Crawler =============");
				mLogger.error("Can't start Crawler. Initialization failed.");
				
				CrawlerLog.WriteProgress(String.format("%s,%s,%d,%d,%d,%d", 
						"start_failed",
						Util.GetElapedTime(mConfig.getStartTime()), 
						mConfig.getCurArguementIndex()+1,
						mConfig.getArgumentsCount(),
						mTotalSaveCnt, 
						mErrCnt));
			}
			return ret;
		}
		
		public void progress(Work work, WorkDeque workDeque) {
			mProcessedCount++;
			mTotalSaveCnt += work.result().getSaveCount();
			mWorkSaveCnt += work.result().getSaveCount();
			
			String work_id = "0";
			
			if(work.getAction() != null) {
				work_id = work.getDepth() +"/"+ work.getAction().getNo();
			}
			
			if(work.result().getErrorList().size() > 0){
				mErrCnt += work.result().getErrorList().size();
				mWorkErrCnt += work.result().getErrorList().size();
				mLogger.error("[" + mWorkErrCnt + " ERR URL, "+ work_id + "] " + work.getURL());
				for(Work.Error err : work.result().getErrorList()){
					mLogger.error(" L " + err.toStringTypeAndMsg());
					//mLogger.error("L " + err.);
				}
			}
			
			mLogger.info(String.format("[Progress %d, %s]  elapsed : %s, curSave : %d, Totalsave : %d, curErr : %d, totalErr : %d, remain_work : %d, total : %d",
												mProcessedCount,
												work_id,
												Util.GetElapedTime(mConfig.getStartTime()),
												mWorkSaveCnt,
												mTotalSaveCnt,
												mWorkErrCnt,
												mErrCnt,
												workDeque.getSize(),
												workDeque.getHistorySize()));
			// 진행상황을 한줄로만 계속 덮어쓰기하는 파일
			CrawlerLog.WriteProgress(String.format("%s,%s,%d,%d,%d,%d", 
					"crawling",
					Util.GetElapedTime(mConfig.getStartTime()), 
					mConfig.getCurArguementIndex()+1,
					mConfig.getArgumentsCount(),
					mTotalSaveCnt, 
					mErrCnt));
//			System.out.println();
			//mLogger.info(String.format());
			/**
			 * 크롤링 횟수 제한 설정이 있다면, 작업 날리기
			 * */
			
//			if(mConfig.CRAWLING_MAX_COUNT != -1 && (mWorkSaveCnt + mWorkErrCnt) >= mConfig.CRAWLING_MAX_COUNT){ 
			if(mConfig.CRAWLING_MAX_COUNT != -1 && (mWorkSaveCnt) >= mConfig.CRAWLING_MAX_COUNT){
				workDeque.clear();
				mLogger.info("crawling_max_count : " + mConfig.CRAWLING_MAX_COUNT);
			}
		}

		public void finish(WorkDeque workDeque) {
			mRemainWork += workDeque.getSize();
			mScraper.clear();
			CrawlerLog.WriteProgress(String.format("%s,%s,%d,%d,%d,%d", 
					"finish",
					Util.GetElapedTime(mConfig.getStartTime()), 
					mConfig.getCurArguementIndex()+1,
					mConfig.getArgumentsCount(),
					mTotalSaveCnt, 
					mErrCnt));
			CrawlerLog.CloseProgressFile();
		}
		
		public void error() {
			
		}

	};
}
