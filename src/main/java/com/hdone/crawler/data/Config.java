package com.hdone.crawler.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import com.hdone.SettingBasedCrawler.App;
import com.hdone.SettingBasedCrawler.ArgumentData;
import com.hdone.common.CrawlerLog;
import com.hdone.common.DBManager;
import com.hdone.common.Util;
import com.hdone.crawler.scraper.JsoupScraper;

import au.com.bytecode.opencsv.CSVReader;

/**
 * 설정 정보를 담는 객체. json 형태의 설정파일을 파싱하여 객체화함
 */
public class Config {
	static Logger mLogger = CrawlerLog.GetInstanceSysout(Config.class);
	
	public static final String DATETIME_FORMAT = "yyMMdd_HHmmssSSS";

	public static final String SAVE_TYPE_DB = "DB";
	public static final String SAVE_TYPE_CSV = "CSV";
	public static final String SAVE_TYPE_JSON = "JSON";
	public static final String SAVE_TYPE_XML = "XML";

	public static final String CRAWLING_TYPE_STATIC = "STATIC";
	public static final String CRAWLING_TYPE_SCENARIO_STATIC = "SCENARIO_STATIC";
	public static final String CRAWLING_TYPE_SCENARIO_DYNAMIC = "SCENARIO_DYNAMIC";

	public static final String DEAULT_OUTPUT_FILE_PATH = "./output";
	public static final String DEAULT_HTML_FILE_PATH = "./html";

	public static final String COLLECT_COLUMN_TYPE_URL = "URL";
	public static final String COLLECT_COLUMN_TYPE_KEYWORD = "KEYWORD";
	public static final String COLLECT_COLUMN_TYPE_DATETIME = "DATETIME";
	public static final String COLLECT_COLUMN_TYPE_TEXT = "TEXT";
	public static final String COLLECT_COLUMN_TYPE_ELEMENT = "ELEMENT";
	
	public static final String CONTENTS_TYPE_STRING = "string";
	public static final String CONTENTS_TYPE_INT = "int";
	public static final String CONTENTS_TYPE_FLOAT = "float";
	public static final String CONTENTS_TYPE_FILE = "file";
	
	public Work mSeedInfo;
	
	public String GET_ARGUMENTS_TYPE = "conf"; // db, csv, cmd, conf 
	public String GET_ARGUMENTS_QUERY = "";
//	public String GET_ARGUMENTS_ = "";
//	public String[][] GET_ARGUMENTS_LIST;
	public Map<String, String> mCurMap;
	public List<Map<String, String>> MAP_VARI_ARG_LIST;
	public Map<String, String> MAP_FIXED_ARG;
	
	public boolean IGNORE_ROBOTS = false;

	public String CRAWLING_TYPE = CRAWLING_TYPE_STATIC;
	public float CRAWLING_DELAY = -1; // ms
	public int CRAWLING_MAX_COUNT = -1;
	public int CRAWLING_MAX_DEPTH = -1;
	public boolean CRAWLING_UPPER_SEARCH = false;

	public String OUTPUT_SAVE_TYPE = SAVE_TYPE_CSV; // json, xml, db;
	public String OUTPUT_FILE_PATH = DEAULT_OUTPUT_FILE_PATH;
	public String DB_PATH = "";
	public String DB_ID = "";
	public String DB_PW = "";
	public String CRAWLING_NAME = "";
	public String CRAWLING_FILE = "";
	public Long CRAWLING_START_TIME;
	public String CRAWLING_NAME_AND_TIME = "";
	
	public String CHARACTER_SET = "UTF-8";

	public String SELENIUM_DRIVER_NAME = "phantomjs";
	public String SELENIUM_DRIVER_PATH = "";
	public boolean SELENIUM_HEADLESS = true;
	
	public boolean SAVE_HTML = false;
	
	public String NOTIFICATION_TYPE = "";
	public String NOTIFICATION_FROM_EMAIL = "";
	public String NOTIFICATION_FROM_EMAIL_PWD = "";
	public String NOTIFICATION_FROM_NAME = "";
	public String NOTIFICATION_TO_EMAIL = "";
	public String NOTIFICATION_TO_NAME = "";
	
	public String LOG_PROGRESS_PATH = "";

	List<Duplicate> mFilterDuplicate; // URL중복 체크 조건
	List<String> mFilterAllow; // 허용된 URL(또는 패턴)
	List<String> mFilterDisallow; // 접속하지 않는 URL(또는 패턴)
	List<String> mLeafURL; // 크롤링 마지막 URL(또는 패턴) : 해당 페이지의 링크는 수집하지 않는다
	List<FilterModify> mFilterModify;
	
