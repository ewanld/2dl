package com.github.toodle.model;

import com.github.toodle.model.DataType.Variance;

public class DataTypeParamDefinition {
	private final DataType dataType;
	private final Variance variance;

	public DataTypeParamDefinition(String dataType, Variance variance) {
		this.dataType = new DataType(dataType);
		this.variance = variance;
	}

	public Variance getVariance() {
		return variance;
	}

	public DataType getDataType() {
		return dataType;
	}

}