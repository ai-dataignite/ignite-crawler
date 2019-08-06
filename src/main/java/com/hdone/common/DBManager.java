package com.hdone.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.hdone.crawler.data.DataMap;

public class DBManager {
	Connection mConn;
	
	public boolean open(String path, String id, String pw){
		Properties properties = new Properties();
		properties.put("connectTimeout", "300000");
		String dbConnectionString = "jdbc:"+ path + "?user=" + id + "&password=" + pw;
		try {
			mConn = DriverManager.getConnection(dbConnectionString, properties);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public List<DataMap> select(String query){
		List<DataMap> list = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		ResultSetMetaData rsmd;
		try {
			statement = mConn.prepareStatement(query);
			rs = statement.executeQuery();
			rsmd = rs.getMetaData();
			DataMap row;
			String key;
			int type;
			list = new ArrayList<DataMap>();
			while(rs.next()){
				row = new DataMap();
				for(int i = 1 ; i <= rsmd.getColumnCount() ; i++){
					key = rsmd.getColumnLabel(i);
					type = rsmd.getColumnType(i);
					//System.out.println(key + "  "+type);
					switch(type){
					case Types.INTEGER : 
						//row.put(key, generator.nextInt(5));
						row.put(key, rs.getInt(key));    
					break;
					case Types.BIGINT : 
						//row.put(key, generator.nextInt(5));
						row.put(key, rs.getLong(key));    
						break; 
					case Types.FLOAT :   row.put(key, rs.getFloat(key));  break;
					case Types.DOUBLE :  row.put(key, rs.getDouble(key)); break;
					default:             row.put(key, rs.getString(key));
					}
				}
				list.add(row); 
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		} finally { 
//			try{rs.close();} catch(Exception e){}
//			try{statement.close();} catch(Exception e){}
		}
		return list;
	}
	
	public void close(){
		if (mConn != null) {
			try {
				mConn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
