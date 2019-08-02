package com.onycom.crawler.data;

import java.util.List;

/**
 * work 쓰레드의 결과를 패키징한 객체
 * */
public class WorkResult {
	Work mCurWorkInfo;
	List<Work> mAryNewWork;
	
	public WorkResult(Work curWork, List<Work> newWorks){
		mCurWorkInfo = curWork;
		mAryNewWork = newWorks;
	}
	
	public Work getCurWork(){
		return mCurWorkInfo;
	}

	public List<Work> getNewWorks(){
		return mAryNewWork;
	}
}	