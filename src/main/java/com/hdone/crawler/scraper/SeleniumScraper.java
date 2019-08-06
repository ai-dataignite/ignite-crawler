package com.hdone.crawler.scraper;

import com.hdone.SettingBasedCrawler.Crawler;
import com.hdone.common.CrawlerLog;
import com.hdone.crawler.data.Action;
import com.hdone.crawler.data.Config;
import com.hdone.crawler.data.Cookie;
import com.hdone.crawler.data.Work;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.W3CHttpCommandCodec;
import org.openqa.selenium.remote.http.W3CHttpResponseCodec;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 싱글톤으로 운영
 */
public class SeleniumScraper implements Scraper {
	// static SeleniumScraper mInstance;
	private Logger mLogger  = CrawlerLog.GetInstance(getClass());
	static WebDriver mSeleniumDriver;
	static Map<String, String> mJSData = new HashMap<String, String>();
	static Config mConfig;

	static final String JAVASCRIPT_REMOVE_ELEMENTS = "var selector = \"%s\"; var els = document.querySelectorAll(selector); for(var i = 0 ; i < els.length ;i++){ els[i].remove()};";

	static final String JAVASCRIPT_HOOKING_AJAX = 
			"if(typeof(sbc_loading_ajax) == 'undefined') {"
			+ " sbc_loading_ajax = {}; " 
			+ " oldXHROpen = window.XMLHttpRequest.prototype.open; " 
			+ " window.XMLHttpRequest.prototype.open = function(method, url, async, user, password) { " 
			+ " this.addEventListener('loadstart', function() { " 
			+ "  sbc_loading_ajax[url] = true; " 
			+ " }); " 
			+ " this.addEventListener('progress', function(){" 
			+ "  sbc_loading_ajax[url] = true; "
			+ " });"
			+ " this.addEventListener('loadend', function() { " 
			+ "  delete sbc_loading_ajax[url]; "
			+ " }); " 
			+ "  return oldXHROpen.apply(this, arguments); " 
			+ " };"
			+ "}";
	static final String JAVASCRIPT_IS_LOADING_AJAX = "return Object.keys(sbc_loading_ajax).length";
	
	final int DEFAULT_AJAX_LOAD_TIMEOUT_MS = 3000;
	final int DEFAULT_PAGE_LOAD_TIMEOUT_SEC = 30;
	
	public SeleniumScraper(Config config) {
		mConfig = config;
	}

	public boolean open() {
		return connectSelenium(mConfig);
	}

	public void close() {
		quitSelenium();
	}

