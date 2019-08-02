package com.onycom.crawler.parser;

import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.Work;
import org.jsoup.nodes.Document;

import java.util.List;

public interface Parser <Work, Contents>{
    public void setConfig(Config config);
    public List<Work> parse(Work[] history, Work work, Document document);
}
