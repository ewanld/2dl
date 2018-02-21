package com.github.toodle;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.github.toodle.antlr.MyToodleListener;
import com.github.toodle.model.Definition;
import com.github.toodle.model.Type;
import com.github.toodle.validator.ToodleValidationException;
import com.github.toodle.validator.ToodleValidator;

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

	public Collection<Definition> read() throws IOException {
		final Collection<Definition> definitions = readDefinitions(definitionsReader);
		if (schemaReader != null) {
			final Collection<Definition> schemaDefinitions = readDefinitions(schemaReader);

			// validate schema against meta-schema
			try (final InputStreamReader metaSchemaReader = new InputStreamReader(
					ToodleReader.class.getClassLoader().getResourceAsStream("2dl-schema.2dl"), "UTF-8")) {
				final Collection<Definition> metaSchemaDefinitions = readDefinitions(metaSchemaReader);
				final ToodleValidator schemaValidator = new ToodleValidator(metaSchemaDefinitions);
				if (!schemaValidator.validate(schemaDefinitions)) {
					throw new ToodleValidationException(schemaValidator.getViolations());
				}

				// validate definitions against schema
				final ToodleValidator validator = new ToodleValidator(schemaDefinitions);
				if (!validator.validate(definitions)) {
					throw new ToodleValidationException(validator.getViolations());
				}
			}
		}
		return definitions;
	}

	private static Collection<Definition> readDefinitions(Reader reader) throws IOException {
		final ToodleLexer lexer = new ToodleLexer(CharStreams.fromReader(reader));
		// Get a list of matched tokens
		final CommonTokenStream tokens = new CommonTokenStream(lexer);

		// Pass the tokens to the parser
		final ToodleParser parser = new ToodleParser(tokens);

		final MyToodleListener listener = new MyToodleListener();
		parser.addParseListener(listener);
		parser.definitions();
		final Type rootType = listener.getRootType();
		return rootType.getChildren();
	}
}