	public Document getDocument(Work work) throws Exception {
		Document ret = null;
		// List<String> checkSelector;
		WebElement we = null;
		List<WebElement> wes = null;
		// String newWindow;
		Action action = work.getAction();
		// urlInfo.setParentWindow(mSeleniumDriver.getWindowHandle());
		if (action != null) {
			String selector = action.getSelector();
			String empty_selector = action.getEmptySelector();
			String value = action.getValue();
			if (selector != null) {
				wes = waitingForAllElements(mSeleniumDriver, action.getWaitTime(), selector, empty_selector);
				if (wes == null) { // 못찾음
					// mLogger.error("Not found element : " + selector);
					work.result().addError(Work.Error.ERR_ACTION, selector, null);
					work.setURL(mSeleniumDriver.getCurrentUrl());
					return null;
				}
				if (wes.isEmpty()) { // 이제 없음
					// mLogger.info("empty element : " + selector);
					if (action.getType().contentEquals(Action.TYPE_PARSE_CONTENTS)) {
						work.setURL(mSeleniumDriver.getCurrentUrl());
						ret = Jsoup.parse(mSeleniumDriver.getPageSource());
						return ret;
					} else {
						work.setURL(mSeleniumDriver.getCurrentUrl());
						return null;
					}
				}
				if (wes.size() == 1 || action.getType().equalsIgnoreCase(Action.TYPE_PARSE_CONTENTS)
						|| action.getType().equalsIgnoreCase(Action.TYPE_REMOVE_ELEMENTS)) {
					we = wes.get(0);

					if (action.getCondition() != null) {
						JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
						Object js_ret = jse.executeScript(action.getCondition(),
								action.getSelector()); /* 스크립트 오류 익셉션 체크해야함 */
						if (js_ret != null) {
							try {
								if (js_ret.getClass().getName().contentEquals("java.lang.Boolean")) {
									Boolean bool = (Boolean) js_ret;
									if (!bool) {
										mLogger.info("pass : " + work.getAction().getType() + "," + work.toString());
										return null;
									}
								}
							} catch (JSONException e) {
								mLogger.error(e.getMessage(), e.fillInStackTrace());
								e.printStackTrace();
							}

						}
					}

					if (action.getType().equalsIgnoreCase(Action.TYPE_CLICK)) {
						
						// 셀레니움 액션 이벤트로 페이지 로딩이 발생할 경우 로딩이 완료 될때까지 블록에 걸림
						int timeout_sec = DEFAULT_PAGE_LOAD_TIMEOUT_SEC;
						int try_reload = action.getTryRefresh();
						boolean isLoaded= false;
						
						try {
							mSeleniumDriver.manage().timeouts().pageLoadTimeout(timeout_sec, TimeUnit.SECONDS);
							we.click();
							isLoaded = true;
						}catch(org.openqa.selenium.TimeoutException e) {
						}
						if (!isLoaded) {
							for(int i = 0 ; i < try_reload ; i++) {
								System.err.println("try refresh ..." + (i+1));
								timeout_sec *= 2;
								
								SessionId session_id = ((RemoteWebDriver)mSeleniumDriver).getSessionId();
								HttpCommandExecutor executor = (HttpCommandExecutor) ((RemoteWebDriver)mSeleniumDriver).getCommandExecutor();
								mSeleniumDriver = createDriverFromSession(session_id, executor.getAddressOfRemoteServer());
								mSeleniumDriver.manage().timeouts().pageLoadTimeout(timeout_sec, TimeUnit.SECONDS);
								try {
									System.err.println("exc refresh");
									mSeleniumDriver.navigate().refresh();
									isLoaded = true;
									break;
								}catch(org.openqa.selenium.TimeoutException e2) {
									isLoaded = false;
								}
							}
						}
						if(!isLoaded) {
							throw new org.openqa.selenium.TimeoutException();
						}
						if(timeout_sec != DEFAULT_PAGE_LOAD_TIMEOUT_SEC)
							mSeleniumDriver.manage().timeouts().pageLoadTimeout(DEFAULT_PAGE_LOAD_TIMEOUT_SEC, TimeUnit.SECONDS);
					} else if (action.getType().equalsIgnoreCase(Action.TYPE_NEW_WINDOW_CLICK)) {
						Actions actions = new Actions(mSeleniumDriver);
						actions.keyDown(Keys.LEFT_CONTROL).click(we).keyUp(Keys.LEFT_CONTROL).build().perform();
					} else if (action.getType().equalsIgnoreCase(Action.TYPE_CLEAR)) {
						we.clear();
					} else if (action.getType().equalsIgnoreCase(Action.TYPE_INPUT) && value != null) {
						we.sendKeys(value);
					} else if (action.getType().equalsIgnoreCase(Action.TYPE_VERTICAL_SCROLL_BY) && value != null) {
						JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
						jse.executeScript("window.scrollBy(0," + value + ")", "");
					} else if (action.getType().equalsIgnoreCase(Action.TYPE_VERTICAL_SCROLL_TO) && value != null) {
						JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
						jse.executeScript("window.scrollTo(0," + value + ")", "");
					} else if (action.getType().equalsIgnoreCase(Action.TYPE_SELECT) && value != null) {
						Select dropdown = new Select(we);
						try {
							int intValue = Integer.parseInt(value);
							dropdown.selectByIndex(intValue);
						} catch (NumberFormatException e) { // exception 이라면
							// String 이므로 String
							// 처리
							dropdown.selectByValue(value);
						}
					} else if (action.getType().equalsIgnoreCase(Action.TYPE_JAVASCRIPT) && value != null) {
						JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
//                        Iterator<String> it = mJSData.keySet().iterator();
//                        String k, v;
//                        while (it.hasNext()) {
//                            k = it.next();
//                            v = mJSData.get(k);
//                            value = "var " + k + "=" + v + "; " + value;
//                        }
						Object js_ret = jse.executeScript(value, action.getSelector()); /* 스크립트 오류 익셉션 체크해야함 */
						if (js_ret != null) {
							try {
								if (js_ret.getClass().getTypeName().contentEquals("Boolean")) {
									Boolean bool = (Boolean) js_ret;
									if (!bool) {

									}
								}
//                                System.err.println("script return " + String.valueOf(js_ret));
//                                JSONObject json = new JSONObject(String.valueOf(js_ret));
//                                for (Object key : json.keySet().toArray()) {
//                                    k = String.valueOf(key);
//                                    mJSData.put(k, String.valueOf(json.get(k)));
//                                }
							} catch (JSONException e) {
								throw e;
								// Crawler.Log('e', e.getMessage(),
								// e.fillInStackTrace());
								// e.printStackTrace();
							}
						}
					} else if (action.getType().equalsIgnoreCase(Action.TYPE_REMOVE_ELEMENTS) && value != null) {
						JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
						String script = String.format(JAVASCRIPT_REMOVE_ELEMENTS, value);
						Object js_ret = jse.executeScript(script, ""); /* 스크립트 오류 익셉션 체크해야함 */
					}

					if (action.getTargetDepth() != -1) {
						work.setParseType(Work.PARSE_SCENARIO);
						work.setDepth(action.getTargetDepth());
					}
					
					work.setURL(mSeleniumDriver.getCurrentUrl());
					ret = Jsoup.parse(mSeleniumDriver.getPageSource());
					// mSeleniumDriver.
				} else {
					/*
					 * 액션 찾는 로직으로 전달
					 */
					work.setParseType(Work.PARSE_FIND_ACTION);
					work.setURL(mSeleniumDriver.getCurrentUrl());
					ret = Jsoup.parse(mSeleniumDriver.getPageSource());
				}
			} else {
				if (action.getType().equalsIgnoreCase(Action.TYPE_SWITCH_WINDOW)) {
					ArrayList<String> tab = new ArrayList<String>(mSeleniumDriver.getWindowHandles());
					int cur_idx = 0;
					int tab_size = tab.size();
					// 값이 없으면 무조건 마지막 윈도우로
					if (value == null) {
						mSeleniumDriver.switchTo().window(tab.get(tab_size - 1));
					} else { // 현재를 기준으로 이동하고자 할때
						int new_idx = Integer.parseInt(value);
						if (value.indexOf('+') != -1 || value.indexOf('-') != -1) {
							for (int i = 0; i < tab_size; i++) {
								if (tab.get(i).contentEquals(mSeleniumDriver.getWindowHandle())) {
									cur_idx = i;
								}
							}
							new_idx = (cur_idx + new_idx);
							if (new_idx < 0)
								new_idx = 0;
						}
						if (tab_size > new_idx) {
							mSeleniumDriver.switchTo().window(tab.get(new_idx));
						}
					}
				} else if (action.getType().equalsIgnoreCase(Action.TYPE_CLOSE_WINDOW)) {
					ArrayList<String> tab = new ArrayList<String>(mSeleniumDriver.getWindowHandles());
					int tab_size = tab.size();
					int cur_idx = 0;
					for (int i = 0; i < tab_size; i++) {
						if (tab.get(i).contentEquals(mSeleniumDriver.getWindowHandle())) {
							cur_idx = i;
						}
					}
					if (value == null) {
						mSeleniumDriver.close();
					} else { // 현재를 기준으로 이동하고자 할때
						int new_idx = Integer.parseInt(value);
						if (tab_size > new_idx) {
							mSeleniumDriver.switchTo().window(tab.get(new_idx));
							mSeleniumDriver.close();
						}
					}
					tab_size = mSeleniumDriver.getWindowHandles().size();
					if (tab_size > cur_idx) {
						mSeleniumDriver.switchTo().window(tab.get(cur_idx));
					} else {
						mSeleniumDriver.switchTo().window(tab.get(tab_size - 1));
					}
				} else if (action.getType().equalsIgnoreCase(Action.TYPE_BACKWORD_WINDOW)) {
					mSeleniumDriver.navigate().back();
				} else if (action.getType().equalsIgnoreCase(Action.TYPE_FORWORD_WINDOW)) {
					mSeleniumDriver.navigate().forward();
				} else if (action.getType().equalsIgnoreCase(Action.TYPE_REFRESH_WINDOW)) {
					mSeleniumDriver.navigate().refresh();
				} else if (action.getType().equalsIgnoreCase(Action.TYPE_CLOSE_POPUP)) {
					if (value != null) {
						String cur_handle = mSeleniumDriver.getWindowHandle();
						Integer target_idx = Integer.parseInt(value);
						ArrayList<String> tab = new ArrayList<String>(mSeleniumDriver.getWindowHandles());
						int tab_size = tab.size();
						if (tab_size > target_idx) {
							mSeleniumDriver.switchTo().window(tab.get(target_idx));
							mSeleniumDriver.close();
							mSeleniumDriver.switchTo().window(cur_handle);
						}
					}
				} else if (action.getType().equalsIgnoreCase(Action.TYPE_SLEEP)) {
					if (value != null) {
						long v = (long) (Float.valueOf(value) * 1000);
						Thread.sleep(v);
					}
				} else if (action.getType().equalsIgnoreCase(Action.TYPE_START_MONITOR_AJAX)) {
					JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
					jse.executeScript(JAVASCRIPT_HOOKING_AJAX, ""); 
				} else if (action.getType().equalsIgnoreCase(Action.TYPE_WAIT_AJAX)) {
					JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
					int wait_time = 0;
					while(true) {
						if((Long)jse.executeScript(JAVASCRIPT_IS_LOADING_AJAX, "") > 0) {
							Thread.sleep(500);
							wait_time += 500;
						}else {
							break;
						}
						if (wait_time > DEFAULT_AJAX_LOAD_TIMEOUT_MS) {
							work.result().addError(Work.Error.ERR_AJAX, "depth " + work.getDepth(), null);
							throw new Exception();
						}
					}
				}

				if (action.getTargetDepth() != -1) {
					work.setParseType(Work.PARSE_SCENARIO);
					ret = Jsoup.parse(mSeleniumDriver.getPageSource());
				} else {
					ret = null;
				}
				work.setURL(mSeleniumDriver.getCurrentUrl());
			}
		} else { // 액션 없는 seed 일 경우
			// URL 호출
			mSeleniumDriver.get(work.toString());
			// window 정보 저장하고
			work.setParentWindow(mSeleniumDriver.getWindowHandle());
			work.setParseType(Work.PARSE_SCENARIO);
			ret = Jsoup.parse(mSeleniumDriver.getPageSource());
		}
		if (work.getAction() != null) {
			mLogger.info("depth " + work.getDepth() 
								  + "/" + work.getAction().getNo()
								  + ", " + work.getAction().getType() 
								  + ", " + work.toString());
		} else {
			mLogger.info("url," + work.toString());
		}

		return ret;
	}

//    public static void closeDocument(Work urlInfo) {
//        String window = urlInfo.getParentWindow();
//        String curWindow = mSeleniumDriver.getWindowHandle();
//        if (window == null) {
//            mSeleniumDriver.close();
//        } else if (!window.contentEquals(curWindow)) {
//            mSeleniumDriver.close();
//            mSeleniumDriver.switchTo().window(window);
//        } else {
//            mSeleniumDriver.navigate().back();
//        }
//    }
//
//    public static List<WebElement> GetEelements(String selector) {
//        try {
//            return waitingForAllElements(mSeleniumDriver, selector);
//        } catch (TimeoutException e) {
//            return null;
//        }
//    }
//
//    private static WebElement waitingForElement(WebDriver wd, String selector)
//            throws org.openqa.selenium.TimeoutException {
//        return new WebDriverWait(wd, 10).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
//    }

//    private boolean waitingForLoadPage(WebDriver wd){
//        WebDriverWait wdw = new WebDriverWait(wd, 10);
//        
//       // System.err.println("wait page : " + ((JavascriptExecutor) wd).executeScript("return document.readyState").toString());
//        boolean ret = wdw.until(d -> (((JavascriptExecutor) d).executeScript("return document.readyState").toString().contentEquals("complete")));
//       // System.err.println("loaded page : " + ret);
//        return ret;
//    }

