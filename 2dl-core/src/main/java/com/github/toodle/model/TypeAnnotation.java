package com.github.toodle.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.visitorj.VisitEvent;
import com.github.visitorj.VisitResult;
import com.github.visitorj.Visitable;

import com.github.toodle.services.ToodleVisitor;

public class TypeAnnotation implements Visitable<ToodleVisitor> {
	private String name;
	private final List<Object> parameters = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Object> getObjectParams() {
		return parameters;
	}

	public List<String> getStringParams() {
		final List<String> res = new ArrayList<>(parameters.size());
		for (final Object p : parameters) {
			if (p instanceof String) {
				res.add(((String) p));
			} else {
				throw new RuntimeException(
						String.format("getStringParams() failed because one parameter is a %s! Parameters are: %s",
								p.getClass().getSimpleName(), parameters));
			}
		}
		return res;
	}

	public List<Integer> getIntParams() {
		final List<Integer> res = new ArrayList<>(parameters.size());
		for (final Object p : parameters) {
			if (p instanceof BigDecimal) {
				res.add(((BigDecimal) p).intValueExact());
			} else {
				throw new RuntimeException(
						String.format("getIntParams() failed because one parameter is a %s! Parameters are: %s",
								p.getClass().getSimpleName(), parameters));
			}
		}
		return res;
	}

	public List<Long> getLongParams() {
		final List<Long> res = new ArrayList<>(parameters.size());
		for (final Object p : parameters) {
			if (p instanceof BigDecimal) {
				res.add(((BigDecimal) p).longValueExact());
			} else {
				throw new RuntimeException(
						String.format("getLongParams() failed because one parameter is a %s! Parameters are: %s",
								p.getClass().getSimpleName(), parameters));
			}
		}
		return res;
	}

	public List<BigDecimal> getBigDecimalParams() {
		final List<BigDecimal> res = new ArrayList<>(parameters.size());
		for (final Object p : parameters) {
			if (p instanceof BigDecimal) {
				res.add((BigDecimal) p);

			} else {
				throw new RuntimeException(String.format(
						"getBigDecimalParams() failed because one parameter is a %s! Parameters are: %s",
						p.getClass().getSimpleName(), parameters));
			}
		}
		return res;
	}

	public List<Boolean> getBooleanParams() {
		final List<Boolean> res = new ArrayList<>(parameters.size());
		for (final Object p : parameters) {
			if (p instanceof String) {
				res.add(parseBoolean((String) p));
			} else {
				throw new RuntimeException(
						String.format("getBooleanParams() failed because one parameter is a %s! Parameters are: %s",
								p.getClass().getSimpleName(), parameters));
			}
		}
		return res;
	}

	public Boolean parseBoolean(String value) {
		if (value.equals("true")) {
			return true;
		} else if (value.equals("false")) {
			return false;
		} else {
			throw new RuntimeException(String.format("Value '%s' cannot be cast to Boolean! ", value));
		}
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
		if (!parameters.isEmpty()) sb.append("(")
				.append(parameters.stream().map(Object::toString).collect(Collectors.joining(", "))).append(")");
		return sb.toString();
	}
}
