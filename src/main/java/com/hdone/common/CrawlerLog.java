package com.hdone.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.hdone.SettingBasedCrawler.Crawler;

public class CrawlerLog {
	static Logger mLogger;
//	static Logger mProgreesLogger;
	static File mProgressFile;
	static BufferedWriter mProgressBWriter;
	static RandomAccessFile mProgressRaf;
	
	public static String LOGGER_NAME = "DEFALUT";
	public static String START_TIME = "";
	
	public static void SetName(String name, String time){
		LOGGER_NAME = name.replace(" ","_");
		START_TIME = time.replace(" ","_");
	}
	
	public static Logger GetInstanceSysout(Class logClass) {
//		String fileName = null;
//		String logName = LOGGER_NAME; 
		mLogger = Logger.getLogger(logClass);
		// 로그 파일 대한 패턴을 정의
		String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
		PatternLayout layout = new PatternLayout(pattern);

		// 날짜 패턴에 따라 추가될 파일 이름
		String datePattern = ".yyyyMMdd";
		ConsoleAppender appender = new ConsoleAppender(layout);
		appender.setThreshold(Level.ALL);
		mLogger.addAppender(appender);
		return mLogger;
	}
	
	public static boolean OpenProgressFile(String path) {
		String logName = LOGGER_NAME;
		
		if(mProgressFile == null) {
			if(path.isEmpty()) {
				path = Crawler.BASE_PATH + "/log/" + logName +"/"+ logName +"_progress.log";
			}else {
				path = path.replace("\\", "/");
				if(path.charAt(1) != ':' && !path.startsWith("/")) {
					path = Crawler.BASE_PATH + "/" + path;
				}
			}
			mProgressFile = new File(path);
			if (!mProgressFile.getParentFile().exists()) mProgressFile.getParentFile().mkdirs();
			
		    try {
		    	mProgressRaf = new RandomAccessFile(mProgressFile, "rwd");
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return false;
			}
//			
//			try {
//				mProgressBWriter = new BufferedWriter(new FileWriter(mProgressFile));
//			} catch (IOException e) {
//				e.printStackTrace();
//				return false;
//			}
		}
		return true;
	}
	
	public static void WriteProgress(String str) {
		if(mProgressRaf == null) return;
		if(mProgressFile.isFile() && mProgressFile.canWrite()){
			try {
				mProgressRaf.seek(0);
				mProgressRaf.setLength(0);
				mProgressRaf.write(StandardCharsets.UTF_8.encode(str).array());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void CloseProgressFile() {
		try {
			if(mProgressRaf != null)
				mProgressRaf.close();
			mProgressRaf = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Logger GetInstance(Class logClass) {
		String fileName = null;
		String logName = LOGGER_NAME;
		mLogger = Logger.getLogger(logClass);
		// 로그 파일 대한 패턴을 정의
		String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
		PatternLayout layout = new PatternLayout(pattern);

		// 날짜 패턴에 따라 추가될 파일 이름
		String datePattern = ".yyyyMMdd";

		DailyRollingFileAppender appender = null;
		try {
			String dir_path = Crawler.BASE_PATH+ "/log/" + logName;
			File dirFile = new File(dir_path);
			if (!dirFile.exists()) dirFile.mkdirs();
			
			fileName = dir_path + "/" + START_TIME + "_error.log";
			appender = new DailyRollingFileAppender(layout, fileName, datePattern);
			appender.setThreshold(Level.ERROR);
			mLogger.addAppender(appender);

			fileName = dir_path + "/" + START_TIME + "_all.log";
			appender = new DailyRollingFileAppender(layout, fileName, datePattern);
			appender.setThreshold(Level.ALL);
			mLogger.addAppender(appender);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		ConsoleAppender appender2 = new ConsoleAppender(layout);
		appender.setThreshold(Level.ALL);
		mLogger.addAppender(appender2);
		return mLogger;
	}
}