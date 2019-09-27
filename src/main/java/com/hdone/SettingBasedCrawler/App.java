package com.hdone.SettingBasedCrawler;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Hello world!
 */

public class App  
{
	
	
	
    public static void main(String[] args)
    {
//		CodeSource codeSource = App.class.getProtectionDomain().getCodeSource();
//		File jarFile = new File(codeSource.getLocation().getPath());
//		String jarDir = jarFile.getParentFile().getPath();
//		Crawler.BASE_PATH = jarDir;
    	
    	Crawler.BASE_PATH = new File(".").getAbsoluteFile().getParentFile().getAbsolutePath();
    	
		System.out.println("BASE_PATH : " + Crawler.BASE_PATH);
		
    	String config_path = null;
    	/*
    	 * 필수 파마리터 조회
    	 * */
    	
    	String configString = null;
    	
    	if (args == null || args.length < 1) {
    		System.out.println("require parameter : --config_file=[FILE_PATH]");
			return;
    	}
    	
    	if (!ArgumentData.Parse(args)) return;
    	
		// ap 프로젝트를 위한 커스텀 구문
    	// from_date 와 to_date 필수 yyyyMMdd (없다면 현재시간으로 지정)
    	// 
		if(ArgumentData.sMapParams != null) {
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); 
			String strTime = sdf.format(cal.getTime());
			if(ArgumentData.sMapParams.get("from_date") == null) {
				// from_date 가 없다면 현재월의 1일부터 크롤링 시작
				ArgumentData.sMapParams.put("from_date", strTime.substring(0,6)+"01");
			}else if(ArgumentData.sMapParams.get("from_date").length() == 4) {
				ArgumentData.sMapParams.put("from_date", (ArgumentData.sMapParams.get("from_date") + "0101"));
			}
			if(ArgumentData.sMapParams.get("to_date") == null) {
				ArgumentData.sMapParams.put("to_date", strTime);
			}else if (ArgumentData.sMapParams.get("to_date").length() == 4) {
				ArgumentData.sMapParams.put("to_date", (ArgumentData.sMapParams.get("to_date") + "1231"));
			}
			try {
				if(sdf.parse(ArgumentData.sMapParams.get("from_date")).getTime() > sdf.parse(ArgumentData.sMapParams.get("to_date")).getTime()) {
					String tmp = ArgumentData.sMapParams.get("from_date");
					ArgumentData.sMapParams.put("from_date", ArgumentData.sMapParams.get("to_date"));
					ArgumentData.sMapParams.put("to_date", tmp);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
    	

		
		
    	Crawler c = new Crawler(1, 0);
    	boolean ready = c.setConfig(ArgumentData.sConfigType, 
    				ArgumentData.sStrConfigURI, 
    				ArgumentData.sDB_id, 
    				ArgumentData.sDB_pw,
    				ArgumentData.sDB_path, 
    				ArgumentData.sMapParams);
    	if(ready) {
    		c.start();
    	}else {
    		System.err.println("[err] config file err");
    	}
    	
//    	if(args != null){
//    		int len = args.length;
//    		if(len >= 2){
//    			if(args[0].equalsIgnoreCase("-config_file")){
//    				config_path = args[1];
//    				
//    			}else if(args[0].equalsIgnoreCase("-config_url")){
//    				config_path = args[1];
//    				
//    			}else if(args[0].equalsIgnoreCase("-config_sql")){
//    				config_path = args[1];
//    				
//    			}else{
//    				System.out.println("require parameter : -config_file [FILE_PATH]]");
//    				return;
//    			}
//    		}else{
//    			System.out.println("require parameter : -config_file [FILE_PATH]");
//    			return;
//    		}
//    	}
    	
    	// 
    	
//    	JsoupScraper jsoupScraper = new JsoupScraper();
//    	try {
//			Document doc = jsoupScraper.getDocument(new Work(config_path));
//			if(doc != null) return;
//			System.out.println(doc.text());
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//    	
//    	/**
//    	 * 파일 로딩 작업
//    	 * */
//    	File configFile = new File(config_path);
//    	if(!configFile.exists()){
//    		System.err.println("[ERROR] Config file is not exists");
//    		return;
//    	}
    	
//    	String[][] params = null;
    	/**
    	 * 크롤러 로딩 시작
    	 * */
		
		
		
//		if(args.length >= 4){
//			if(args[2].equalsIgnoreCase("-params")){
//				params = new String[1][args.length - 3];
//				for(int j = 3 ;j  < args.length ; j++){
//					params[0][j-3] = args[j];
//				}
//			}
//			for(int i = 0 ; i < params.length ; i++){
//				for(int j = 0; j < params[i].length ; j++){
//	    			c.setConfig(config_path, null, params[i]);
//	    	    	c.start();
//				}
//			}
//		}else{
//			params = null;
//			c.setConfig(config_path, null, null);
//	    	c.start();
//		}
    	
    }
}