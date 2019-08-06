package com.hdone.crawler.data;

public class Action{
//	public static String TARGET_BLANK = "blank";
//	public static String TARGET_SELF = "self";
	public static String TYPE_URL = "url";
	public static String TYPE_CLICK = "click";
	public static String TYPE_NEW_WINDOW_CLICK = "new_window_click";
	public static String TYPE_CLEAR = "clear";
	public static String TYPE_INPUT = "input";
	public static String TYPE_VERTICAL_SCROLL_BY = "vertical_scroll_by";
	public static String TYPE_VERTICAL_SCROLL_TO = "vertical_scroll_to";
	public static String TYPE_SELECT = "select";
	public static String TYPE_JAVASCRIPT = "javascript"; 
	public static String TYPE_SWITCH_WINDOW = "switch_window";
	public static String TYPE_CLOSE_WINDOW = "close_window";
	public static String TYPE_PARSE_CONTENTS = "parse_contents";
	public static String TYPE_BACKWORD_WINDOW = "backward_window";
	public static String TYPE_FORWORD_WINDOW = "forword_window";
	public static String TYPE_REFRESH_WINDOW = "refresh_window";
	public static String TYPE_CLOSE_POPUP = "close_popup";
	public static String TYPE_REMOVE_ELEMENTS = "remove_elements";
	public static String TYPE_SLEEP = "sleep";
	public static String TYPE_START_MONITOR_AJAX = "start_monitor_ajax";
	public static String TYPE_WAIT_AJAX = "wait_ajax";

	/**
	 * depth 에 따라 시나리오가 흘러감
	 * 무한루프 주의 필요
	 * 스크롤 같은 기능에서 0depth 페이지에서 스크롤 action에 depth 0 줄경우
	 * 무한 루프에 빠짐
	 * 따라서 scroll 처럼 페이지에 변화가 없는 경우 설정에서 반드시 -1 값을 넣거나 생략해야함 
	 * */
	int no = -1;
	int target_depth;
	int action_idx;
	int wait_time;
	int contents_depth;
	int try_refresh;
	String cssSelector;
	String emptySelector;
	String type;
	String value;
	String condition;
	
	public Action(String type, int target_depth, String value) {
		this.type = type;
		this.target_depth = target_depth;
		this.value = value;
	}
	
	public Action(int contents_depth) {
		if(contents_depth > -1) {
			this.type = TYPE_PARSE_CONTENTS;
			this.contents_depth = contents_depth;
		}
	}
	
	public Action(String type, int target_depth, String cssSelector, String emptySelector, String value, int wait_time, String condition, int contents_depth, int try_refresh){
		this.target_depth = target_depth;
		this.cssSelector = cssSelector;
		if(type == null){
			this.type = TYPE_URL;
		}else{
			this.type = type;
		}
		this.wait_time = wait_time;
		this.value = value;
		this.emptySelector = emptySelector;
		this.condition = condition;
		this.contents_depth = contents_depth;
		this.try_refresh = try_refresh;
	}
	
	public void setNo(int no) {
		this.no = no;
	}
	
	public int getNo() {
		return this.no;
	}
	
	public void setEmptySelector(String selector){
		emptySelector = selector;
	}
	
	public String getEmptySelector(){
		return emptySelector;
	}
	
	public int getTargetDepth(){
		return target_depth;
	}
	
	public void setValue(String value){
		this.value = value;
	}
	
	public int getWaitTime() {
		return this.wait_time;
	}
	
	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	public int getContentsDepth() {
		return this.contents_depth;
	}
	
	public int getTryRefresh() {
		return this.try_refresh;
	}
	
	public String getType(){
		return type;
	}
	
	public String getSelector() {
		return cssSelector;
	}
	
	public String getValue(){
		return value;
	}
	
	public String getCondition() {
		return condition;
	}
}
