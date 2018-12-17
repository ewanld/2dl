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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.github.toodle.model.BuiltinCatalog;
import com.github.toodle.model.DataType;
import com.github.toodle.model.DataTypeDefinition;
import com.github.toodle.model.DataTypeCatalog;
import com.github.toodle.model.TypeDefinition;
import com.github.toodle.services.ToodleToJsonConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class ToodleTest {
	private static final File last = new File(getResourceFile("ToodleTest-ref.txt").getParentFile(),
			"ToodleTest-last.txt");
	private static final File ref = getResourceFile("ToodleTest-ref.txt");
	private final Gson gson;

	public ToodleTest() throws IOException {
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

	private static File getResourceFile(String resourceName) {
		return new File(ToodleTest.class.getClassLoader().getResource(resourceName).getFile());
	}

	@Test
	public void testAll() throws Exception {
		try (Writer writer = new BufferedWriter(new FileWriter(last))) {
			testDatabaseFile(writer);
		}
		//		System.out.println(String.format("Comparing %s with %s", last, ref));
		//		System.out.println("Contents of 'last' file:");
		//		Files.lines(last.toPath()).forEach(System.out::println);
		//		System.out.println("Contents of 'ref' file:");
		//		Files.lines(ref.toPath()).forEach(System.out::println);
		//		assertTrue(FileUtils.contentEquals(last, ref));
	}

	private void testDatabaseFile(Writer writer) throws IOException, FileNotFoundException {
		try (final Reader definitionsReader = new BufferedReader(new FileReader(getResourceFile("database.2dl")));
				Reader schemaReader = new BufferedReader(new FileReader(getResourceFile("schema.2dl")))) {
			final ToodleReader toodleReader = new ToodleReader(definitionsReader, schemaReader);
			final Collection<TypeDefinition> definitions = toodleReader.read().getSubDefinitions();
			final JsonElement definitions_json = new ToodleToJsonConverter().toJson(definitions);
			gson.toJson(definitions_json, writer);
		}
	}

	@Test
	public void testDataTypes() {
		final DataTypeCatalog env = BuiltinCatalog.get();

		final DataType string_t = dataType(BuiltinCatalog.TYPE_STRING);
		final DataType any_t = dataType(BuiltinCatalog.TYPE_ANY);
		final DataType arrayOfString = dataType(BuiltinCatalog.TYPE_ARRAY, BuiltinCatalog.TYPE_STRING);
		final DataType arrayOfArrayOfString = new DataType(BuiltinCatalog.TYPE_ARRAY,
				dataType(BuiltinCatalog.TYPE_ARRAY, BuiltinCatalog.TYPE_STRING));
		final DataType arrayOfAny = dataType(BuiltinCatalog.TYPE_ARRAY, BuiltinCatalog.TYPE_ANY);

		assertTrue(env.isSubstitute(string_t, any_t));
		assertFalse(env.isSubstitute(any_t, string_t));
		assertTrue(env.isSubstitute(arrayOfAny, arrayOfAny));
		assertTrue(env.isSubstitute(arrayOfString, arrayOfString));
		assertTrue(env.isSubstitute(arrayOfString, arrayOfAny));
		assertFalse(env.isSubstitute(arrayOfAny, arrayOfString));

		assertTrue(env.isSubstitute(arrayOfArrayOfString, arrayOfAny));
		assertFalse(env.isSubstitute(arrayOfAny, arrayOfArrayOfString));

		final DataTypeDefinition any_def = env.get(BuiltinCatalog.TYPE_ANY);
		final DataTypeDefinition bool_def = env.get(BuiltinCatalog.TYPE_BOOL);
		final DataTypeDefinition int_def = env.get(BuiltinCatalog.TYPE_INT);
		final DataTypeDefinition number_def = env.get(BuiltinCatalog.TYPE_NUMBER);
		final DataTypeDefinition array_def = env.get(BuiltinCatalog.TYPE_ARRAY);
		final DataTypeDefinition primitive_def = env.get(BuiltinCatalog.TYPE_PRIMITIVE);
		assertEquals(DataTypeDefinition.lowestCommonAncestor(bool_def, int_def), primitive_def);

	}

	public static DataType dataType(String name, String... paramTypes_str) {
		final List<DataType> paramTypes = Stream.of(paramTypes_str).map(paramName -> new DataType(paramName))
				.collect(Collectors.toList());
		return new DataType(name, paramTypes);
	}

}
