package com.badlogic.ldstats.service;

import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.badlogic.ldstats.crawler.LdEntry;

public class InMemoryIndex implements Index {
	private String latestLd;
	private Map<String, List<LdEntry>> ludumDares;
	
	public void index(String latestLd, Map<String, List<LdEntry>> ludumDares) {
		this.latestLd = latestLd;
		this.ludumDares = ludumDares;
	}

	public List<LdEntry> query(String titleTextQuery, String userQuery, String linkQuery, String typeQuery, String ldQuery, String sortBy) {
		String[] linkKeywords = linkQuery != null && linkQuery.length() != 0? linkQuery.split(","): new String[0];
		String[] userKeywords = userQuery != null && userQuery.length() != 0? userQuery.split(","): new String[0];
		String[] textKeywords = titleTextQuery != null && titleTextQuery.length() != 0? titleTextQuery.split(","): new String[0];
		String[] typeKeywords = typeQuery != null && typeQuery.length() != 0? new String[] { typeQuery }: new String[0];
		List<LdEntry> entries = ludumDares.get(ldQuery);
		if(entries == null) {
			entries = ludumDares.get(latestLd);
		}
		List<LdEntry> results = new ArrayList<LdEntry>();
		for(LdEntry entry: entries) {
			if(match(linkKeywords, entry.links.keySet()) &&
			   match(userKeywords, entry.user) &&
			   (match(textKeywords, entry.text) || match(textKeywords, entry.title)) &&
			   match(typeKeywords, entry.type)){
				   results.add(entry);
			   }
		}
		
		if("comment".equals(sortBy)) {
			Collections.sort(results, new Comparator<LdEntry>() {
				public int compare(LdEntry o1, LdEntry o2) {
					return o2.comments.size() - o1.comments.size();
				}
			});
		}
		
		if("title".equals(sortBy)) {
			Collections.sort(results, new Comparator<LdEntry>() {
				public int compare(LdEntry o1, LdEntry o2) {
					return o1.title.compareTo(o2.title);
				}
			});
		}
		
		return results;
	}
	
	public static boolean match(String[] keywords, String text) {
		if(keywords.length == 0) return true;
		if(text == null) return false;
		String lowerText = text.toLowerCase();
		for(String keyword: keywords) {
			if(keyword.length() == 0) continue;
			if(lowerText.contains(keyword.toLowerCase())) return true;
		}
		return false;
	}
	
	public static boolean match(String[] keywords, Collection<String> values) {
		for(String value: values) {
			if(match(keywords, value)) return true;
		}
		return false;
	}
}
