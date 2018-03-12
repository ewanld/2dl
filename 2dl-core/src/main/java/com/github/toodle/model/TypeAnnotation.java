package com.github.toodle.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.toodle.services.ToodleVisitor;
import com.github.visitorj.VisitEvent;
import com.github.visitorj.VisitResult;
import com.github.visitorj.Visitable;

public class TypeAnnotation implements Visitable<ToodleVisitor> {
	private String name;
	private final List<Expr> parameters = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Object> getObjectParams() {
		return parameters.stream().map(Expr::getAsObject).collect(Collectors.toList());
	}

	public List<Expr> getExprParams() {
		return Collections.unmodifiableList(parameters);
	}

	public List<Expr> getExprParams_mutable() {
		return parameters;
	}

	public List<String> getStringParams() {
		return parameters.stream().map(Expr::getAsString).collect(Collectors.toList());
	}

	public List<Integer> getIntParams() {
		return parameters.stream().map(Expr::getAsInt).collect(Collectors.toList());
	}

	public List<Long> getLongParams() {
		return parameters.stream().map(Expr::getAsLong).collect(Collectors.toList());
	}

	public List<BigDecimal> getBigDecimalParams() {
		return parameters.stream().map(Expr::getAsBigDecimal).collect(Collectors.toList());
	}

	public List<Boolean> getBooleanParams() {
		return parameters.stream().map(Expr::getAsBoolean).collect(Collectors.toList());
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
	public String toString() {
		final StringBuilder sb = new StringBuilder(name);
		if (!parameters.isEmpty()) sb.append("(").append(
				parameters.stream().map(Expr::getAsObject).map(Object::toString).collect(Collectors.joining(", ")))
				.append(")");
		return sb.toString();
	}

	public List<String> getParamsAsLiterals() {
		return parameters.stream().map(Expr::toLiteral).collect(Collectors.toList());
	}

}
