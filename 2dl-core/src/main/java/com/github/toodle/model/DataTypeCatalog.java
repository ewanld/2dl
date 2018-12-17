package com.github.toodle.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.toodle.model.DataTypeDefinition.Variance;

public class DataTypeCatalog {
	private final Map<String, DataTypeDefinition> defs = new HashMap<>();

	public DataTypeCatalog() {
		this(Collections.emptyList());
	}

	public DataTypeCatalog(Collection<DataTypeDefinition> definitions) {
		definitions.forEach(d -> defs.put(d.getName(), d));
	}

	public DataTypeDefinition get(String name) {
		return defs.get(name);
	}

	public void add(DataTypeDefinition dataType) {
		defs.put(dataType.getName(), dataType);
	}

	private boolean isValid(DataType dataType) {
		final DataTypeDefinition def = get(dataType.getName());
		if (def == null) return false;
		final int paramCount = def.getParamTypes().size();
		if (paramCount != dataType.getParamTypes().size()) return false;
		for (int i = 0; i < paramCount; i++) {
			final DataType paramType = dataType.getParamType(i);
			final DataTypeParamDefinition paramDef = def.getParamType(i);
			if (!isSubstitute(paramType, paramDef.getDataType(), paramDef.getVariance())) return false;
		}
		return true;
	}

	private boolean isSubstitute(String actual, String expected, Variance variance) {
		if (actual.equals(expected)) return true;
		final DataTypeDefinition type = defs.get(actual);
		final DataTypeDefinition expectedType = defs.get(expected);
		switch (variance) {
		case COVARIANT:
			if (type.getSuperType() == null) return false;
			return isSubstitute(type.getSuperType().getName(), expected, variance);
		case CONTRAVARIANT:
			if (expectedType.getSuperType() == null) return false;
			return isSubstitute(actual, expectedType.getSuperType().getName(), variance);
		default:
			throw new RuntimeException();
		}
	}

	public boolean isSubstitute(DataType actual, DataType expected) {
		return isValid(expected) && isSubstitute(actual, expected, Variance.COVARIANT);
	}

	private boolean isSubstitute(DataType actual, DataType expected, Variance variance) {
		if (get(expected.getName()).isTopType()) return true;
		if (!isSubstitute(actual.getName(), expected.getName(), variance)) return false;
		final DataTypeDefinition def = get(actual.getName());
		final int paramCount = def.getParamTypes().size();
		if (paramCount != actual.getParamTypes().size()) return false;
		for (int i = 0; i < paramCount; i++) {
			final DataTypeParamDefinition paramTypeDef = def.getParamType(i);
			if (!isSubstitute(actual.getParamType(i), expected.getParamType(i), paramTypeDef.getVariance()))
				return false;
		}
		return true;
	}

	public boolean isSubstitute(Expr expr, DataType expected) {
		final String typeName = expected.getName();
		if (typeName.equals(BuiltinCatalog.TYPE_ANY)) {
			return true;
		} else if (typeName.equals(BuiltinCatalog.TYPE_STRING)) {
			return expr.isString();
		} else if (typeName.equals(BuiltinCatalog.TYPE_PRIMITIVE)) {
			return expr.isPrimitive();
		} else if (typeName.equals(BuiltinCatalog.TYPE_INT)) {
			return expr.isInt();
		} else if (typeName.equals(BuiltinCatalog.TYPE_NUMBER)) {
			return expr.isBigDecimal();
		} else if (typeName.equals(BuiltinCatalog.TYPE_BOOL)) {
			return expr.getAsBoolean();
		}
		return true;
	}
}
