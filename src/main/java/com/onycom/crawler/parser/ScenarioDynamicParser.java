package com.onycom.crawler.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebElement;

import com.onycom.common.CrawlerLog;
import com.onycom.common.Util;
import com.onycom.crawler.data.Action;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.Scenario;
import com.onycom.crawler.data.Work;
import com.onycom.crawler.writer.DBWriter;

public class ScenarioDynamicParser extends DefaultParser {
	private Logger mLogger  = CrawlerLog.GetInstance(getClass());

	public ScenarioDynamicParser() {
	}

	@Override
	public List<Work> parse(Work[] history, Work work, Document document) {
		int saveCount;
		List<Work> ret = new ArrayList<Work>();
		if (document == null) {  return ret; }
		if (work.getAction() != null
				&& work.getAction().getType().equalsIgnoreCase(Action.TYPE_PARSE_CONTENTS)) {
			List<Contents> contents = this.parseContents(work, document);
			if (contents != null) {
				saveCount = this.saveContents(work, contents);
				work.result().setSaveCount(saveCount);
			}
		}

		if (work.getParseType() != Work.PARSE_NORMAL) {
			List<Work> list = parseURL(work, document);
			for (int i = list.size() - 1; 0 <= i; i--) {
				ret.add(list.get(i));
			}
		}
		return ret;
	}

	/**
	 * parseAction
	 */
	@Override
	public List<Work> parseURL(Work urlInfo, Document document) {
		List<Work> ret = new ArrayList<Work>();
		Scenario scen;
		Action action;
		String type, value, selector, empty_selector, condition;
		Work newUrlInfo;
		if (super.ifLeaf(urlInfo)) {
			return ret;
		}

		Elements els;
		Element el;

		/* action selector 의 결과값이 여러개 일때, 각각의 selector 를 찾아 워크로 만드는 작업 수행 */
		if (urlInfo.getParseType() == Work.PARSE_FIND_ACTION) { 
			selector = urlInfo.getAction().getSelector();
			empty_selector = urlInfo.getAction().getEmptySelector();
			els = document.select(selector);
			action = urlInfo.getAction();
			for (int i = 0; i < els.size(); i++) {
				el = els.get(i);
				newUrlInfo = new Work(urlInfo.getURL(), mConfig.CHARACTER_SET);
				newUrlInfo.setDepth(urlInfo.getDepth());
				newUrlInfo.setHighPriority(true);
				newUrlInfo.setAction(
						new Action(action.getType(), 
								   action.getTargetDepth(), 
								   Util.GetCssSelector(el), 
								   empty_selector, 
								   action.getValue(),
								   action.getWaitTime(),
								   action.getCondition(),
								   action.getContentsDepth()));
				newUrlInfo.setDepth(urlInfo.getDepth());
				newUrlInfo.getAction().setNo(action.getNo());
				ret.add(newUrlInfo);
			}
		} 
		/* root 거나 target_depth가 발견되면 해당 depth의 시나리오 패턴을 워크로 만드는 작업 수행 */
		else if (urlInfo.getParseType() == Work.PARSE_SCENARIO) {
			Map<Integer, Scenario> scenarios = getConfig().getScenarios();
			int curDepth = 0;
			if(urlInfo.getAction() != null) {
				curDepth = urlInfo.getDepth();
			}
			if (scenarios != null) {
				int len = scenarios.size();
				int target_depth;
				if (len != 0 && len >= curDepth) {
					scen = scenarios.get(curDepth);
					if (scen == null)
						return ret;
					len = scen.getSize();
					for (int i = 0; i < len; i++) {
						action = scen.getAction(i);
						type = action.getType(); /* 액션타입 */
						value = action.getValue(); /* 액션값 */
						target_depth = action.getTargetDepth(); /* 시나리오변경 */
						selector = action.getSelector(); /* selector */
						empty_selector = action.getEmptySelector();
						condition = action.getCondition();
						newUrlInfo = new Work(urlInfo.getURL(), mConfig.CHARACTER_SET).setDepth(target_depth);
						newUrlInfo.setHighPriority(true);
						newUrlInfo.setAction(new Action(type,
														target_depth, 
														selector,
														empty_selector,
														value,
														action.getWaitTime(),
														condition,
														action.getContentsDepth()));
						newUrlInfo.setDepth(curDepth);
						newUrlInfo.getAction().setNo(i);
						ret.add(newUrlInfo);
					}
				}
			}
		}

		return ret;
	}
}
