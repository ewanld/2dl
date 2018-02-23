package com.github.toodle.model;

public class SourceLocation {
	private final String fileName;
	private final int line;

	public SourceLocation(String fileName, int line) {
		this.fileName = fileName;
		this.line = line;
	}

	public int getLine() {
		return line;
	}

	@Override
	public String toString() {
		return fileName + ":" + line;
	}
}
