package com.github.toodle.services;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.github.toodle.model.TypeDefinition;
import com.github.toodle.model.Type;

public class MyTypesGenerator extends AbstractGenerator {
	private final String packageName;
	private final String topLevelClassName;
	private final Collection<TypeDefinition> definitions;

	public MyTypesGenerator(Writer writer, String packageName, String topLevelClassName,
			Collection<TypeDefinition> definitions) {
		super(writer);
		this.packageName = packageName;
		this.topLevelClassName = topLevelClassName;
		this.definitions = definitions;
	}

	public MyTypesGenerator(String outputDirectory, String packageName, String topLevelClassName,
			Collection<TypeDefinition> definitions) throws IOException {
		super(outputDirectory, packageName, topLevelClassName);
		this.packageName = packageName;
		this.topLevelClassName = topLevelClassName;
		this.definitions = definitions;
	}

	@Override
	public void generate() throws IOException {
		writeln("package %s", packageName);
		writeln();
		writeln("import java.util.List");
		writeln("import %s", Type.class.getName());
		writeln();
		writeln("public class %s {", topLevelClassName);
		for (final TypeDefinition definition : definitions) {
			genDefinitionClass(definition);
		}
		writeln("}");
	}

	public void genDefinitionClass(TypeDefinition definition) throws IOException {
		final String className = definitionToClassName(definition);
		writeln("	public static class %s {", className);
		writeln("		private final Type type;");
		genTypeAnnotationFields(definition.getType());
		writeln("		");
		writeln("		public %s(Type type) {", className);
		writeln("			this.type = type;");

		final Map<String, TypeDefinition> annotations = definition.getType().getSubDefinitionsOfType("annotation");
		for (final Entry<String, TypeDefinition> e : annotations.entrySet()) {
			final Type paramsType = e.getValue().getType().getTypeParams().get(0);
			final String fieldName = toJavaFieldName(e.getKey());
			writeln("			this.%s = type.getAnnotation(%s).%s;", fieldName, toJavaLiteral(e.getKey()),
					getAnnotationsParamsCall(paramsType, false));
		}

		writeln("		}");
		writeln("	}");
		writeln();
	}

	public String definitionToClassName(TypeDefinition definition) {
		final String defName = definition.getName();

		if (defName.equals("*")) {
			return "CustomType";
		} else if (defName.equals("type")) {
			return "MetaType";
		} else {
			return toJavaClassName(defName) + "Type";
		}
	}

	public void genTypeAnnotationFields(Type type) throws IOException {
		final Map<String, TypeDefinition> annotations = type.getSubDefinitionsOfType("annotation");
		for (final Entry<String, TypeDefinition> e : annotations.entrySet()) {
			final Type paramsType = e.getValue().getType().getTypeParams().get(0);
			final String annotationsParamsJavaType = getAnnotationsParamsType(paramsType, true);
			final String fieldName = toJavaFieldName(e.getKey());
			final String getterName = fieldName;
			writeln("		private %s %s;", annotationsParamsJavaType, fieldName);
			writeln("		public %s %s() { return %s; }", annotationsParamsJavaType, getterName, fieldName);
		}
	}

	private String getAnnotationsParamsType(Type annotationParamsType, boolean primitiveAllowed) {
		if (annotationParamsType.getName().equals("primitive")) {
			return "Object";
		} else if (annotationParamsType.getName().equals("bool")) {
			return primitiveAllowed ? "boolean" : "Boolean";
		} else if (annotationParamsType.getName().equals("string")) {
			return "String";
		} else if (annotationParamsType.getName().equals("number")) {
			return "Number";
		} else if (annotationParamsType.getName().equals("int")) {
			return primitiveAllowed ? "int" : "Integer";
		} else if (annotationParamsType.getName().equals("variadic")) {
			return "List<" + getAnnotationsParamsType(annotationParamsType.getTypeParams().get(0), false) + ">";
		} else {
			throw new RuntimeException("Unknown type: " + annotationParamsType.getName());
		}
	}

	private String getAnnotationsParamsCall(Type annotationParamsType, boolean list) {
		if (annotationParamsType.getName().equals("primitive")) {
			return list ? "getObjectParams()" : "getObjectParams().get(0)";
		} else if (annotationParamsType.getName().equals("bool")) {
			return list ? "getBooleanParams()" : "getBooleanParams().get(0)";
		} else if (annotationParamsType.getName().equals("string")) {
			return list ? "getStringParams()" : "getStringParams().get(0)";
		} else if (annotationParamsType.getName().equals("number")) {
			return list ? "getNumberParams()" : "getNumberParams().get(0)";
		} else if (annotationParamsType.getName().equals("int")) {
			return list ? "getIntParams()" : "getIntParams().get(0)";
		} else if (annotationParamsType.getName().equals("variadic")) {
			return getAnnotationsParamsCall(annotationParamsType.getTypeParams().get(0), true);
		} else {
			throw new RuntimeException("Unknown type: " + annotationParamsType.getName());
		}
	}

}
