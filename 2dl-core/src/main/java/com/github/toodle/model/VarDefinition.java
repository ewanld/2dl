package com.github.toodle.model;

public class VarDefinition {
	private final String name;
	private final Expr value;

	public VarDefinition(String name, Expr value) {
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
