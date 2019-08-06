package com.hdone.crawler.data;

import java.util.ArrayList;
import java.util.List;

public class Scenario {
	
	int mDepth;
	List<String> mAryLoadCheckSelector;
	List<Action> mAryAction;
	
	public Scenario(int depth, int size) {
		mDepth = depth;
		mAryAction = new ArrayList<Action>(size);
		mAryLoadCheckSelector = new ArrayList<String>();
	}
	
	public void add(int target_depth, String cssSelector){
		add(null, target_depth, cssSelector, null, null, 30, null, -1, -1);
	}
	
	public void add(String action, int target_depth, String cssSelector, String emptySelector,  String value, int wait_time, String condition, int contents_depth, int try_refresh){
		mAryAction.add(new Action(action, target_depth, cssSelector, emptySelector,  value, wait_time, condition, contents_depth, try_refresh));
	}
	
	public Action getAction(int idx){
		return mAryAction.get(idx);
	}
	
	public int getSize(){
		return mAryAction.size();
	}
	
	public void addLoadCheckSelector(String selector){
		mAryLoadCheckSelector.add(selector);
	}
	
	public List<String> getLoadCheckSelector(){
		return mAryLoadCheckSelector;
	}
}