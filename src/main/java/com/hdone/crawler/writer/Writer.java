package com.hdone.crawler.writer;

import java.util.List;

import com.hdone.crawler.data.Config;
import com.hdone.crawler.data.Contents;

/**
 * 콘텐츠 저장 인터페이스
 * */
public interface Writer {
	/** 
	 * 열기 */
	public boolean open() throws Exception ;
	
	
	public boolean isClosed() throws Exception;
	
	/** 
	 * 종료 */
	public void close() throws Exception ;
	
	/** 
	 * 설정 파일 세팅 */
	public void setConfig(Config config);
	
	/**
	 * 콘텐츠 저장 
	 * @param values 반드시 첫번째인자는 데이터셋의 이름, 이후는 데이터 */
	public int write(String... values) throws Exception ;
	
	/**
	 * 콘텐츠 저장
	 * @param contents 저장할 콘텐츠 객체 */
	public int write(Contents contents) throws Exception ;
	
	/** 
	 * 콘텐츠 저장
	 * @param aryContents 저장할 콘텐츠 객체 배열 */
	public int write(List<Contents> aryContents) throws Exception ;
}