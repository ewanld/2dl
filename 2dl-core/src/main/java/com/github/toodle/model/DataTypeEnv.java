package com.github.toodle.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.toodle.model.DataTypeDefinition.Variance;

public class DataTypeEnv {
	public static final String TYPE_ANY = "any";
	public static final String TYPE_MAP = "map";
	public static final String TYPE_ARRAY = "array";
	public static final String TYPE_PRIMITIVE = "primitive";
	public static final String TYPE_INT = "int";
	public static final String TYPE_NUMBER = "number";
	public static final String TYPE_BOOL = "bool";
	public static final String TYPE_STRING = "string";

	private final Map<String, DataTypeDefinition> defs = new HashMap<>();

	public DataTypeEnv() {
		this(Collections.emptyList());
	}

	public DataTypeEnv(Collection<DataTypeDefinition> definitions) {
		definitions.forEach(d -> defs.put(d.getName(), d));
	}

	public DataTypeDefinition get(String name) {
		return defs.get(name);
	}

	public void add(DataTypeDefinition dataType) {
		defs.put(dataType.getName(), dataType);
	}

	public static DataTypeEnv createBuiltinEnv() {
		final DataTypeEnv env = new DataTypeEnv();

		final DataTypeParamDefinition anyCovariant = new DataTypeParamDefinition(TYPE_ANY, Variance.COVARIANT);
		final DataTypeParamDefinition primitiveCovariant = new DataTypeParamDefinition(TYPE_PRIMITIVE,
				Variance.COVARIANT);

		final DataTypeDefinition any = new DataTypeDefinition(TYPE_ANY, null);
		env.add(any);
		final DataTypeDefinition array = new DataTypeDefinition(TYPE_ARRAY, any, anyCovariant);
		env.add(array);
		final DataTypeDefinition map = new DataTypeDefinition(TYPE_MAP, any, primitiveCovariant, anyCovariant);
		env.add(map);
		final DataTypeDefinition primitive = new DataTypeDefinition(TYPE_PRIMITIVE, any);
		env.add(primitive);
		final DataTypeDefinition string = new DataTypeDefinition(TYPE_STRING, primitive);
		env.add(string);
		final DataTypeDefinition bool = new DataTypeDefinition(TYPE_BOOL, string);
		env.add(bool);
		final DataTypeDefinition number = new DataTypeDefinition(TYPE_NUMBER, primitive);
		env.add(number);
		final DataTypeDefinition int_t = new DataTypeDefinition(TYPE_INT, number);
		env.add(int_t);
		return env;
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
		if (typeName.equals(TYPE_ANY)) {
			return true;
		} else if (typeName.equals(TYPE_STRING)) {
			return expr.isString();
		} else if (typeName.equals(TYPE_PRIMITIVE)) {
			return expr.isPrimitive();
		} else if (typeName.equals(TYPE_INT)) {
			return expr.isInt();
		} else if (typeName.equals(TYPE_NUMBER)) {
			return expr.isBigDecimal();
		} else if (typeName.equals(TYPE_BOOL)) {
			return expr.getAsBoolean();
		}
		return true;
	}
}