	Dictionary mDictionary;
	
	List<CollectRecode> mCollects; // 수집할 콘텐츠
	
	
	Map<Integer, Scenario> mScenarios;

	Map<String, Robots> mRobots;
	
	String[] mPostProcessingQuery;
	
	public static final String KEY_CRAWLING_NAME = "name";
	public static final String KEY_IGNORE_ROBOTS = "ignore_robots";
	public static final String KEY_CRAWLING_MAX_DEPTH = "crawling_max_depth";
	public static final String KEY_CRAWLING_DELAY = "crawling_delay";
	public static final String KEY_CRAWLING_MAX_COUNT = "crawling_max_count";
	public static final String KEY_CRAWLING_TYPE = "crawling_type";
	public static final String KEY_CONTENTS_SAVE_TYPE = "contents_save_type";
	public static final String KEY_OUTPUT_FILE_PATH = "output_file_path";
	public static final String KEY_DB_ID = "db_id";
	public static final String KEY_DB_PW = "db_pw";
	public static final String KEY_DB_PATH = "db_path";
	public static final String KEY_SAVE_HTML = "save_html";
	public static final String KEY_SELENIUM_DRIVER_NAME = "selenium_driver_name";
	public static final String KEY_SELENIUM_DRIVER_PATH = "selenium_driver_path";
	public static final String KEY_SELENIUM_HEADLESS = "selenium_headless";
	public static final String KEY_CHARACTER_SET = "charset";
	
	public static final String KEY_NOTIFICATION = "notification";
	public static final String KEY_NOTIFICATION_TYPE = "type";
	public static final String KEY_NOTIFICATION_FROM = "from";
	public static final String KEY_NOTIFICATION_TO = "to";
	
	private static final String KET_ARGUMENTS = "arguments";
	private static final String KEY_GET_ARGUMENTS_TYPE = "get_type";
	private static final String KEY_GET_ARGUMENTS_QUERY = "query";
	private static final String KEY_GET_ARGUMENTS_LIST = "list";
	
	private static final String KEY_GET_ARGUMENTS_CSV_FILE_PATH = "file_path";
	
	private static final String KEY_POST_PROCESSING = "post_processing";
	private static final String KEY_POST_PROCESSING_QUERY = "query";
	
	private static final String KEY_LOG = "log";
	private static final String KEY_LOG_PROGRESS_PATH = "progress_path";
	
	String mConfigStr;
	
	private void setConfigString(String config) {
		mConfigStr = config;
	}
	
	private String getConfigString() {
		return mConfigStr;
	}
	
	String mConfigURI = "";
	
	int mConfigParamCursor = -1;
	public Map<String,String> getCurArguments(){
		if(MAP_VARI_ARG_LIST == null || mConfigParamCursor == -1) return null;
		return MAP_VARI_ARG_LIST.get(mConfigParamCursor);
	}
	
	public int getCurArguementIndex() {
		return mConfigParamCursor;
	}
	
	public int getArgumentsCount() {
		if(MAP_VARI_ARG_LIST == null) return 0;
		return MAP_VARI_ARG_LIST.size();
	}
	
	public Map<String,String> getNextArguments(){
		if(MAP_VARI_ARG_LIST == null || mConfigParamCursor == MAP_VARI_ARG_LIST.size()-1) return null;
		mConfigParamCursor++;
		return MAP_VARI_ARG_LIST.get(mConfigParamCursor);
	}
	
	public String getConfigURI(){
		return mConfigURI;
	}

