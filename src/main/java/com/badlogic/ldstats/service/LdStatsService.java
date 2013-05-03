package com.badlogic.ldstats.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.badlogic.ldstats.crawler.LdCrawler;
import com.badlogic.ldstats.crawler.LdEntry;
import com.sun.jersey.spi.resource.Singleton;

@Path("/")
@Singleton
public class LdStatsService {
	private static final Logger log = Logger.getLogger(LdStatsService.class.getSimpleName());
	Map<String, List<LdEntry>> ludumDares = new HashMap<String, List<LdEntry>>();
	Index index;
	boolean isCrawling = false;
	String latestLd;
	LdStatsServiceConfig config;

	public LdStatsService() {
		config = new LdStatsServiceConfig();
		config.datasetUrl = "file:///Users/badlogic/workspace/ldstats/data/ludum-dares.zip";
		config.ldsToCrawl = new String[0];
//		config = readConfig();
		loadDataset(config);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				crawl();
			}
		};
//		new Timer().scheduleAtFixedRate(task, 0, 1000 * 3600 * 24);
	}
	
	public static class LdStatsServiceConfig {
		public String datasetUrl;
		public String[] ldsToCrawl;
	}
	
	private void loadDataset(LdStatsServiceConfig config) {
		if(config.datasetUrl == null) return;
		ZipInputStream in = null;
		log.info("loading dataset from " + config.datasetUrl);
		try {
			Map<String, List<LdEntry>> lds = new HashMap<String, List<LdEntry>>();
			in = new ZipInputStream(new URL(config.datasetUrl).openStream());
			ZipEntry entry = null;
			ObjectMapper mapper = new ObjectMapper();
			while((entry = in.getNextEntry()) != null) {
				if(entry.getName().startsWith("ld-") && entry.getName().endsWith(".json")) {
					String ldNum = entry.getName().replace("ld-", "").replace(".json", "");
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					IOUtils.copy(in, bytes);
					LdEntry[] entries = mapper.readValue(new String(bytes.toByteArray()), LdEntry[].class);
					lds.put(ldNum, Arrays.asList(entries));
					log.info("loaded dataset ld-" + ldNum + ".json");
				}
			}
			setLudumDares(lds);
			log.info("loaded dataset");
		} catch(Exception e) {
			e.printStackTrace();
			log.info("Couldn't load dataset");
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
	
	private LdStatsServiceConfig readConfig() {
		InputStream in = null;
		try {
			in = new URL("http://www.badlogicgames.com/ldstats/config.json").openStream();
			return new ObjectMapper().readValue(in, LdStatsServiceConfig.class);
		} catch(Exception e) {
			log.info("Couldn't read config");
			e.printStackTrace();
			LdStatsServiceConfig config = new LdStatsServiceConfig();
			config.datasetUrl = null;
			config.ldsToCrawl = new String[] { "26" };
			return config;
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("crawl")
	public boolean crawl() {
		synchronized(this) {
			if (isCrawling) return true;
			isCrawling = true;
		}
		new Thread(new Runnable() {
			public void run() {
				log.info("crawling LD");
				Map<String, List<LdEntry>> crawledLudumDares = new HashMap<String, List<LdEntry>>();
				for(String ld: config.ldsToCrawl) {
					for(int t = 0; t < 3; t++) {
						try {
							List<LdEntry> entries = new LdCrawler().crawlEntries("http://www.ludumdare.com/compo/ludum-dare-" + ld + "/");
							crawledLudumDares.put(ld, entries);
							break;
						} catch(IOException e) {
							e.printStackTrace();
							log.info("retrying to crawl " + "http://www.ludumdare.com/compo/ludum-dare-" + ld + "/");
						}
					}
					
				}
				setLudumDares(crawledLudumDares);
				log.info("crawling done");
			}
		}).start();
		return true;
	}
	
	public void setLudumDares(Map<String, List<LdEntry>> crawledLudumDares) {
		synchronized (this) {
			for(String ld: crawledLudumDares.keySet()) {
				ludumDares.put(ld, crawledLudumDares.get(ld));
			}
			latestLd = getLatestLd(ludumDares);
			index = new InMemoryIndex();
			index.index(latestLd, ludumDares);
			isCrawling = false;
		}
	}

	private String getLatestLd(Map<String, List<LdEntry>> crawledLudumDares) {
		int highest = 0;
		for(String key: crawledLudumDares.keySet()) {
			int num = Integer.parseInt(key);
			if(num > highest) highest = num;
		}
		return "" + highest;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("isCrawling")
	public boolean isCrawling() {
		return isCrawling;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("linkTypes")
	public List<String> linkTypes(@QueryParam("qld") String ldQuery, @QueryParam("q") String query) {
		Set<String> linkTypes = new HashSet<String>();
		String[] keywords = query != null ? query.split(",") : new String[0];
		List<LdEntry> entries = ludumDares.get(ldQuery);
		if(entries == null) {
			entries = ludumDares.get(latestLd);
		}
		synchronized (this) {
			if (query == null) {
				for (LdEntry entry : entries) {
					linkTypes.addAll(entry.links.keySet());
				}
			} else {
				for (LdEntry entry : entries) {
					for (String linkType : entry.links.keySet()) {
						if (InMemoryIndex.match(keywords, linkType)) {
							linkTypes.add(linkType);
						}
					}
				}
			}
		}
		List<String> sortedTypes = new ArrayList<String>();
		sortedTypes.addAll(linkTypes);
		Collections.sort(sortedTypes);
		return sortedTypes;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("query")
	public LdSearchResult query(@QueryParam("qlink") String linkQuery,
			@QueryParam("quser") String userQuery,
			@QueryParam("qtext") String textQuery,
			@QueryParam("qtype") String typeQuery,
			@QueryParam("qld") String ldQuery,  
			@QueryParam("qstart") int start,
			@QueryParam("qlength") int length,
			@QueryParam("qsort") String sortBy) {
		List<LdEntry> entries = index.query(textQuery, userQuery, linkQuery, typeQuery, ldQuery, sortBy);
		LdSearchResult result = new LdSearchResult();
		
		start = Math.max(0, start);

		List<LdEntry> page = new ArrayList<LdEntry>();
		for(int i = start; i < entries.size() && i < start + length; i++) {
			page.add(entries.get(i));
		}
		result.total = entries.size();
		result.offset = start;
		result.entries = page;
		return result;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("count")
	public int count(@QueryParam("qlink") String linkQuery,
			@QueryParam("quser") String userQuery,
			@QueryParam("qtext") String textQuery,
			@QueryParam("qtype") String typeQuery,
			@QueryParam("qld") String ldQuery, @QueryParam("qsort") String sortBy) {
		return index.query(textQuery, userQuery, linkQuery, typeQuery, ldQuery, sortBy).size();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("ludumDares")
	public List<String> getLudumDares() {
		synchronized(this) {
			List<String> lds = new ArrayList<String>();
			lds.addAll(ludumDares.keySet());
			Collections.sort(lds);
			Collections.reverse(lds);
			return lds;
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("raw")
	public List<LdEntry> raw(@QueryParam("qld") String ldQuery) {
		synchronized (this) {
			List<LdEntry> entries = ludumDares.get(ldQuery);
			if(entries == null) {
				entries = ludumDares.get(latestLd);
			}
			return entries;
		}
	}
}
