package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 콘텐츠의 레코드 하나를 담는 객체..
 * */
public class Contents {
	String mName;
	boolean mInsertOrUpdate = false;
	List<KeyValue> mData;
	
	public Contents(String name, int len){
		mName = name;
		mData = new ArrayList<KeyValue>(len);
	}
	
	public void setInsertOrUpdate(boolean bool) {
		mInsertOrUpdate = bool;
	}
	
	public boolean isInsertOrUpdate() { 
		return mInsertOrUpdate;
	}
	
	public String getName(){
		return mName;
	}
	
	public void add(String key, String type, String value){
		mData.add(KeyValue.Create(key, type, value));
	}
	
	public void add(int idx, String key, String type, String value){
		mData.add(KeyValue.Create(key, type, value));
	}
	
	public List<KeyValue> getData(){
		return mData;
	}
	
	public int size(){
		return mData.size();
	}
	
	public KeyValue get(int idx){
		return mData.get(idx);
	}
}
