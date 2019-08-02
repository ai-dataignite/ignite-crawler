package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.List;

public class Dictionary {
	String mDB_Path;
	String mDB_PW;
	String mDB_ID;
	
	List<String> mListKeyword;
	int mSizeListKeyword;
	
	public Dictionary(){
		mListKeyword = new ArrayList<String>();
		mSizeListKeyword = 0;
	}
	
	public void addKeyword(String keyword){
		mListKeyword.add(keyword);
		mSizeListKeyword ++;
	}
	
	public List<String> getKeyWordList(){
		return mListKeyword;
	}
	
	public boolean hasDict(){
		return mListKeyword.size() > 0;
	}
}
