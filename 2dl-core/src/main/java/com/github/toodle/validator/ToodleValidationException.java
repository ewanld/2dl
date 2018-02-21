package com.github.toodle.validator;

import java.util.List;

public class ToodleValidationException extends RuntimeException {
	private static final long serialVersionUID = -2937421941143483L;
	private final List<String> violations;

	public ToodleValidationException(List<String> violations) {
		super(getMessage(violations));
		this.violations = violations;
	}

	private static String getMessage(List<String> violations) {
		final StringBuilder sb = new StringBuilder("2dl schema validation failed. Violations were found:\n");
		violations.forEach(v -> sb.append("  - ").append(v).append("\n"));
		return sb.toString();
	}

	public List<String> getViolations() {
		return violations;
	}
}