	public boolean setConfig(int type, String uri, String db_id, String db_pw, String db_path, Map<String, String> mapArgs) {
		String configStr = null;
		
		if(!db_id.isEmpty() && !db_pw.isEmpty() && !db_path.isEmpty()){
			DB_ID = db_id;
			DB_PW = db_pw;
			DB_PATH = db_path;
		}
		
		if (type == ArgumentData.TYPE_FILE) {
			File configFile = new File(uri);
	    	if(!configFile.exists()){
	    		System.err.println("[ERROR] Config file is not exists");
	    		return false;
	    	}
	    	configStr = (String) Util.GetFileToString(uri);
		}else if (type == ArgumentData.TYPE_URL) {
			JsoupScraper jsoupScraper = new JsoupScraper();
	    	try {
				Document doc = jsoupScraper.getDocument(new Work(uri));
				if(doc == null) {
					System.err.println("[ERROR] Can't connect to config url");
					return false;
				}
				configStr = doc.text();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("[ERROR] Can't connect to config url");
				return false;
			}
		}else if (type == ArgumentData.TYPE_DB) {
			DBManager dbm = new DBManager();
			if(dbm.open(DB_PATH, DB_ID, DB_PW)){
				List<DataMap> datas = dbm.select(uri);
				if(datas != null){
					if (datas.size() > 0) {
						Set<String> set = datas.get(0).keySet();
						Iterator<String> iter = set.iterator();
						if (iter.hasNext() ) {
							configStr = datas.get(0).getString(iter.next());
						}else {
							System.err.println("[ERROR] Not found config data from db");
							return false;
						}
					}
				}
				dbm.close();
			}else {
				System.err.println("[ERROR] Can't connect to db");
				return false;
			}
		}
		mConfigURI = uri;
		setConfigString(configStr);
		setMetaConfig(configStr);
//		mConfigStr = configStr;
		
		// 파라미터
		MAP_FIXED_ARG = mapArgs;
		MAP_VARI_ARG_LIST = getConfigVariableArgs(configStr);
		if(MAP_VARI_ARG_LIST == null) {
			MAP_VARI_ARG_LIST = new ArrayList<Map<String,String>>();
			MAP_VARI_ARG_LIST.add(new HashMap<String,String>());
		}
		
		if(MAP_FIXED_ARG != null) { 
			configStr = configParseParams(configStr, MAP_FIXED_ARG);
		}
		
		if(nextConfig()) {
			return true;
		}else {
			return false;
		}
	}

//	public boolean setConfig(String uri, 
//							 String configStr, 
//							 String[] metaArgs, 
//							 String[] crawlingArgs){		
//		mConfigURI = uri;
////		String configStr = (String) Util.GetFileToString(filePath);
//		mConfigStr = configStr;
//		setMetaConfig(configStr, metaArgs, crawlingArgs);
//		return updateNext();
//	}
//	
	/*
	 * 파라미터 값 설정 string 에 적용
	 * */
	public String configParseParams(String configStr, Map<String, String> mapParams){
//		String regexParams = "<%[0-9a-zA-Z_-]+%>";
//        Pattern pattern = Pattern.compile(regexParams);
//        Matcher matcher = pattern.matcher(config);
		
        Iterator<String> keys = mapParams.keySet().iterator();
        String key, value;
        while(keys.hasNext()){
        	key = keys.next();
        	value = mapParams.get(key);
        	configStr = configStr.replace("<%"+ (key) +"%>", value);
        }
		return configStr;
	}
	
