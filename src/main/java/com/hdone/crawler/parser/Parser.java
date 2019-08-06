package com.hdone.crawler.parser;

import org.jsoup.nodes.Document;

import com.hdone.crawler.data.Config;
import com.hdone.crawler.data.Contents;
import com.hdone.crawler.data.Work;

import java.util.List;

public interface Parser <Work, Contents>{
    public void setConfig(Config config);
    public List<Work> parse(Work[] history, Work work, Document document);
}