	private List<WebElement> waitingForAllElements(WebDriver wd, int wait_sec, String selector,
			String empty_selector) {
		List<WebElement> ret = null;
		WebElement check_sub_load;
		long ms = wait_sec * 1000;
		long startTime = System.currentTimeMillis();

		while (true) {
			try {
//				By.xpath(xpathExpression);
//				By.
				ret = wd.findElements(By.cssSelector(selector));
				if (ret != null && !ret.isEmpty()) {
					break;
				}
				if (empty_selector != null) {
					try {
						check_sub_load = wd.findElement(By.cssSelector(empty_selector));
					} catch (NoSuchElementException e) {
						check_sub_load = null;
					}
					if (check_sub_load != null) {
						ret = new ArrayList<WebElement>();
						break;
					}
				}
			} catch (WebDriverException e) {
				mLogger.error("WebDriverException " + e.getMessage());
				return null;
			}
			if ((System.currentTimeMillis() - startTime) > ms) {
				return null;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return ret;
	}
	
	// 세션 복사해서 새로운 driver 만드는 코드
	// window 는 첫 window 로 지정되니 윈도우를 변경할 필요가 있을때는 해당코드 추가 필요
	public static RemoteWebDriver createDriverFromSession(final SessionId sessionId, URL command_executor) {
		CommandExecutor executor = new HttpCommandExecutor(command_executor) {

			@Override
			public Response execute(Command command) throws IOException {
				Response response = null;
				if (command.getName() == "newSession") {
					response = new Response();
					response.setSessionId(sessionId.toString());
					response.setStatus(0);
					response.setValue(Collections.<String, String>emptyMap());
					try {
						Field commandCodec = null;
						commandCodec = this.getClass().getSuperclass().getDeclaredField("commandCodec");
						commandCodec.setAccessible(true);
						commandCodec.set(this, new W3CHttpCommandCodec());

						Field responseCodec = null;
						responseCodec = this.getClass().getSuperclass().getDeclaredField("responseCodec");
						responseCodec.setAccessible(true);
						responseCodec.set(this, new W3CHttpResponseCodec());
					} catch (NoSuchFieldException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}

				} else {
					response = super.execute(command);
				}
				return response;
			}
		};

		return new RemoteWebDriver(executor, new DesiredCapabilities());
	}

//    private static List<WebElement> waitingForAllElements(WebDriver wd, String selector)
//            throws org.openqa.selenium.TimeoutException {
//        return new WebDriverWait(wd, 10)
//                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(selector)));
//    }

	private boolean connectSelenium(Config config) {
		String os = System.getProperty("os.name").toLowerCase();
		String base_path = Crawler.BASE_PATH;
		String driver_path;
		if (mSeleniumDriver == null) {
			if (config.SELENIUM_DRIVER_PATH.isEmpty()) {
				if (config.SELENIUM_DRIVER_NAME.equalsIgnoreCase("chrome")) {
					if (os.indexOf("win") >= 0) {
						driver_path = base_path + "/web_driver/chromedriver.exe";
					} else {
						driver_path = base_path + "/web_driver/chromedriver";
					}
				} else {
					if (os.indexOf("win") >= 0) {
						driver_path = base_path + "/web_driver/phantomjs.exe";
					} else {
						driver_path = base_path + "/web_driver/phantomjs";
					}
				}
			} else {
				if (config.SELENIUM_DRIVER_PATH.startsWith(".")) {
					driver_path = base_path + config.SELENIUM_DRIVER_PATH;
				} else {
					driver_path = config.SELENIUM_DRIVER_PATH;
				}
			}
			System.out.println("Load selenium driver : " + driver_path);
			try {
				if (config.SELENIUM_DRIVER_NAME.equalsIgnoreCase("chrome")) {
					System.setProperty("webdriver.chrome.driver", driver_path);
					ChromeOptions options = new ChromeOptions();
					options.addArguments("--disable-application-cache");
					options.addArguments("--disable-notifications");
					options.addArguments("--no-sandbox");
					options.setCapability("browserstack.use_w3c", true);
					// options.addArguments("window-size=1920x1080");
					if (config.SELENIUM_HEADLESS) {
						options.addArguments("headless");
					}
					mSeleniumDriver = new ChromeDriver(options);
				} else {
					DesiredCapabilities DesireCaps = new DesiredCapabilities();
					DesireCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, driver_path);
					DesireCaps.setJavascriptEnabled(true);
					// DesireCaps.setCapability("takesScreenshot", true);
					mSeleniumDriver = new PhantomJSDriver(DesireCaps);
				}
				mSeleniumDriver.manage().window().setSize(new Dimension(1920, 1080));
			} catch (IllegalStateException e) {
				mLogger.error("Not found SeleniumDriver");
				return false;
			}
		}
		return true;
	}

	private static void quitSelenium() {
		if (mSeleniumDriver != null) {
			try {
				mSeleniumDriver.quit();
			} catch (WebDriverException e) {
				e.printStackTrace();
			}
			mSeleniumDriver = null;
		}
	}

	public void clear() {
		if (mSeleniumDriver != null) {
			try {
				ArrayList<String> tab = new ArrayList<String>(mSeleniumDriver.getWindowHandles());
//	            if(mSeleniumDriver.getClass() == PhantomJSDriver.class) {
//	            	((PhantomJSDriver) mSeleniumDriver).executeScript("window.open()", "");
//	            }else if(mSeleniumDriver.getClass() == ChromeDriver.class) {
//	            	JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
//	                jse.executeScript("window.open()", "");
//	            }
				// JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
				// jse.executeScript("window.open()", "");
				for (int i = 0; i < tab.size(); i++) {
					mSeleniumDriver.switchTo().window(tab.get(i));
					if ((tab.size() - 1) > i)
						mSeleniumDriver.close();
				}
			} catch (org.openqa.selenium.WebDriverException e) {
				e.printStackTrace();
				mLogger.error("Not reachable WebDriver");
			}

		}
	}
}