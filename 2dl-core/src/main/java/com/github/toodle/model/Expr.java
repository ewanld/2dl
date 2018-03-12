package com.github.toodle.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Expr {
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

	public String toLiteral() {
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toPlainString();
		} else if (value instanceof String) {
			// TODO escape
			return "\"" + (String) value + "\"";
		} else if (value instanceof Var) {
			return "$" + getAsVar().getName();
		} else {
			throw new IllegalArgumentException("Unknown type: " + value.getClass());
		}
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

	public int getAsInt() {
		return getAsBigDecimal().intValueExact();
	}

	public long getAsLong() {
		return getAsBigDecimal().longValueExact();
	}

	public BigDecimal getAsBigDecimal() {
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value);
		} else {
			throw new RuntimeException(String.format("Expression '%s' of type %s cannot be cast to BigDecimal", value,
					value.getClass().getSimpleName()));
		}
	}
}
