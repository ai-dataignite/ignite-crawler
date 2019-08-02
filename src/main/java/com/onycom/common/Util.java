package com.onycom.common;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.onycom.crawler.data.Work;

/**
 * 유틸성 메서드 모음
 * */
public class Util  {
	
	static String[] SPLIT_URL_TOKEN = {"/", "?" ,"#"};
	
	public static URL GetURL(String src_url, String des_url){
		URL ret = null;
		try {
			
			if(des_url.startsWith("http")){
				ret = new URL(des_url);
			}else{
				//String domain;
				int find;
				if(des_url.charAt(0) == '/'){
					find = src_url.substring(8).indexOf("/"); 
					if(find != -1){
						ret = new URL(src_url.substring(0, 8+find) + des_url);
					}else{
						ret = new URL(src_url += des_url);
					}
				}else if(des_url.charAt(0) == '?'){
					ret = new URL(src_url);
					ret = new URL(ret.getProtocol() + "://" 
							+ ret.getHost()
							+ ((ret.getPort() != -1)? ":"+ret.getProtocol() : "")
							+ ret.getPath()+des_url);
				}else if(des_url.charAt(0) == '#'){
					ret = new URL(src_url);
				}else {
					ret = new URL(src_url += des_url);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		
		return ret;
	}
	
	public static String GetDomain(URL url){
		return url.getProtocol() + "://" 
				+ url.getHost()
				+ ((url.getPort() != -1)? ":"+url.getProtocol() : "");
	}
	
	
	public static String[] SplitDomainAndSubURL(Work urlInfo, String target){
		String[] ret = new String[2];
		int idx;
		if(target.matches("^(http).*$")){
			for(String t : SPLIT_URL_TOKEN){
				idx = target.indexOf(t, 8);
				if(idx != -1){
					ret[0] = target.substring(0, idx);
					ret[1] = target.substring(idx);
					break;
				}
			}
			if(ret[0] == null){
				ret[0] = target;
				ret[1] = "/";
			}
		}else{
			//String parentUrl = urlInfo.getURL();
			String domainUrl = urlInfo.getDomainURL();
			String parentSubUrl = urlInfo.getSubURL();
			
			// ./* 일때 파싱 처리
			if(target.matches("^(\\./).*$")){
				idx = parentSubUrl.lastIndexOf("/");
				if(idx != -1){
					ret[0] = domainUrl;
					ret[1] = parentSubUrl.substring(0, idx) + target.substring(1);
				}else{
					ret[0] = domainUrl;
					ret[1] = target.substring(1);
				}
				
			// ../* 일때 파싱 처리
			}else if(target.matches("^(\\.\\./).*$")){
				idx = parentSubUrl.lastIndexOf("/");
				if(idx != -1){
					idx = parentSubUrl.substring(0, idx).lastIndexOf("/");
				}
				
				if(idx != -1){
					ret[0] = domainUrl;
					ret[1] = parentSubUrl.substring(0, idx) + target.substring(2);
				}else{
					ret[0] = domainUrl;
					ret[1] = target.substring(1);
				}
				ret[0] = domainUrl;
				ret[1] = target.substring(2);
			}else{
				ret[0] = domainUrl;
				ret[1] = target;
			}
		}
		return ret;
	}
	
	public static CharSequence GetFileToString(String path){
		File file = new File(path);
		char[] ch = new char[(int) file.length()];
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			br.read(ch);
			sb.append(ch);
		} catch (IOException e) {
			return null;
		}
		//System.out.println(sb.toString());
		return sb.toString();
	}
	
	public static String ConvertForRegex(String str){
		str = str.replace("?", "\\?");
		str = str.replace(".", "\\.");
		return str;
	}
	
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	public static String Encoding(String str, String encodeType){
		try {
			str = URLEncoder.encode(str, encodeType);
		} catch (UnsupportedEncodingException e) {
			str = null;
		}
		return str;
	}
	
	public static String Remove4ByteEmoji(String str){
		Pattern ptEmoji = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+");
		Matcher m = ptEmoji.matcher(str);
		return m.replaceAll(" ");
	}
	
	public static float CalcExpiredTime(Date startDate){
		return ((new Date().getTime() - startDate.getTime())/ 1000f); 
	}
	
	public static String GetCssSelector(Element e){
		String tagName = e.tagName();
		if(tagName.equalsIgnoreCase("html")) return tagName;
		String selector = ":nth-child("+ (e.elementSiblingIndex() + 1) +")";
		if(e.parent() == null || (e.parent() instanceof Document)){
			return null;
		}else{
			String parent = GetCssSelector(e.parent());
			if(parent != null){
				return parent + " > " + selector;
			}else{
				return selector;
			}
		}
	}
	
	public static boolean WriteFile(String path, String data){
		File file = new File(path);
		FileWriter fw;
		try {
			fw = new FileWriter(file);
			fw.write(data);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public static boolean DownloadFileFromURL(String downloadUrl, String outFilePath){
		boolean ret = false;
		try {
			//String imgUrl = "http://pics.avs.io/200/200/" + code[i] + ".PNG";// http://~~~~~~~~.jpg;  // image url http://pics.avs.io/200/200/KE.png
			URL url;
			url = new URL(downloadUrl);
			String dirName = outFilePath.substring( 0, outFilePath.lastIndexOf('/')); // 이미지 파일명 추출
			String ext = outFilePath.substring( outFilePath.lastIndexOf('.')+1, outFilePath.length() );  // 이미지 확장자 추출
			BufferedImage img;
			img = ImageIO.read(url);
			File file = new File(dirName);
			if(!file.exists()) file.mkdirs();
			ImageIO.write(img, ext, new File(outFilePath));
			ret = true;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean CheckHangul(char cValue) {
		if (cValue >= 0xAC00 && cValue <= 0xD743) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean CheckEng(char cValue){
		if (cValue >= 'a' && cValue <= 'z' || cValue >= 'A' && cValue <= 'Z') {
			return true;
		} else {
			return false;
		}
	}
	
	public static String GetElapedTime(long e){
		e = System.currentTimeMillis() - e;
		e = e / 1000;
		int h=0,m=0,s=0;
		if(e > 0){
			h = (int) e / (3600);
			m = (int) e % 3600;
			m = (m > 0)? m/60 : 0;
			s = (int) e % 60;
		}
		return String.format("%02d:%02d:%02d", h, m, s);
	}
	
	static final String NEWLINE_KEY = "-40h9a-LINE-40h9a-";
	static final String[] HTML_NEWLINE_TEG = { "</p>",
											   "</br>",
											   "<br/>",
											   "<br>",
											   "</h1>",
											   "</h2>",
											   "</h3>",
											   "</h4>",
											   "</h5>",
											   "</h6>",
											   "</ol>",
											   "</ul>",
											   "</li>",
											   "</dd>",
											   "</dt>",
											   "</div>",
											   "</dt>",
											   "</dl>"};
	
	public static String htmlToPlainText(String html) {
		String ret = null;
		boolean convert = false;
		for(String tag : HTML_NEWLINE_TEG) {
			convert = true;
			html = html.replace(tag, NEWLINE_KEY+tag);
		}
		if(convert)
			ret = Jsoup.parse(html).text().replace(NEWLINE_KEY, "\n");
		else
			ret = Jsoup.parse(html).text();
		return ret;
	}
}
