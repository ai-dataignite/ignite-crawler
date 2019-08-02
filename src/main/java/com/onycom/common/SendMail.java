package com.onycom.common;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.log4j.Logger;

import com.onycom.crawler.scraper.SeleniumScraper;

import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

public class SendMail {
	static Logger mLogger = CrawlerLog.GetInstance(SendMail.class);
	MimeMessage msg;
	InternetAddress to;
	
	public SendMail(String senderEmail, String name,String pw) {
		final String email = senderEmail;
		final String pwd = pw;
		Authenticator auth = new Authenticator() {

			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(email, pwd);
			}

		};
		
		if( senderEmail.indexOf("@gmail.com") != -1) {
			Properties props = System.getProperties();
			props.setProperty("mail.smtp.starttls.enable", "true"); // gmail은 무조건 true 고정
			props.setProperty("mail.smtp.auth", "true"); // gmail은 무조건 true 고정
			props.setProperty("mail.smtp.host", "smtp.gmail.com"); // smtp 서버 주소
			props.setProperty("mail.smtp.port", "587"); // gmail 포트
			
			Session session = Session.getDefaultInstance(props, auth);
			msg = new MimeMessage(session);
			try {
				to = new InternetAddress(senderEmail, name);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				mLogger.error(e);
			}
		}
	}
	
	public SendMail To(String receiverEmail, String name) {
		try {
			msg.addRecipient(RecipientType.TO, new InternetAddress(receiverEmail, name));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			mLogger.error(e);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mLogger.error(e);
		}
		return this;
	}
	
	public SendMail setMessage(String title, String body) {
		if (msg == null) return this;
		try {
			msg.setSubject(title);
			msg.setText(body);
		} catch (MessagingException e) {
			e.printStackTrace();
			mLogger.error(e);
		}
		
		return this;
	}
	
	public SendMail addFile(String filePath) {
		if (msg == null) return this;
		Multipart mp = new MimeMultipart();
		MimeBodyPart mbp1 = new MimeBodyPart();
		try {
			mp.addBodyPart(mbp1);
			if (filePath != null) {
				if (fileSizeCheck(filePath)) {
					MimeBodyPart mbp2 = new MimeBodyPart();
					FileDataSource fds = new FileDataSource(filePath);
					mbp2.setDataHandler(new DataHandler(fds));
					mbp2.setFileName(MimeUtility.encodeText(fds.getName(), "UTF-8", "B"));
					mp.addBodyPart(mbp2);
				} else {
					throw new Exception("file size overflow !");
				}
			}
			
			MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
			mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
			mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
			mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
			mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
			mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
			CommandMap.setDefaultCommandMap(mc);
			msg.setContent(mp);
		} catch (MessagingException e) {
			e.printStackTrace();
			mLogger.error(e);
		} catch (Exception e) {
			e.printStackTrace();
			mLogger.error(e);
		}
		
		return this;
	}
	
	public boolean send() {
		if (msg == null) return false;
		try {
			Transport.send(msg);
		} catch (MessagingException e) {
			e.printStackTrace();
			mLogger.error(e);
			return false;
		}
		return true;
	}
	
	private boolean fileSizeCheck(String filename) {
		if (new File(filename).length() > (1024 * 1024 * 2.5)) {
			return false;
		}
		return true;
	}
}
