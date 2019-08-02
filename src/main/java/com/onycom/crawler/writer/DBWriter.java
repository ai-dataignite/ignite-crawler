package com.onycom.crawler.writer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.KeyValue;
import com.onycom.SettingBasedCrawler.Crawler;
import com.onycom.common.CrawlerLog;
import com.onycom.crawler.data.CollectRecode;

/**
 * DB 저장 구현제. 기본 JDBC 를 활용. 안정성을 위한 구현 고도화 필요
 */
public class DBWriter implements Writer {
	static Logger mLogger = CrawlerLog.GetInstance(DBWriter.class);
	Config mConfig;

	static String DRIVER = "org.mariadb.jdbc.Driver";
	static String PATH = "jdbc:mariadb://localhost:3306/DEV_CRAWLER_LOG"; // 172.17.0.10
	static String USER = "root";
	static String PW = "thqkr";

	static final String Q_INSERT_HISTORY = "INSERT INTO T_HISTORY (URL) VALUES (\"%s\")";
	static final String Q_INSERT_ERR = "INSERT INTO T_ERR_URL (URL,REASON) VALUES (\"%s\", \"%s\")";
	static final String Q_INSERT_FILTER = "INSERT INTO T_FILTER_URL (URL,LINK_URL, REASON) VALUES (\"%s\", \"%s\", \"filter\")";

	static final String Q_INSERT_CONTENTS = "INSERT INTO %s (%s) VALUES (%s)";
	
	static final String Q_INSERT_OR_UPDATE_CONTENTS = "INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s";

	//	static final String Q_CREATE_CONTENTS_TABLE = "CREATE TABLE IF NOT EXISTS %s ("
//			+ "ROW_ID INT(11) NOT NULL AUTO_INCREMENT %s, PRIMARY KEY (ROW_ID %s))";
	
	static final String Q_CREATE_CONTENTS_TABLE = "CREATE TABLE IF NOT EXISTS %s (%s %s)";

	Connection mConn;

	public synchronized boolean open() throws Exception {
		try {
			if(mConn == null || mConn.isClosed()) {
				Connection conn = null;
				Properties properties = new Properties();
				properties.put("connectTimeout", "300000");
				String dbConnectionString;
				if(PATH.indexOf("sqlite") != -1){
					mLogger.info("Load db : sqlite");
					Class.forName("org.sqlite.JDBC");
					dbConnectionString = PATH;
					conn = org.sqlite.JDBC.createConnection(dbConnectionString, properties);
				}else if(PATH.indexOf("mariadb") != -1){
					mLogger.info("Load db : mariadb");
					Class.forName(DRIVER);
					properties.put("validationQuery", "select 1");
					dbConnectionString = "jdbc:" + PATH + "?user=" + USER + "&password=" + PW +"&autoReconnect=true";
					conn = DriverManager.getConnection(dbConnectionString, properties);
				}else {
					mLogger.error("[DB] Not support DB");
					return false;
				}	
				mConn = conn;
				
				// 콘텐츠 저장을 위한 DB TABLE 을 준비
				String keys= null;
				String query = null;
				try {
					List<CollectRecode> collects = mConfig.getCollects();
					String tableName, colName, colType;
					for (CollectRecode c : collects) {
						// TABLE NAME
						tableName = c.getName();//.toUpperCase();
						query = "";
						keys = "";
						for (CollectRecode.Column col : c.getColumns()) {
							colName = col.getDataName();//.toUpperCase(); // 컬럼 명
							colType = col.getDataType(); // 컬럼 타입
							if(query.isEmpty()){
								query = colName + " " + colType;
							}else{
								query += ", " + colName + " " + colType;
							}
							if(col.isKey()){
								if(keys.isEmpty()) {
									keys= ",PRIMARY KEY (" + colName;
								}else{
									keys += ", " + colName;
								}
							}
						}
						if(!keys.isEmpty()) keys+=")";
						if (query.length() > 0) {
							query = String.format(Q_CREATE_CONTENTS_TABLE, tableName, query, keys);
							insert(query);
						}
					}
				} catch (Exception e) {
					mLogger.error("[SQL] " + query, e.fillInStackTrace());
					close();
					return false;
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			mLogger.error(e.getMessage());
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isClosed() throws Exception {
		return mConn.isClosed();
	}

//	public synchronized int writeHistory(String url) {
//		try {
//			return insert(String.format(Q_INSERT_HISTORY, url));
//		} catch (SQLNonTransientConnectionException e) {
//			mLogger.error(e.getMessage(), e.fillInStackTrace());
//			return 0;
//		}
//	}
//
//	public synchronized int writeErr(String url, String reason) {
//		try {
//			return insert(String.format(Q_INSERT_ERR, url, reason));
//		} catch (SQLNonTransientConnectionException e) {
//			mLogger.error(e.getMessage(), e.fillInStackTrace());
//			return 0;
//		}
//	}

	public synchronized int insert(String query) throws Exception {
		mConn.prepareStatement(query).execute();
		return 1;
	}

	public synchronized void close() {
		if (mConn != null) {
			try {
				mConn.close();
			} catch (SQLException e) {
				mLogger.error(e.getMessage(), e.fillInStackTrace());
			}
		}
	}

	public void setConfig(Config config) {
		PATH = config.DB_PATH;
		USER = config.DB_ID;
		PW = config.DB_PW;
		mConfig = config;
		String keys= null;
		String query = null;
	}

	public synchronized int write(String... values) throws Exception {
		return 0;
	}

	public synchronized int write(Contents contents) throws Exception {
		String tableName = contents.getName();
		List<KeyValue> data = contents.getData();

		String cols = "";
		String values = "";
		String update =  "";
		String v = "";
		boolean isFisrt = true;
		for (KeyValue kv : data) {
			if (kv != null) {
				if (isFisrt) {
					cols += kv.key();
					v = kv.value().replace("\"", "\\\"");
					values += "\"" + v + "\"";
					isFisrt = false;
					if(contents.isInsertOrUpdate()) {
						update += (kv.key() + "=\"" + v + "\"");
					}
				} else {
					cols += "," + kv.key();
					v = kv.value().replace("\"", "\\\"");
					values += ", \"" + v + "\"";
					if(contents.isInsertOrUpdate()) {
						update += (", " + kv.key() + "=\"" + v + "\"");
					}
				}
				
				if(kv.type().equalsIgnoreCase("file")){
					// 파일 다운로드에 대한 처리?
				}
			}
			
		}
		int ret = 0;
		try {
			if(contents.isInsertOrUpdate()) {
				ret = insert(String.format(Q_INSERT_OR_UPDATE_CONTENTS, tableName, cols, values, update));
			}else {
				ret = insert(String.format(Q_INSERT_CONTENTS, tableName, cols, values));
			}
		} catch (Exception e) {
			throw e;
//			open();
//			ret = insert(String.format(Q_INSERT_CONTENTS, tableName, cols, values));
		}
//		if(ret == -1){
//			mLogger.error(e2.getMessage(), e2.fillInStackTrace());
//		}
		return ret;
	}

	public synchronized int write(List<Contents> aryContents) {
		// TODO Auto-generated method stub
		return 0;
	}
}