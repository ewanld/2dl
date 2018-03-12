package com.github.toodle.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.toodle.services.ToodleVisitor;
import com.github.visitorj.IdentifiedVisitable;
import com.github.visitorj.VisitEvent;
import com.github.visitorj.VisitResult;
import com.github.visitorj.Visitable;
import com.github.visitorj.VisitableList;

public class Type implements Visitable<ToodleVisitor> {
	private static final String IDENTIFIER_TYPE_PARAM = "TYPE_PARAM";
	private static final String IDENTIFIER_SUB_DEFINITION = "SUB_DEFINITION";
	private String name;
	private final Map<String, TypeAnnotation> annotations = new HashMap<>();
	private final List<TypeDefinition> subDefinitions = new ArrayList<>();
	private final Map<String, AliasDefinition> aliasDefinitions = new HashMap<>();
	private final Map<String, VarDefinition> constDefinitions = new HashMap<>();
	private final List<Type> typeParams = new ArrayList<>();
	// the container type (in case of a type parameter or a subdefinition)
	private final Type parent;

	public Type(String name, Type parent) {
		this.name = name;
		this.parent = parent;
	}

	public Type(Type parent) {
		this(null, parent);
	}

	public static String exprToLiteral(final Object o) {
		if (o instanceof BigDecimal) {
			return ((BigDecimal) o).toPlainString();
		} else if (o instanceof String) {
			// TODO escape
			return "\"" + (String) o + "\"";
		} else {
			throw new IllegalArgumentException("Unknown type: " + o.getClass());
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, TypeAnnotation> getAnnotations() {
		return annotations;
	}

	public TypeAnnotation getAnnotation(String annotationName) {
		return annotations.get(annotationName);
	}

	public Collection<TypeDefinition> getSubDefinitions() {
		return subDefinitions;
	}

	public Map<String, TypeDefinition> getSubDefinitionsOfType(String typeName) {
		return subDefinitions.stream().filter(
				d -> Objects.equals(Optional.ofNullable(d.getType()).map(Type::getName).orElse(null), typeName))
				.collect(Collectors.toMap(TypeDefinition::getName, Function.identity()));
	}

	public TypeDefinition getSubDefinition(String name) {
		return subDefinitions.stream().filter(d -> Objects.equals(d.getName(), name)).findAny().orElse(null);
	}

	public Type getParent() {
		return parent;
	}

	@Override
	public void event(VisitEvent event, ToodleVisitor visitor) {
		visitor.event(event, this);
	}

	@Override
	public VisitResult visit(ToodleVisitor visitor, String identifier) {
		return visitor.visit(this, identifier);
	}

	@Override
	public Iterable<IdentifiedVisitable<ToodleVisitor>> getVisitableChildren() {
		final VisitableList<ToodleVisitor> visitableChildren = new VisitableList<>();
		visitableChildren.add(annotations.values());
		visitableChildren.add(subDefinitions, IDENTIFIER_SUB_DEFINITION);
		if (!typeParams.isEmpty()) visitableChildren.add(new TypeParamCollection(typeParams), IDENTIFIER_TYPE_PARAM);
		return visitableChildren;
	}

	public List<Type> getTypeParams() {
		return typeParams;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(name);
		if (!typeParams.isEmpty()) sb.append("<")
				.append(typeParams.stream().map(Type::toString).collect(Collectors.joining(", "))).append(">");
		annotations.values().forEach(a -> sb.append(" ").append(a.toString()));
		if (!subDefinitions.isEmpty()) sb.append(" { ... }");
		return sb.toString();
	}

	public void addAliasDefinition(String name, Type value) {
		aliasDefinitions.put(name, new AliasDefinition(name, value));
	}

	public Collection<AliasDefinition> getAliasDefinitions() {
		return aliasDefinitions.values();
	}

	public Map<String, AliasDefinition> getAliasDefinitionMap() {
		return aliasDefinitions;
	}

	public void addConstDefinition(String constName, Expr currentConstValue) {
		constDefinitions.put(constName, new VarDefinition(constName, currentConstValue));
	}

	public Map<String, VarDefinition> getConstDefinitionMap() {
		return constDefinitions;
	}

	public Collection<VarDefinition> getConstDefinitions() {
		return constDefinitions.values();
	}

	public Object getConstValue(String constName) {
		return constDefinitions.get(constName);
	}
}
