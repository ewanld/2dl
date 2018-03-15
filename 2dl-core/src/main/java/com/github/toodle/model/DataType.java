package com.github.toodle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;;

public class DataType {
	private final String name;
	private final List<DataType> paramTypes = new ArrayList<>();
	
	public enum Variance {
		COVARIANT, CONTRAVARIANT
	}
	
	public DataType(String name, Collection<DataType> paramTypes) {
		this.name = name;
		this.paramTypes.addAll(paramTypes);
	}

	public DataType(String name, DataType... paramTypes) {
		this(name, Arrays.asList(paramTypes));
	}

	public List<DataType> getParamTypes() {
		return paramTypes;
	}

	public DataType getParamType(int index) {
		return paramTypes.get(index);
	}

	public String getName() {
		return name;
	}

}
