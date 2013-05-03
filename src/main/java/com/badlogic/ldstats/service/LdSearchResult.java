package com.badlogic.ldstats.service;

import java.util.List;

import com.badlogic.ldstats.crawler.LdEntry;

public class LdSearchResult {
	public int offset;
	public int total;
	public List<LdEntry> entries;
}
