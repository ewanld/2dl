package com.github.toodle.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Expr {
	// Possible types: BigDecimal, String, Var, List
	private final Object value;

	public Expr(String value) {
		Objects.requireNonNull(value);
		this.value = value;
	}

	public Expr(BigDecimal value) {
		Objects.requireNonNull(value);
		this.value = value;
	}

	public Expr(Var value) {
		Objects.requireNonNull(value);
		this.value = value;
	}

	public Expr(List<Expr> value) {
		Objects.requireNonNull(value);
		this.value = value;
	}

	public String toLiteral() {
		if (isBigDecimal()) {
			return getAsBigDecimal().toPlainString();
		} else if (isString()) {
			// TODO escape
			return "\"" + getAsString() + "\"";
		} else if (isVar()) {
			return "$" + getAsVar().getName();
		} else if (isList()) {
			return "[" + getAsList().stream().map(Expr::toLiteral).collect(Collectors.joining(" ")) + "]";
		} else {
			throw new IllegalArgumentException("Unknown type: " + value.getClass());
		}
	}

	public boolean isList() {
		return value instanceof List;
	}

	@SuppressWarnings("unchecked")
	public List<Expr> getAsList() {
		if (value instanceof List) {
			return (List<Expr>) value;
		} else {
			throw new RuntimeException(String.format("Expression '%s' of type %s cannot be cast to List", value,
					value.getClass().getSimpleName()));
		}

	}

	public boolean isString() {
		return value instanceof String;
	}

	public String getAsString() {
		if (value instanceof String) {
			return (String) value;
		} else {
			throw new RuntimeException(String.format("Expression '%s' of type %s cannot be cast to String", value,
					value.getClass().getSimpleName()));
		}
	}

	public Object getAsObject() {
		return value;
	}

	public boolean isBoolean() {
		try {
			getAsBoolean();
			return true;
		} catch (final RuntimeException e) {
			return false;
		}
	}

	public boolean getAsBoolean() {
		return parseBoolean(getAsString());
	}

	private static boolean parseBoolean(String value) {
		if (value.equals("true")) {
			return true;
		} else if (value.equals("false")) {
			return false;
		} else {
			throw new RuntimeException(String.format("Expression '%s' of type %s cannot be cast to boolean", value,
					value.getClass().getSimpleName()));
		}
	}

	public boolean isVar() {
		return value instanceof Var;
	}

	public Var getAsVar() {
		if (value instanceof Var) {
			return (Var) value;
		} else {
			throw new RuntimeException(String.format("Expression '%s' of type %s cannot be cast to Var", value,
					value.getClass().getSimpleName()));
		}
	}

	public boolean isInt() {
		try {
			getAsInt();
			return true;
		} catch (final ArithmeticException e) {
			return false;
		}
	}

	public int getAsInt() {
		return getAsBigDecimal().intValueExact();
	}

	public long getAsLong() {
		return getAsBigDecimal().longValueExact();
	}

	public boolean isPrimitive() {
		return isBigDecimal() || isString() || isVar();
	}

	public boolean isBigDecimal() {
		return value instanceof BigDecimal;
	}

	public BigDecimal getAsBigDecimal() {
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value);
		} else {
			throw new RuntimeException(String.format("Expression '%s' of type %s cannot be cast to BigDecimal", value,
					value.getClass().getSimpleName()));
		}
	}

	@Override
	public String toString() {
		return toLiteral();
	}

	public DataType getType() {
		if (isString()) {
			return new DataType(DataTypeCatalog.TYPE_STRING);
		} else if (isBigDecimal()) {
			return new DataType(DataTypeCatalog.TYPE_NUMBER);
		} else {
			return null;
		}
	}
}
