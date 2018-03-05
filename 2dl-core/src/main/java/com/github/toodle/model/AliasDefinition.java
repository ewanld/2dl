package com.github.toodle.model;

public class AliasDefinition {
	private final String name;
	private final Type value;

	public AliasDefinition(String name, Type value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Type getValue() {
		return value;
	}

}