	private boolean vaildateConfigFromParams(String configStr) {
		String regexParams = "<%[0-9a-zA-Z_-]+%>";
		Pattern pattern = Pattern.compile(regexParams);
      	Matcher matcher = pattern.matcher(configStr);
      	
      	boolean ret = true;
      	while(matcher.find()) {
      		ret = false;
      		System.err.println("unmatch parameter: " + matcher.group().substring(2, matcher.group().length()-2));
      	}
      	
      	return ret;
	}
	
//	public String configParseArgs(String configStr, String[] args){
//		String config = configStr;
//		
//		String regexParams = "<%[0-9]+%>";
//        Pattern pattern = Pattern.compile(regexParams);
//        Matcher matcher = pattern.matcher(config);
//
//        int totalParams = 0;
//        String value = "";
//        Integer cnt = 0;
//        HashMap<String, Integer> paramMap = new HashMap<String, Integer>();
//        while (matcher.find()){
//        	value = matcher.group();
//        	cnt = paramMap.get(value);
//        	if(cnt != null){
//        		cnt ++;
//        	}else{
//        		totalParams++;
//        		cnt = 1;
//        	}
//        	paramMap.put(matcher.group(), cnt);
//        }
//        
//        int len = 0;
//        if(args != null){
//        	len = args.length;
//        }
//        if(totalParams != (len)){
//        	System.err.println("[ERROR] Mismatching config parameters.");
//        	return null;
//        }
//		if(len > 0){
//			for(int i =  0 ; i < len ; i++){
//				config = config.replace("<%"+ (i) +"%>", args[i]);
//			}
//		}
//		return config;
//	}
	
	
	private List<Map<String,String>> getConfigVariableArgs(String strConfig) {
		List<Map<String, String>> ret = null;
		Map<String,String> newMapArg;
		Iterator<String> keys;
		String key,value;
		
		JSONObject root, jObj;
		JSONArray jAry;
		try{
			root = new JSONObject(strConfig);
			if(!root.isNull(KET_ARGUMENTS)){
				jObj = root.getJSONObject(KET_ARGUMENTS);
				if(!jObj.isNull(KEY_GET_ARGUMENTS_TYPE)){
					GET_ARGUMENTS_TYPE = jObj.getString(KEY_GET_ARGUMENTS_TYPE);
				}
				
				/* 값이 config 파일 안에 설정되어 있는 경우 */
				if(GET_ARGUMENTS_TYPE.equalsIgnoreCase("list")){
					if(!jObj.isNull(KEY_GET_ARGUMENTS_LIST)){
						jAry = jObj.getJSONArray(KEY_GET_ARGUMENTS_LIST);
						int len_i = jAry.length();
						if (len_i == 0) return ret; 
						ret = new ArrayList<Map<String,String>>();
						JSONObject objectRow;
						
						
						for(int i = 0 ; i < len_i ; i++) {
							objectRow = jAry.getJSONObject(i);
							keys = objectRow.keys();
							
							newMapArg = new HashMap<String,String>();
							while(keys.hasNext()) {
								key = keys.next();
								value = objectRow.getString(key);
								newMapArg.put(key, value);
							}
							if(newMapArg.size() > 0) {
								ret.add(newMapArg);
							}
						}
					}
				}else if(GET_ARGUMENTS_TYPE.equalsIgnoreCase("db")){
					if(!jObj.isNull(KEY_GET_ARGUMENTS_QUERY)){
						GET_ARGUMENTS_QUERY = jObj.getString(KEY_GET_ARGUMENTS_QUERY);
						DBManager dbm = new DBManager();
						if(dbm.open(DB_PATH, DB_ID, DB_PW)){
							List<DataMap> datas = dbm.select(GET_ARGUMENTS_QUERY);
							if(datas != null){
								int len_i = datas.size();
								if(len_i == 0) return ret;
								DataMap row;
								ret = new ArrayList<Map<String,String>>();
								for(int i = 0 ; i < len_i ; i++){
//									GET_ARGUMENTS_LIST[i] = new String[datas.get(i).size()];
									row = datas.get(i);
									Set<String> set = row.keySet();
									Iterator<String> iter = row.keySet().iterator();
									newMapArg = new HashMap<String,String>();
									while ( iter.hasNext() ) {
										key = iter.next(); 
										value = String.valueOf(row.get(key));
										newMapArg.put(key, value);
									}
									if(newMapArg.size() > 0) {
										ret.add(newMapArg);
									}
								}
							}
							dbm.close();
						}
					}
				}else if(GET_ARGUMENTS_TYPE.equalsIgnoreCase("csv")){
					if(!jObj.isNull(KEY_GET_ARGUMENTS_QUERY)){
						String query = jObj.getString(KEY_GET_ARGUMENTS_QUERY);
						if(!jObj.isNull(KEY_GET_ARGUMENTS_CSV_FILE_PATH)){
							String filePath = jObj.getString(KEY_GET_ARGUMENTS_CSV_FILE_PATH);
							File file = new File(filePath);
							if(!file.exists()) {
								System.err.println("Config ERR : argument csv file is not exists.");
								return ret;
							}
							try {
								String encode = "UTF-8";
								if(!jObj.isNull("file_encode")){
									encode = jObj.getString("file_encode");
								}
								CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), encode));
								List<String[]> datas = reader.readAll();
								if(datas != null && datas.size() > 1){
									String[] row;
									String[] header = datas.get(0); // header 파싱 
//									GET_ARGUMENTS_LIST = new String[datas.size()-1][];
									ret = new ArrayList<Map<String,String>>();
									newMapArg = new HashMap<String,String>();
									// header 첫행 제외 로직 포함
									for(int i = 1 ; i < datas.size() ; i ++){
										row = datas.get(i);
										for(int j = 0 ; j < row.length ; j++) {
											newMapArg.put(header[j], row[j]);
										}
										if(newMapArg.size() > 0) {
											ret.add(newMapArg);
										}
									}
								}
								reader.close();
							} catch (IOException e1){
								e1.printStackTrace();
							}
						}
					}
				}
			}
		}catch(JSONException e){
			System.err.println("Config ERR : wrong config file");
			return null;
		}
		
		return ret;
	}
	
	
	/**
	 * 크롤러 처음 실행후 변하지 않는 중요 정보 
	 */
	public boolean setMetaConfig(String config){
		JSONObject root, jObj;
		JSONArray jAry;
		try{
			root = new JSONObject(config);

			if(!root.isNull(KEY_CRAWLING_NAME)){
				CRAWLING_NAME = root.getString(KEY_CRAWLING_NAME);
				CRAWLING_NAME = CRAWLING_NAME.trim().replace(" ", "_");
			}else{
				System.out.println("Config ERR : Require crawling name");
				return false;
			}
			
			if(DB_ID.isEmpty()) {
				if(!root.isNull(KEY_DB_ID)){
					DB_ID = root.getString(KEY_DB_ID);
				}
			}
			
			if(DB_PW.isEmpty()) {
				if(!root.isNull(KEY_DB_PW)){
					DB_PW = root.getString(KEY_DB_PW);
				}
			}
			
			if(DB_PATH.isEmpty()) {
				if(!root.isNull(KEY_DB_PATH)){
					DB_PATH = root.getString(KEY_DB_PATH);
				}
			}
			
			// 커맨드 우선
//			if(crawlingArgs != null && crawlingArgs.length > 0){
//				GET_ARGUMENTS_LIST = new String[1][crawlingArgs.length];
//				GET_ARGUMENTS_LIST[0] = crawlingArgs;
//			}else{
//				//---
//			}
			
			if(!root.isNull(KEY_POST_PROCESSING)){
				jObj = root.getJSONObject(KEY_POST_PROCESSING);
				if(!jObj.isNull(KEY_POST_PROCESSING_QUERY)){
					jAry = jObj.getJSONArray(KEY_POST_PROCESSING_QUERY);
					if(jAry.length() > 0){
						mPostProcessingQuery = new String[jAry.length()];
						for(int i = 0 ; i < mPostProcessingQuery.length ; i++){
							mPostProcessingQuery[i] = jAry.getString(i);
						}
					}
				}
			}
			
//			"notification": {
//				"type" : "email",
//				"from" : {"email": "gmanhdh@gmail.com", "name": "HandhCrawler", "password": "<%email_pwd%>"},
//				"to" : {"email": "", "name": "done"}
//			}, 
			
			if(!root.isNull(KEY_NOTIFICATION)){
				jObj = root.getJSONObject(KEY_NOTIFICATION);
				if(!jObj.isNull(KEY_NOTIFICATION_TYPE)){
					NOTIFICATION_TYPE = jObj.getString(KEY_NOTIFICATION_TYPE);
					if(NOTIFICATION_TYPE.equalsIgnoreCase("email")) {
						JSONObject subObj = jObj.getJSONObject(KEY_NOTIFICATION_FROM);
						NOTIFICATION_FROM_EMAIL = subObj.getString("email");
						NOTIFICATION_FROM_EMAIL_PWD = subObj.getString("password");
						NOTIFICATION_FROM_NAME = subObj.getString("name");
						
						subObj = jObj.getJSONObject(KEY_NOTIFICATION_TO);
						NOTIFICATION_TO_EMAIL = subObj.getString("email");
						NOTIFICATION_TO_NAME = subObj.getString("name");
					}
				}
			}
			
			if(!root.isNull(KEY_LOG)){
				jObj = root.getJSONObject(KEY_LOG);
				if(!jObj.isNull(KEY_LOG_PROGRESS_PATH)){
					LOG_PROGRESS_PATH = jObj.getString(KEY_LOG_PROGRESS_PATH);
				}
			}
			
		}catch(JSONException e){
			System.err.println("Config ERR : wrong config file" + e.getMessage());
			return false;
		}

		CRAWLING_START_TIME = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT); 
		String strTime = sdf.format(new Date(CRAWLING_START_TIME));
		CRAWLING_NAME_AND_TIME = CRAWLING_NAME + "_" + strTime;
			
		return true;
	}

	public int configCount(){
		if(MAP_VARI_ARG_LIST != null) return MAP_VARI_ARG_LIST.size();
		else return 1;
	}
	
	public boolean nextConfig(){
		// 고정 파라미터 업데이트
		String configString = getConfigString();
		if(MAP_FIXED_ARG != null) { 
			configString = configParseParams(configString, MAP_FIXED_ARG);
		}
		Map<String,String> variArgs = getNextArguments();
		if (variArgs != null) {
			configString = configParseParams(configString, variArgs);
			if (vaildateConfigFromParams(configString) 
					&& setMetaConfig(configString) 
					&& setCrawlingConfig(configString)) {
				return true;
			}else {
				System.err.println("Config ERR : wrong config file. [code:0]");
				return false;
			}
		}else {
			return false;
		}
	}
	
	/**
	 * 크롤러 실행 마다 변경되는 파라미터 내용 업데이트
	 * 예) 조회 키워드, 조회 날짜 등.. 
	 * */
	public boolean setCrawlingConfig(String config){
//		config = configParseArgs(config, args);
		if(config == null) return false;
		JSONObject root;
		try{
			root = new JSONObject(config);
		}catch(JSONException e){
			System.out.println("Config ERR : wrong config file");
			return false;
		}
		
		if(!root.isNull(KEY_IGNORE_ROBOTS)){
			IGNORE_ROBOTS = root.getBoolean(KEY_IGNORE_ROBOTS);
		}
		
		if(!root.isNull(KEY_CRAWLING_MAX_DEPTH)){
			CRAWLING_MAX_DEPTH = root.getInt(KEY_CRAWLING_MAX_DEPTH);
		}
		
		if(!root.isNull(KEY_CRAWLING_DELAY)){
			CRAWLING_DELAY = root.getFloat(KEY_CRAWLING_DELAY);
		}
		
		if(!root.isNull(KEY_CRAWLING_MAX_COUNT)){
			CRAWLING_MAX_COUNT = root.getInt(KEY_CRAWLING_MAX_COUNT);
		}
		
		if(!root.isNull(KEY_CRAWLING_TYPE)){
			CRAWLING_TYPE = root.getString(KEY_CRAWLING_TYPE);
		}
		
		if(!root.isNull(KEY_CONTENTS_SAVE_TYPE)){
			OUTPUT_SAVE_TYPE = root.getString(KEY_CONTENTS_SAVE_TYPE);
		}
		
		if(!root.isNull(KEY_OUTPUT_FILE_PATH)){
			OUTPUT_FILE_PATH = root.getString(KEY_OUTPUT_FILE_PATH);
		}
		
		if(!root.isNull(KEY_SAVE_HTML)){
			SAVE_HTML = root.getBoolean(KEY_SAVE_HTML);
		}

		if(!root.isNull(KEY_SELENIUM_DRIVER_NAME)){
			SELENIUM_DRIVER_NAME = root.getString(KEY_SELENIUM_DRIVER_NAME);
		}

		if(!root.isNull(KEY_SELENIUM_DRIVER_PATH)){
			SELENIUM_DRIVER_PATH = root.getString(KEY_SELENIUM_DRIVER_PATH).trim();
		}

		if(!root.isNull(KEY_SELENIUM_HEADLESS)){
			SELENIUM_HEADLESS = root.getBoolean(KEY_SELENIUM_HEADLESS);
		}
		
		if(!root.isNull(KEY_CHARACTER_SET)){
			CHARACTER_SET = root.getString(KEY_CHARACTER_SET);
		}
//		
//		if(!root.isNull(KEY_)){
//			IGNORE_ROBOTS = root.getBoolean(KEY_IGNORE_ROBOTS);
//			mLogger.info("Set Config ignore_robots : " + IGNORE_ROBOTS);
//		}
		
		
		//CRAWLING_UPPER_SEARCH = root.getBoolean("crawling_upper_search");
		
		mFilterDuplicate = new ArrayList<Duplicate>();
		mFilterAllow = new ArrayList<String>();
		mFilterDisallow = new ArrayList<String>();
		
		mLeafURL = new ArrayList<String>();
		mCollects = new ArrayList<CollectRecode>();
		mScenarios = new HashMap<Integer, Scenario>();
		mFilterModify = new ArrayList<FilterModify>();
		
		mRobots = new HashMap<String, Robots>();
		
		JSONObject object;
		JSONArray array;
		
		/**
		 * SEED 파싱
		 * */
		try{
			object = root.getJSONObject("seed");
			mSeedInfo = new Work(object.getString("url"), CHARACTER_SET);
			mSeedInfo.setDepth(0); // 루트로 인식
			if(object.isNull("type")) {
				mSeedInfo.setContentType(Work.GET);
			}else{
				String type = object.getString("type");
				if(type.equalsIgnoreCase("POST")){
					mSeedInfo.setContentType(Work.POST);
				}else{
					mSeedInfo.setContentType(Work.GET);
				}
			}
			if(!object.isNull("data")){
				array = object.getJSONArray("data");
				String key, value;
				for(int i = 0 ; i < array.length() ; i++){
					object = array.getJSONObject(i);
					key = object.getString("key");
					value = object.getString("value");
					mSeedInfo.setData(key, value, CHARACTER_SET);
				}
			}
		}catch(JSONException e){ // seed 파싱을 못할 경우 크롤링을 아예 진행하지 않도록
			System.err.println("Config ERR : seed");
			return false;
		}
		
		/**
		 * filter 파싱
		 * */
		Duplicate duplicate;
		
		try{
			object = root.getJSONObject("filter");
			try {
				array = object.getJSONArray("duplicate");
				for (Object url : array) {
					if (url != null) {
						duplicate = new Duplicate((JSONArray) url);
						mFilterDuplicate.add(duplicate);
					}
				}
				System.out.println("CONFIG duplicate : " + array.length());
			} catch (JSONException e) { mFilterDuplicate.clear(); }
			
			try {
				array = object.getJSONArray("allow");
				for (Object url : array) {
					if (url != null) {
						mFilterAllow.add((String) url);
					}
				}
				System.out.println("CONFIG allow : " + array.length());
			} catch (JSONException e) {  mFilterAllow.clear(); }

			try {
				array = object.getJSONArray("disallow");
				for (Object url : array) {
					if (url != null) {
						mFilterDisallow.add((String) url);
					}
				}
				System.out.println("CONFIG disallow : " + array.length());
			} catch (JSONException e) { mFilterDisallow.clear();}

			try {
				array = object.getJSONArray("leaf_url");
				for (Object url : array) {
					if (url != null) {
						mLeafURL.add((String) url);
					}
				}
				System.out.println("CONFIG leaf_url : " + array.length());
			} catch (JSONException e) { mLeafURL.clear(); }
			
			try {
				array = object.getJSONArray("modify");
				
				for (int i = 0 ; i < array.length() ; i++) {
					JSONObject o = array.getJSONObject(i);
					if (!o.isNull("url") && !o.isNull("replace")) {
						String url = o.getString("url");
						String replace = o.getString("replace");
						mFilterModify.add(new FilterModify(url, replace));
					}
				}
			} catch (JSONException e) { mFilterModify.clear(); }
			
		} catch (JSONException e) { }

		JSONArray aryCols,aryJson;
		JSONObject col, jsonElement;
		CollectRecode recode;
		int len_i, len_j, len_k;
		String[] regexs = null;
		String regexs_replace= null;
		String recode_selector;
		CollectRecode.Column.Element[] elements = null;
		CollectRecode.Column.Element el;
		if(root.isNull("collect_recode")){
			System.err.println("Config err : collect_recode");
			return false;
		}else{
			array = root.getJSONArray("collect_recode");
			len_i = array.length();
			for(int i = 0 ; i < len_i ; i ++){
				try{
					object = array.getJSONObject(i);
					recode = new CollectRecode(object.isNull("regex_url")? "": object.getString("regex_url"), 
											   object.getString("name"));
					if(!object.isNull("depth")) recode.setDepth(object.getInt("depth"));
					if(object.isNull("recode_selector")){
						recode_selector = "html";
					}else{
						recode_selector = object.getString("recode_selector");
						if(recode_selector.trim().isEmpty()){
							recode_selector = "html";
						}
					}
					recode.setRecodeSelector(recode_selector);
					
					if(object.isNull("insert_or_update")) {
						recode.setInsertORUpdate(false);
					}else {
						recode.setInsertORUpdate(object.getBoolean("insert_or_update"));
					}
					
					aryCols = object.getJSONArray("column");
					len_j = aryCols.length();
					for(int j = 0 ; j < len_j ; j++ ){
						col = aryCols.getJSONObject(j);
						regexs = null;
						if(!col.isNull("regex_filter")){
							aryJson = col.getJSONArray("regex_filter");
							regexs = new String[aryJson.length()];
							aryJson.toList().toArray(regexs);
						}
						
						if(!col.isNull("regex_replace")){
							regexs_replace = col.getString("regex_replace");
						}
						
						
						if(!col.isNull("element")){
							aryJson = col.getJSONArray("element");
							elements = new CollectRecode.Column.Element[aryJson.length()];
							len_k = aryJson.length();
							for(int k = 0 ; k < len_k ; k++){
								jsonElement = aryJson.getJSONObject(k);
								elements[k] = new CollectRecode.Column.Element(
												jsonElement.isNull("selector")? "": jsonElement.getString("selector"), 
												jsonElement.isNull("type")? "text": jsonElement.getString("type"), // element의 값 타입 attr or text
												jsonElement.isNull("from_root")? false: jsonElement.getBoolean("from_root"));
							}
						}
						
						recode.add(col.isNull("key")? false : col.getBoolean("key"),
								   col.isNull("type")? COLLECT_COLUMN_TYPE_ELEMENT : col.getString("type"), // 수집 타입 time, url, element, keyword ...
								   elements,
								   col.isNull("data_type")? "text" : col.getString("data_type"),  // 데이터 저장 타입
								   col.isNull("data_name")? "" : col.getString("data_name"),
								   col.isNull("allow_null")? false : col.getBoolean("allow_null"),
								   regexs,
								   regexs_replace,
								   col.isNull("value")? null : col.getString("value"));
					}
					mCollects.add(recode);
				} catch (JSONException e) { 
					//mCollects.clear(); 
					System.out.println("Config err : collect_recode " + i + "row");
					//e.printStackTrace(); 
				}
			}
		}
			
		try{
			array = root.getJSONArray("scenario");
			len_i = array.length();
			int depth;
			JSONArray aryScenario, aryLoadCheckSelector;
			JSONObject scenObject;
			Scenario scenario;
			for(int i = 0 ; i < len_i ; i++){
				object = array.getJSONObject(i);
				aryScenario = object.getJSONArray("action");
				depth = object.getInt("depth");
				len_j = aryScenario.length();
				scenario = new Scenario(depth, len_j);
				mScenarios.put(depth, scenario);
				if(!object.isNull("check_load_selector")){
					aryLoadCheckSelector = object.getJSONArray("check_load_selector");
					for(Object selector: aryLoadCheckSelector){
						scenario.addLoadCheckSelector(String.valueOf(selector));
					}
				}
				for(int j = 0 ; j < len_j ; j++){
					scenObject = aryScenario.getJSONObject(j);
					scenario.add(scenObject.isNull("type")? "click": scenObject.getString("type"),
								 scenObject.isNull("target_depth")? -1: scenObject.getInt("target_depth"), 
								 scenObject.isNull("selector")? null : scenObject.getString("selector"),
								 scenObject.isNull("empty_selector")? null : scenObject.getString("empty_selector"),
								 scenObject.isNull("value")? null: scenObject.getString("value"),
								 scenObject.isNull("wait_time")? 30: scenObject.getInt("wait_time"),
								 scenObject.isNull("condition")? null: scenObject.getString("condition"),
								 scenObject.isNull("contents_depth")? -1: scenObject.getInt("contents_depth"),
								 scenObject.isNull("try_refresh")? -1: scenObject.getInt("try_refresh"));
				}
				for(int j = 0 ; j < scenario.getSize() ; j++){
					scenario.getAction(j);
				}
			}
		} catch (JSONException e){ 
			System.err.println("Config err : Scenario " + e.getMessage() );
			mScenarios.clear(); 
		}
		
		
		try{
			if(!root.isNull("word_dictionary")){
				mDictionary = new Dictionary();
				
				JSONObject jsonObjectDict = root.getJSONObject("word_dictionary");
				// 단어사전 DB 정보가 있는가?
				if(!jsonObjectDict.isNull("dictionary_db")){
					
				}
				// 단어사전 키워드가 설정파일에 있는가?
				// 목적 : 콘텐츠에 해당 키워드가 있는 데이터만 수집함
				if(!jsonObjectDict.isNull("collect_keyword")){
					array = jsonObjectDict.getJSONArray("collect_keyword");
					for(int i = 0 ; i < array.length() ; i++){
						mDictionary.addKeyword(array.getString(i));
					}
					
				}
			}
		}catch(JSONException e){
			System.err.println("Config err : Dictionary");
			return false;
		}
		
		return true;
	}
	
	public String[] getPostProcessingQuery(){
		return mPostProcessingQuery;
	}
	
	public long getStartTime(){
		return CRAWLING_START_TIME;
	}

	public Work getSeedInfo() {
		return mSeedInfo;
	}

	public Map<String, Robots> getRobots() {
		return mRobots;
	}

	public Map<Integer, Scenario> getScenarios() {
		return mScenarios;
	}

	public List<Duplicate> getFilterDuplicate() {
		return mFilterDuplicate;
	}
	
	public List<FilterModify> getFtilerModify(){
		return mFilterModify;
	}

	public List<String> getFilterAllow() {
		return mFilterAllow; // 허용된 URL(또는 패턴)
	}

	public List<String> getFilterDisallow() {
		return mFilterDisallow;
	}

	public List<String> getLeafURL() {
		return mLeafURL;// 크롤링 마지막 URL(또는 패턴) : 해당 페이지의 링크는 수집하지 않는다
	}

	public List<CollectRecode> getCollects() {
		return mCollects;// 수집할 콘텐츠
	}
	
	public Dictionary getDictionary(){
		return mDictionary;
	}
}