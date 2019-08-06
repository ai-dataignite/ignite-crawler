package com.hdone.crawler.core;

import java.util.LinkedList;
import java.util.Queue;

import com.hdone.crawler.data.WorkResult;

public class WorkResultQueue {
	Queue<WorkResult> mQueue;
	
	public WorkResultQueue() {
		mQueue = new LinkedList<WorkResult>();
	}
	
	public synchronized boolean offerResult(WorkResult wr) {
		return mQueue.offer(wr);
	}
	
	public synchronized WorkResult pollResult(){
		return mQueue.poll();
	}
	
	public synchronized int size(){
		return mQueue.size();
	}
	
	public void resultNotifyAll(){ 
		synchronized(mQueue) {
			mQueue.notifyAll();
		}
	}
	
	public void resultWait(){
		try {
			synchronized(mQueue) {
				mQueue.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
