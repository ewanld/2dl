package com.github.toodle.services;

import java.io.IOException;
import java.io.Writer;
import java.util.stream.Collectors;

import com.github.toodle.model.AliasDefinition;
import com.github.toodle.model.VarDefinition;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.github.toodle.model.TypeDefinition;

public class ToodleWriter {
	private final Writer writer;
	private int indentLevel = 0;

	public ToodleWriter(Writer writer) {
		this.writer = writer;
	}

	public void execute(Type rootType) {
		writeType_inner(rootType);
	}

	private void write(String msg, Object... args) {
		try {
			writer.write(String.format(msg, args));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeln() {
		write("\n");
	}

	private void writeln(String msg, Object... args) {
		write(msg + "\n", args);
	}

	private void writeIndent() {
		for (int i = 0; i < indentLevel; i++) {
			try {
				writer.write("\t");
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void writeDefinition(TypeDefinition typeDefinition) {
		writeIndent();
		write(typeDefinition.getName() + ": ");
		writeType(typeDefinition.getType());
		writeln();
	}

	private void writeAlias(AliasDefinition alias) {
		writeIndent();
		write("alias %s = ", alias.getName());
		writeType(alias.getValue());
		writeln();
	}

	private void writeConstDefinition(VarDefinition cst) {
		writeIndent();
		write("const %s = %s", cst.getName(), cst.getValue().toLiteral());
		writeln();
	}

	private void writeType(Type type) {
		write(type.getName());

		// write annotations
		for (final TypeAnnotation annotation : type.getAnnotations().values()) {
			write(" ");
			write(annotation.getName());
			if (!annotation.getObjectParams().isEmpty()) {
				final String params_str = annotation.getParamsAsLiterals().stream().collect(Collectors.joining(", "));
				write("(" + params_str + ")");
			}
		}

		if (!type.getSubDefinitions().isEmpty() || !type.getAliasDefinitions().isEmpty()) {
			writeln("{");
			indentLevel++;

			writeType_inner(type);

			indentLevel--;
			writeln("}");
		}
	}

	private void writeType_inner(Type type) {
		//write alias definitions
		type.getAliasDefinitions().forEach(this::writeAlias);

		//write const definitions
		type.getConstDefinitions().forEach(this::writeConstDefinition);

		// write sub-definitions
		type.getSubDefinitions().forEach(this::writeDefinition);
	}
}
