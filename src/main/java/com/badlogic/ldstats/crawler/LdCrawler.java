package com.badlogic.ldstats.crawler;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LdCrawler {
	private static final Logger log = Logger.getLogger(LdCrawler.class.getSimpleName());
	private static final int RETRIES = 3;

	public List<LdEntry> crawlEntries(String url) throws IOException {
		List<String> urls = crawlEntryUrls(url);
		final List<LdEntry> entries = new ArrayList<LdEntry>();
		ExecutorService executor = Executors.newFixedThreadPool(8);
		for(final String entryUrl: urls) {
			executor.execute(new Runnable() {
				public void run() {
					for(int i = 0; i < RETRIES; i++) {
						try {
							LdEntry entry = crawlEntry(entryUrl);
							synchronized(entries) {
								entries.add(entry);
								if(entries.size() % 100 == 0) {
									log.info("crawled " + entries.size() + " entries");
								}
							}
							return;
						} catch (IOException e) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e1) {
							}
						}
					}
				}
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		log.info("crawled " + entries.size() + " entries");
		return entries;
	}
	
	public List<String> crawlEntryUrls(String seedUrl) throws IOException {
		if(!seedUrl.endsWith("/")) seedUrl += "/";
		List<String> entryUrls = new ArrayList<String>();
		log.info("crawling entry urls for " + seedUrl);
		for (int offset = 0; offset < 5000; offset += 24) {
			List<String> entries = crawlPreviewPage(seedUrl, offset);
			entryUrls.addAll(entries);
			log.info("crawled " + entryUrls.size() + " entry urls");
			if (entries.size() == 0) break;
		}
		return entryUrls;
	}

	private static List<String> crawlPreviewPage(String seedUrl, int offset) throws IOException {
		String url = seedUrl + "?action=preview&q=&etype&start=" + offset;
		Document doc = null; 
		for(int i = 0; i < RETRIES; i++) {
			try {
				doc = Jsoup.connect(url).get();
				break;
			} catch (IOException e) {
				e.printStackTrace();
				if(i == RETRIES - 1) throw e;
				log.info("retrying downloading preview page " + url);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}
			}
		}
		Elements preview = doc.select("table.preview");
		Elements links = preview.select("a[href]");

		List<String> entryUrls = new ArrayList<String>();
		for (int i = 0; i < links.size(); i++) {
			entryUrls.add(seedUrl + "/" + links.get(i).attr("href"));
		}
		return entryUrls;
	}

	public LdEntry crawlEntry(String url) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy @ h:mmaa", Locale.ENGLISH);
		Document doc = Jsoup.connect(url).get();

		LdEntry entry = new LdEntry();
		// extract uid
		entry.uid = url.split("uid=")[1];
		
		// extract content div (id=compo2)
		Elements contentDiv = doc.select("div#compo2");
		
		// get user & title
		Elements userTitle = contentDiv.select("h3");
		entry.title = userTitle.text().split(" - ")[0];
		entry.user = userTitle.text().split(" - ")[1];
		entry.type = "Jam Entry".equals(userTitle.select("i").text())? "open": "compo";
				
		// extract links
		Elements links = doc.select("p.links").select("a");
		for(int i = 0; i < links.size(); i++) {
			Element link = links.get(i);
			String linkUrl = link.attr("href");
			String text = link.text();
			entry.links.put(text, linkUrl);
		}
		
		// extract text, pre ld26, the content div was the second one, now its the 3rd one.
		if(contentDiv.select("p").get(3).select("a").size() != 0) {
			entry.text = contentDiv.select("p").get(2).html();
		} else {
			entry.text = contentDiv.select("p").get(3).html();
		}
		
		// extract screenshot urls
		Elements screenshots = contentDiv.select("table").select("a");
		for(int i = 0; i < screenshots.size(); i++) {
			entry.screenshotUrls.add(screenshots.get(i).attr("href"));
		}
		
		// extract ratings
		if(contentDiv.html().contains("<h3>Ratings</h3>")) {
			Elements ratings = contentDiv.select("table").get(1).select("tr");
			for(int i = 0; i < ratings.size(); i++) {
				Element rating = ratings.get(i);
				String ratingName = rating.child(1).text().replace("(Jam)", "");
				String ratingValue = rating.child(2).text();
				if(ratingValue.contains("%")) {
					ratingValue = ratingValue.replace("%", "");
					entry.ratings.put(ratingName, Float.parseFloat(ratingValue) / 100.0f);
				} else {
					entry.ratings.put(ratingName, Float.parseFloat(ratingValue));
				}
			}
		}
		
		// extract comments
		Elements comments = doc.select("div.comment");
		for(int i = 0; i < comments.size(); i++) {
			Element c = comments.get(i);
			LdComment comment = new LdComment(); 
			comment.user = c.select("strong").select("a").text();
			try {
				comment.date = dateFormat.parse(c.select("small").text());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			comment.text = c.select("p").text();
			entry.comments.add(comment);
		}
		
		return entry;
	}
	
	public static void main(String[] args) {
		new File("data").mkdirs();
		ObjectMapper mapper = new ObjectMapper();
		for(int i = 17; i <= 26; i++) {
			for(int t = 0; t < 3; t++) {
				try {
					List<LdEntry> entries = new LdCrawler().crawlEntries("http://www.ludumdare.com/compo/ludum-dare-" + i + "/");
					mapper.writeValue(new File("data/ld-" + i + ".json"), entries);
					break;
				} catch(IOException e) {
					e.printStackTrace();
					log.info("retrying to crawl " + "http://www.ludumdare.com/compo/ludum-dare-" + i + "/");
				}
			}
		}
	}
}
