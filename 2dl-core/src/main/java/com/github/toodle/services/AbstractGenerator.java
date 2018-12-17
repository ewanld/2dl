package com.github.toodle.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public abstract class AbstractGenerator implements AutoCloseable {
	protected final Writer writer;

	public AbstractGenerator(Writer writer) {
		assert writer != null;
		this.writer = writer;
	}

	public AbstractGenerator(String outputDirectory, String packageName, String topLevelClassName)
			throws IOException {
		final File file = toFile(outputDirectory, packageName, topLevelClassName);
		file.getParentFile().mkdirs();
		this.writer = new BufferedWriter(new FileWriter(file));
	}

	public static File toFile(String outputDirectory, String packageName, String topLevelClassName) {
		return new File(outputDirectory, packageName.replace('.', '/') + "/" + topLevelClassName + ".java");
	}

	public void writeln() throws IOException {
		writeln("");
	}

	public void writeln(String format, Object... args) throws IOException {
		writer.write(String.format(format, args) + "\n");
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	public abstract void generate() throws IOException;

	protected final String escapeJavaString(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	protected final String toJavaLiteral(String s) {
		return "\"" + escapeJavaString(s) + "\"";
	}

	protected final String toJavaClassName(String s) {
		return toJavaName(s, true);
	}

	protected static String toJavaFieldName(String identifier) {
		return toJavaName(identifier, false);
	}

	protected static String titleCase(String s) {
		if (s == null) return null;
		if (s.isEmpty()) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
	}

	protected static String senstenceCase(String s) {
		if (s == null) return null;
		if (s.isEmpty()) return s;
		return s.substring(0, 1).toLowerCase() + s.substring(1, s.length());
	}

	private static String toJavaName(String identifier, boolean firstLetterUpper) {
		final StringBuilder res = new StringBuilder();
		if (Character.isDigit(identifier.charAt(0))) {
			res.append("_");
		}
		final String javaName = identifier.replaceAll("[^\\w]", "_");
		res.append(
				firstLetterUpper ? javaName.substring(0, 1).toUpperCase() : javaName.substring(0, 1).toLowerCase());
		res.append(javaName.substring(1));
		return res.toString();
	}
}
