package com.github.toodle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;;

public class DataType {
	private final String name;
	private final List<DataType> paramTypes = new ArrayList<>();

	public DataType(String name, Collection<DataType> paramTypes) {
		this.name = name;
		this.paramTypes.addAll(paramTypes);
	}

	public DataType(String name, DataType... paramTypes) {
		this(name, Arrays.asList(paramTypes));
	}

	public DataType(Type type) {
		this(type.getName(), type.getTypeParams().stream().map(DataType::new).collect(Collectors.toList()));
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
