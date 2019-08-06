package com.hdone.crawler.parser;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.plaf.synth.SynthSeparatorUI;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hdone.common.Util;
import com.hdone.crawler.data.Action;
import com.hdone.crawler.data.Scenario;
import com.hdone.crawler.data.Work;

public class ScenarioStasticParser extends StaticParser{

	@Override
	public List<Work> parseURL(Work work, Document document) {
		List<Work> ret = new ArrayList<Work>();
		Elements els;
		Scenario scen;
		Action action;
		String href, url, domain_url, sub_url;
		URL new_url;
		Work newWork;
		boolean allow = false;
		
		if(super.ifLeaf(work)){
			return ret;
		}
		
		int curDepth = work.getDepth();
		Map<Integer, Scenario> scenarios = getConfig().getScenarios();
		if(scenarios != null){
			int targetDepth;
			int len = scenarios.size();
			if(len != 0 && len >= curDepth){
				scen = scenarios.get(curDepth);
				if(scen == null) return ret;
				len = scen.getSize();
				for(int i = 0 ; i < len ; i ++){
					action = scen.getAction(i);
					targetDepth = (action == null)? -1 : action.getTargetDepth(); 
					els = document.select(action.getSelector());
					els = els.select("a[href]");
					if(els.size() > 0){
						for(Element e : els){
							href = e.attr("href").trim();
							if(href.length() == 0) continue;
							//tmp = Util.SplitDomainAndSubURL(work, href);
							new_url = Util.GetURL(work.toString(), href);
							if(new_url != null){
								domain_url = Util.GetDomain(new_url);
								sub_url = new_url.getPath() + ((new_url.getQuery()!=null)? "?" + new_url.getQuery() : "");
								url = domain_url + sub_url;
								
								if(getConfig().getFilterAllow() != null && getConfig().getFilterAllow().size() > 0 &&
										getConfig().getFilterDisallow() != null  && getConfig().getFilterDisallow().size() > 0){
									allow = super.isAllow(work, domain_url, sub_url);
									if(allow) {
										url = super.filterModify(url);
										newWork = new Work(url, mConfig.CHARACTER_SET).setDepth(targetDepth);
										newWork.setHighPriority(true);
										ret.add(newWork);
									}else {
										newWork = null;
									}
								}else{
									url = super.filterModify(url);
									newWork = new Work(url, mConfig.CHARACTER_SET).setDepth(targetDepth);
									newWork.setHighPriority(true);
									ret.add(newWork);
								}
								
								if(newWork != null && action.getContentsDepth() > -1){
									newWork.setAction(new Action(action.getContentsDepth()));
								}
							}else{
								work.result().addError(Work.Error.ERR_URL, "URL parse err : " + href, null);
							}
						}
					}else{
						if(action.getEmptySelector() != null && document.select(action.getEmptySelector()).size() > 0) {
							
						}else {
							work.result().addError(Work.Error.ERR_SCEN_ELEMENT, action.getSelector(), null);
						}
					}
				}
			}
		}
		
		return ret;
	}

	@Override
	public List<Work> checkDupliate(Work[] aryHistory, List<Work> aryNewUrl) {
		return aryNewUrl;
	}
	
}