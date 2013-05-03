package com.badlogic.ldstats.service;

import java.util.List;
import java.util.Map;

import com.badlogic.ldstats.crawler.LdEntry;

public interface Index {
	public void index(String ld, Map<String, List<LdEntry>> ludumDares);
	public List<LdEntry> query(String titleTextQuery, String userQuery, String linkQuery, String typeQuery, String ldQuery, String sortBy);
}
