package com.onycom.crawler.core;

import java.util.List;

import com.onycom.crawler.data.Work;

/**
 * 쓰레드를 관리하는 크롤러 총괄 매니저의 상황을 모니터링 하기위한 리스너 인터페이스. 
 * */
public interface WorkManagerListener {
	public boolean start();
	
	public void progress(Work work, WorkDeque workDeque);

	public void finish(WorkDeque workDeque);
	
	public void error();
}
