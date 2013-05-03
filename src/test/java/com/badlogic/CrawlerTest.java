package com.badlogic;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.junit.Test;

import com.badlogic.ldstats.crawler.LdCrawler;
import com.badlogic.ldstats.crawler.LdEntry;

public class CrawlerTest {
	@Test
	public void textEntryParser() throws IOException {
		LdCrawler crawler = new LdCrawler();
//		LdEntry entry = crawler.crawlEntry("http://www.ludumdare.com/compo/ludum-dare-26/?action=preview&uid=22590");
//		LdEntry entry = crawler.crawlEntry("http://www.ludumdare.com/compo/ludum-dare-25/?action=preview&uid=19197");
//		LdEntry entry = crawler.crawlEntry("http://www.ludumdare.com/compo/ludum-dare-25/?action=preview&uid=19384");
		LdEntry entry = crawler.crawlEntry("http://www.ludumdare.com/compo/ludum-dare-24/?action=preview&uid=238");
		ObjectWriter writer = new ObjectMapper().defaultPrettyPrintingWriter();
		System.out.println(writer.writeValueAsString(entry));
	}
//	
//	@Test
//	public void testEntryUrlCrawler() throws IOException {
//		LdCrawler crawler = new LdCrawler();
//		List<String> entryUrls = crawler.crawlEntryUrls("http://www.ludumdare.com/compo/ludum-dare-26/");
//		System.out.println(entryUrls.size());
//	}
	
//	@Test
//	public void testCrawler() throws IOException {
//		List<LdEntry> entries = new LdCrawler().crawlEntries("http://www.ludumdare.com/compo/ludum-dare-26/");
//		System.out.println(entries.size());
//	}
}
