package com.github.toodle.services;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import com.github.toodle.model.TypeDefinition;

public class MyDefinitionsGenerator extends AbstractGenerator {
	private final String packageName;
	private final String topLevelClassName;
	private final Collection<TypeDefinition> definitions;

	public MyDefinitionsGenerator(Writer writer, String packageName, String topLevelClassName,
			Collection<TypeDefinition> definitions) {
		super(writer);
		this.packageName = packageName;
		this.topLevelClassName = topLevelClassName;
		this.definitions = definitions;
	}

	public MyDefinitionsGenerator(String outputDirectory, String packageName, String topLevelClassName,
			Collection<TypeDefinition> definitions) throws IOException {
		super(outputDirectory, packageName, topLevelClassName);
		this.packageName = packageName;
		this.topLevelClassName = topLevelClassName;
		this.definitions = definitions;
	}

	@Override
	public void generate() throws IOException {
		writeln("package %s;", packageName);
		writeln();
		writeln("import %s;", TypeDefinition.class.getName());
		writeln();
		writeln("public class %s {", topLevelClassName);
		writeln("	public %s(Collection<Definition> definitions) {", topLevelClassName);
		writeln("		for (Definition d : definitions) {");
		writeln("			String typeName = d.getType().getName();");
		for (final TypeDefinition d : definitions) {
			writeln("			if (typeName.equals(%s) {", toJavaLiteral(d.getName()));
			writeln("			}");
		}
		writeln("		}");
		writeln("	}");
		writeln("}");

	}

}
