package com.onycom.crawler.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.onycom.crawler.data.Work;
import com.onycom.crawler.writer.DBWriter;

/**
 * 크롤러가 작업할 페이지들의 목록을 관리하는 큐. 멀티쓰레드 작업시 반드시 동기화 필요
 */
public class WorkDeque {

	public static final byte LOCK = 0x0; // 0b0000 읽기 쓰기 불가
	public static final byte WRITE = 0x1; // 0b0001 쓰기만 가능
	public static final byte READ = 0x2; // 0b0010 읽기만 가능
	public static final byte READ_AND_WRINE = 0x3; // 읽기 쓰기 가능

	private byte ACCESS_MODE = READ_AND_WRINE; // 기본은 읽기 쓰기 모드

	/**
	 * 작업 해야할 페이지 목록
	 */
	private Deque<Work> mDueue;

	/**
	 * 방문한 페이지 목록 중복 페이지 방지
	 */
	List<Work> mHistory;

	private Queue<Work> mResultQueue;
	
	public WorkDeque() {
		mDueue = new LinkedList<Work>();
		mResultQueue = new LinkedList<Work>(); 
		mHistory = new ArrayList<Work>();
	}

	public synchronized void setAccessMode(byte mode) {
		ACCESS_MODE = mode;
	}

	public synchronized int getAccessMode() {
		return ACCESS_MODE;
	}

//	public boolean contains(Work urlInfo){
//		if ((ACCESS_MODE & WRITE) == WRITE) {
//			return mHistory.contains(urlInfo);
//		}else{
//			return false;
//		}
//	}
	
	public synchronized boolean offerURL(Work work) {
		if ((ACCESS_MODE & WRITE) == WRITE) {
			// 중복된 페이지 접근은 하지 않음
			mHistory.add(work);
			if(work.isHighPriority()){
				mDueue.addFirst(work);
			}else{
				mDueue.addLast(work);
			}
			//mQueue.
			return true;
//			if (mHistory.contains(urlInfo) == true) {
//				return false;
//			} else {
//				mHistory.add(urlInfo);
//				mQueue.offer(urlInfo);
////				
////				try {
////					mDB.connect().insertHistory(urlInfo.getURL()).close();
////				} catch (Exception e) {
////					e.printStackTrace();
////				} 
//				return true;
//			}
		} else {
			return false;
		}
	}

	public synchronized Work pollWork() {
		if ((ACCESS_MODE & READ) == READ)
			return mDueue.poll();
		else
			return null;

	}

	public synchronized boolean isEmpty() {
		return mDueue.isEmpty();
	}

	public synchronized long getSize() {
		return mDueue.size();
	}

	public long getHistorySize() {
		return mHistory.size();
	}

	public Work[] getHistory(){
		return mHistory.toArray(new Work[mHistory.size()]);
	}
	
	public void clear(){
		mDueue.clear();
	}
}
