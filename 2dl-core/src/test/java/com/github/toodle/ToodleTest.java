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

import com.github.ewanld.objectvisitor.ObjectVisitor;
import com.github.toodle.model.DataType;
import com.github.toodle.model.DataType.Variance;
import com.github.toodle.model.DataTypeEnv;
import com.github.toodle.model.TypeDefinition;
import com.github.toodle.services.ToodleToJsonConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * JUnit tests for the class {@link ObjectVisitor}.
 */
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
		final DataTypeEnv env = DataTypeEnv.createBuiltinEnv();
		assertTrue(env.isSubstitute(dataType(DataTypeEnv.TYPE_ANY), dataType(DataTypeEnv.TYPE_ANY)));
		assertFalse(env.isSubstitute(DataTypeEnv.TYPE_ANY, DataTypeEnv.TYPE_STRING, Variance.COVARIANT));
		assertTrue(env.isSubstitute(DataTypeEnv.TYPE_STRING, DataTypeEnv.TYPE_ANY, Variance.COVARIANT));
		assertTrue(env.isSubstitute(DataTypeEnv.TYPE_ANY, DataTypeEnv.TYPE_STRING, Variance.CONTRAVARIANT));
		assertFalse(env.isSubstitute(DataTypeEnv.TYPE_STRING, DataTypeEnv.TYPE_ANY, Variance.CONTRAVARIANT));

		final DataType string_t = dataType(DataTypeEnv.TYPE_STRING);
		final DataType any_t = dataType(DataTypeEnv.TYPE_ANY);
		final DataType arrayOfString = dataType(DataTypeEnv.TYPE_ARRAY, DataTypeEnv.TYPE_STRING);
		final DataType arrayOfArrayOfString = new DataType(DataTypeEnv.TYPE_ARRAY,
				dataType(DataTypeEnv.TYPE_ARRAY, DataTypeEnv.TYPE_STRING));
		final DataType arrayOfAny = dataType(DataTypeEnv.TYPE_ARRAY, DataTypeEnv.TYPE_ANY);

		assertTrue(env.isSubstitute(string_t, any_t));
		assertFalse(env.isSubstitute(any_t, string_t));
		assertTrue(env.isSubstitute(arrayOfAny, arrayOfAny));
		assertTrue(env.isSubstitute(arrayOfString, arrayOfString));
		assertTrue(env.isSubstitute(arrayOfString, arrayOfAny));
		assertFalse(env.isSubstitute(arrayOfAny, arrayOfString));

		assertTrue(env.isSubstitute(arrayOfArrayOfString, arrayOfAny));
		assertFalse(env.isSubstitute(arrayOfAny, arrayOfArrayOfString));
	}

	public static DataType dataType(String name, String... paramTypes_str) {
		final List<DataType> paramTypes = Stream.of(paramTypes_str).map(paramName -> new DataType(paramName))
				.collect(Collectors.toList());
		return new DataType(name, paramTypes);
	}

}
