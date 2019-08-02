package com.onycom.crawler.data;

public class KeyValue {
	String key;
	String value;
	String type;
	
	static KeyValue Create(String key, String type, String value){
		return new KeyValue(key, type, value);
	}
	
	public KeyValue(String key, String type, String value){
		this.key = key;
		this.value = value;
		this.type = type;
	}
	
	public String key(){
		return this.key;
	}
	
	public String value(){
		return this.value;
	}
	
	public String type(){
		return this.type;
	}
	
	public void key(String key){
		this.key = key;
	}
	
	public void value(String value){
		this.value = value;
	}
}
