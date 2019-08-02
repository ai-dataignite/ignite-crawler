package com.onycom.crawler.core;

import java.util.List;

import com.onycom.crawler.data.Work;

/**
 * 워크 쓰레드의 작업을 모니터링 하기위한 리스너 
 * */
public interface WorkListener <T>{
	public boolean start(T data);
	
	public void done(T data, List<T> result);

	public void error(T data);
}
