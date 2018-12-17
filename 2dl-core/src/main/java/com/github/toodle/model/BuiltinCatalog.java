package com.github.toodle.model;

import com.github.toodle.model.DataTypeDefinition.Variance;

/**
 * The built-in {@code DataTypeCatalog}.
 */
public class BuiltinCatalog {
	public static final String TYPE_ANY = "any";
	public static final String TYPE_MAP = "map";
	public static final String TYPE_ARRAY = "array";
	public static final String TYPE_PRIMITIVE = "primitive";
	public static final String TYPE_INT = "int";
	public static final String TYPE_NUMBER = "number";
	public static final String TYPE_BOOL = "bool";
	public static final String TYPE_STRING = "string";

	private static final DataTypeCatalog catalog = createBuiltinEnv();

	public static DataTypeCatalog get() {
		return catalog;
	}

	private static DataTypeCatalog createBuiltinEnv() {
		final DataTypeCatalog env = new DataTypeCatalog();

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
}
