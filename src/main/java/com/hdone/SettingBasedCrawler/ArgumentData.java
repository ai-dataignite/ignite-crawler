package com.hdone.SettingBasedCrawler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.util.DateTime;
import com.google.gdata.wireformats.GeneratorUtils;

public class ArgumentData {
	public static int TYPE_FILE = 0;
	public static int TYPE_URL = 1;
	public static int TYPE_DB = 2;
	
	public static int sConfigType;
	public static String sStrConfigURI;
	
	public static String sDB_id = "";
	public static String sDB_pw = "";
	public static String sDB_path = "";
	
	public static Map<String,String> sMapParams;
	
	static String[] P_KEY = {"-config_file"};
	
	public static boolean Parse(String[] args){
		boolean ret = false;
		int idx = 0;
		int length = args.length;
		String arg, value;
		try {
			while(idx < length) {
				arg = args[idx++];
				arg = arg.trim();
				if(arg.startsWith("--config_file=")) {
					int s = arg.indexOf("=");
					sStrConfigURI = arg.substring(s+1);
					sConfigType = TYPE_FILE;
				}else if(arg.startsWith("--config_url=")) {
					int s = arg.indexOf("=");
					sStrConfigURI = arg.substring(s+1);
					sConfigType = TYPE_URL;
				}else if(arg.startsWith("--config_sql=")) {
					int s = arg.indexOf("=");
					sStrConfigURI = arg.substring(s+1);
					// "/ 제거
					sStrConfigURI.substring(1, sStrConfigURI.length()-2);
					sConfigType = TYPE_DB;
				}else if(arg.startsWith("--db_id=")) {
					int s = arg.indexOf("=");
					sDB_id = arg.substring(s+1);
				}else if(arg.startsWith("--db_pw=")) {
					int s = arg.indexOf("=");
					sDB_pw = arg.substring(s+1);
				}else if(arg.startsWith("--db_path=")) {
					int s = arg.indexOf("=");
					sDB_path = arg.substring(s+1);
				}else if(arg.startsWith("--param=")) {
					int s = arg.indexOf("=");
					value = arg.substring(s+1);
					sMapParams = new HashMap<String,String>();
					String[] list = value.split("&");
					for(int i = 0 ; i < list.length ; i++) {
						String[] kv = list[i].split("=");
						sMapParams.put(kv[0], kv[1]); 
					}
				}
			}
			
			/**
			 * 유효성 검사
			 * */
			if (sStrConfigURI == null || sStrConfigURI.isEmpty()) {
				System.out.println("require parameter : --config_file(url/sql)=[FILE_PATH]]");
				return false;
			}
			
			if(sConfigType == TYPE_DB) {
				if (sDB_id.isEmpty()) {
					System.out.println("require parameter : --db_id=[ID]");
				}else if (sDB_pw.isEmpty()) {
					System.out.println("require parameter : --db_pw=[PASSWD]");
				}else if (sDB_path.isEmpty()){
					System.out.println("require parameter : --db_path=[PATH]");
				}else {
					ret = true;
				}
			}
			
			ret = true;
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			
		}
		return ret;
	}
}
