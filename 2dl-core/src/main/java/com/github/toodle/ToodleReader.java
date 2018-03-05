package com.github.toodle;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.github.toodle.antlr.MyToodleListener;
import com.github.toodle.model.Type;
import com.github.toodle.validator.ToodleSchema;
import com.github.toodle.validator.ToodleValidationException;

public class ToodleReader {
	private final Reader definitionsReader;
	private final Reader schemaReader;

	/**
	 * @param definitionsReader The reader containing definitions in 2dl format.
	 * @param schemaReader Optional. If not null, the definitions will be validated against the schema.
	 */
	public ToodleReader(Reader definitionsReader, Reader schemaReader) {
		this.definitionsReader = definitionsReader;
		this.schemaReader = schemaReader;
	}

	public ToodleReader(Reader definitionsReader) {
		this(definitionsReader, null);
	}

	public Type read() throws IOException {
		final Type rootType = read(definitionsReader);
		if (schemaReader != null) {
			final Type schemaRootType = read(schemaReader);

			// validate schema against meta-schema
			try (final InputStreamReader metaSchemaReader = new InputStreamReader(
					ToodleReader.class.getClassLoader().getResourceAsStream("2dl-schema.2dl"), "UTF-8")) {
				final Type metaSchemaRootType = read(metaSchemaReader);
				final ToodleSchema schemaValidator = new ToodleSchema(metaSchemaRootType);
				if (!schemaValidator.validate(schemaRootType)) {
					throw new ToodleValidationException(schemaValidator.getViolations());
				}

				// validate definitions against schema
				final ToodleSchema validator = new ToodleSchema(schemaRootType);
				if (!validator.validate(rootType)) {
					throw new ToodleValidationException(validator.getViolations());
				}
			}
		}
		return rootType;
	}

	private static Type read(Reader reader) throws IOException {
		final ToodleLexer lexer = new ToodleLexer(CharStreams.fromReader(reader));
		// Get a list of matched tokens
		final CommonTokenStream tokens = new CommonTokenStream(lexer);

		// Pass the tokens to the parser
		final ToodleParser parser = new ToodleParser(tokens);

		final MyToodleListener listener = new MyToodleListener();
		parser.addParseListener(listener);
		parser.definitions();
		return listener.getRootType();
	}
}
