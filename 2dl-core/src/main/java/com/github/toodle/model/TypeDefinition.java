package com.github.toodle.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.github.visitorj.IdentifiedVisitable;
import com.github.visitorj.VisitEvent;
import com.github.visitorj.VisitResult;
import com.github.visitorj.Visitable;
import com.github.visitorj.VisitableList;

import com.github.toodle.services.ToodleVisitor;

public class TypeDefinition implements Visitable<ToodleVisitor> {
	private String name;
	private final Set<String> modifiers = new HashSet<>();
	private Type type;
	private final VisitableList<ToodleVisitor> visitableChildren = new VisitableList<>();
	private SourceLocation location;

	public TypeDefinition(String name, Collection<String> modifiers, Type type) {
		this.name = name;
		this.type = type;
		this.modifiers.addAll(modifiers);
		visitableChildren.add(type);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Set<String> getModifiers() {
		return modifiers;
	}

	public boolean hasModifier(String modifier) {
		return modifiers.contains(modifier);
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
		return visitableChildren;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		modifiers.forEach(m -> sb.append(m).append(" "));
		sb.append(name);
		if (type != null) sb.append(": ").append(type.toString());
		return sb.toString();
	}

	public SourceLocation getLocation() {
		return location;
	}

	public void setLocation(SourceLocation location) {
		this.location = location;
	}
}
