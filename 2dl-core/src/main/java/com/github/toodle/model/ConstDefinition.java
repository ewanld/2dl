package com.github.toodle.model;

public class ConstDefinition {
	private final String name;
	private final Expr value;

	public ConstDefinition(String name, Expr value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Expr getValue() {
		return value;
	}

}
