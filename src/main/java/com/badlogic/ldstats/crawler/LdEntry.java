package com.badlogic.ldstats.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LdEntry {
	public String uid;
	public String type;
	public String title;
	public String user;
	public Map<String, String> links = new HashMap<String, String>();
	public Map<String, Float> ratings = new HashMap<String, Float>();
	public String text;
	public List<String> screenshotUrls = new ArrayList<String>();
	public List<LdComment> comments = new ArrayList<LdComment>();
}
