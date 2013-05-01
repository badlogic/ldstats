package com.badlogic;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.badlogic.ldstats.crawler.LdCrawler;
import com.badlogic.ldstats.crawler.LdEntry;

public class CrawlerTest {
//	@Test
//	public void textEntryParser() throws IOException {
//		LdCrawler crawler = new LdCrawler();
//		LdEntry entry = crawler.crawlEntry("http://www.ludumdare.com/compo/ludum-dare-26/?action=preview&uid=21045");
//		ObjectWriter writer = new ObjectMapper().defaultPrettyPrintingWriter();
//		System.out.println(writer.writeValueAsString(entry));
//	}
//	
//	@Test
//	public void testEntryUrlCrawler() throws IOException {
//		LdCrawler crawler = new LdCrawler();
//		List<String> entryUrls = crawler.crawlEntryUrls("http://www.ludumdare.com/compo/ludum-dare-26/");
//		System.out.println(entryUrls.size());
//	}
	
	@Test
	public void testCrawler() throws IOException {
		List<LdEntry> entries = new LdCrawler().crawlEntries("http://www.ludumdare.com/compo/ludum-dare-26/");
		System.out.println(entries.size());
	}
}
