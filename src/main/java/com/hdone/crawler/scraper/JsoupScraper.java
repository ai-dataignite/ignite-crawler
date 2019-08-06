package com.hdone.crawler.scraper;

import com.hdone.SettingBasedCrawler.Crawler;
import com.hdone.common.CrawlerLog;
import com.hdone.crawler.data.Config;
import com.hdone.crawler.data.Cookie;
import com.hdone.crawler.data.Work;

import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class JsoupScraper implements Scraper{
    Logger mLogger = CrawlerLog.GetInstance(getClass());

    public static String charset = "UTF-8";
    private static Cookie mCookie;

    public JsoupScraper() {
        mCookie = new Cookie();
    }

//    public static void SetConfig(Config config) {
//        TYPE = config.CRAWLING_TYPE;
//    }

    public boolean open() {
        return true;
    }

    public void close() {

    }

//    public String getText() {
//    	
//    }
    
    public Document getDocument(Work work) throws Exception{
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        } };

        SSLContext sc;
        sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        Document doc = null;
        Connection conn = Jsoup.connect(work.getURL());
        
        // Map<String,String> cookies = mCOOKIE.get(urlInfo.getRootURL());
        // if(cookies != null) conn.cookies(cookies);
        conn.userAgent(Crawler.USER_AGENT_NAME);
        if (work.getContentType() == Work.POST) {
        	if (work.getData() != null && work.getData().size() > 0) {
                for (String key : work.getData().keySet()) {
                    if(key != null && !key.isEmpty()) {
                        conn.data(key, work.getData().get(key));
                    }
                }
            }
            doc = conn.ignoreContentType(true).post();
        } else { // urlInfo.getType() == URLInfo.GET
            doc = conn.ignoreContentType(true).get();
        }
        doc.outputSettings().prettyPrint(false);
        // mCOOKIE.put(urlInfo.getRootURL(), conn.response().cookies());
        mLogger.info("url, " + ((work.getAction() != null) ? work.getAction().getType() +", ": "click, ") 
        					+ work.toString());
        return doc;
    }
    
	public void clear() {
		// TODO Auto-generated method stub
		
	}

}
