package com.badlogic;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.junit.Test;

import com.badlogic.ldstats.crawler.LdEntry;

public class EntrySerializerTest {
	@Test
	public void testEntrySerializer() throws JsonGenerationException, JsonMappingException, IOException {
		LdEntry entry = new LdEntry();
		ObjectWriter writer = new ObjectMapper().defaultPrettyPrintingWriter();
		System.out.println(writer.writeValueAsString(entry));
	}
}
