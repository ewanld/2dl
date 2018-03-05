package com.github.toodle;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.github.ewanld.objectvisitor.ObjectVisitor;
import com.github.toodle.model.TypeDefinition;
import com.github.toodle.services.ToodleToJsonConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;

/**
 * JUnit tests for the class {@link ObjectVisitor}.
 */
public class ToodleTest {
	private static final File last = getResourceFile("ToodleTest-last.txt");
	private static final File ref = getResourceFile("ToodleTest-ref.txt");
	private final Writer writer;
	private final Gson gson;
	private final JsonWriter jsonWriter;

	public ToodleTest() throws IOException {
		writer = new BufferedWriter(new FileWriter(last));
		gson = new GsonBuilder().setPrettyPrinting().create();
		jsonWriter = gson.newJsonWriter(writer);
	}

	private static File getResourceFile(String resourceName) {
		return new File(ToodleTest.class.getClassLoader().getResource(resourceName).getFile());
	}

	public void tearDown() throws IOException {
		writer.close();
	}

	@Test
	public void testAll() throws Exception {
		testDatabaseFile();
		jsonWriter.close();
		writer.close();
		assertTrue(FileUtils.contentEquals(last, ref));
	}

	private void testDatabaseFile() throws IOException, FileNotFoundException {
		try (final Reader definitionsReader = new BufferedReader(new FileReader(getResourceFile("database.2dl")));
				Reader schemaReader = new BufferedReader(new FileReader(getResourceFile("schema.2dl")))) {
			final ToodleReader toodleReader = new ToodleReader(definitionsReader, schemaReader);
			final Collection<TypeDefinition> definitions = toodleReader.read().getSubDefinitions();
			final JsonElement definitions_json = new ToodleToJsonConverter().toJson(definitions);
			gson.toJson(definitions_json, jsonWriter);
		}
	}

}
