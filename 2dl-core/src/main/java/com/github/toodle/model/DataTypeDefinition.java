package com.github.toodle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DataTypeDefinition {
	private final String name;
	private final List<DataTypeParamDefinition> paramTypes = new ArrayList<>();
	private final String superType;

	public DataTypeDefinition(String name, String superType, Collection<DataTypeParamDefinition> paramTypes) {
		this.name = name;
		this.paramTypes.addAll(paramTypes);
		this.superType = superType;
	}

	public DataTypeDefinition(String name, String superType, DataTypeParamDefinition... paramTypes) {
		this(name, superType, Arrays.asList(paramTypes));
	}

	public boolean isTopType() {
		return superType == null;
	}

	public String getSuperType() {
		return superType;
	}

	public List<DataTypeParamDefinition> getParamTypes() {
		return paramTypes;
	}

	public DataTypeParamDefinition getParamType(int index) {
		return paramTypes.get(index);
	}

	public String getName() {
		return name;
	}

}
