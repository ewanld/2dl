package com.github.toodle.transformers;

import java.util.Map;

import com.github.toodle.model.ConstDefinition;
import com.github.toodle.model.Expr;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.github.toodle.model.TypeDefinition;

public class ExpandConstants {
	public void execute(Type rootType) {
		expandConstants(rootType, rootType);
	}

	private void expandConstants(Type type, Type scope) {
		// process the current type
		for (final TypeAnnotation ta : type.getAnnotations().values()) {
			expandConstants(ta, scope);
		}

		// recursively process type parameters
		type.getTypeParams().forEach(t -> expandConstants(t, scope));

		// recursively process sub-definitions
		type.getSubDefinitions().stream().map(TypeDefinition::getType).forEach(t -> expandConstants(t, type));
	}

	private void expandConstants(TypeAnnotation typeAnnotation, Type scope) {
		for (int i = 0; i < typeAnnotation.getExprParams().size(); i++) {
			Expr param = typeAnnotation.getExprParams().get(i);
			while (param.isVar()) {
				final Expr expanded = getConstant(param.getAsVar().getName(), scope);
				if (expanded == null) {
					throw new RuntimeException("Unknown constant: " + param.toLiteral());
				}
				param = expanded;
			}
			typeAnnotation.getExprParams_mutable().set(i, param);
			// getConstant()
		}
	}

	/**
	 * Return the alias with the specified name, or {@code null} if no such alias exists.
	 */
	private Expr getConstant(String constantName, Type scope) {
		final Map<String, ConstDefinition> constants = scope.getConstDefinitionMap();
		final ConstDefinition constant = constants.get(constantName);
		if (constant != null) return constant.getValue();
		if (scope.getParent() == null) return null;
		return getConstant(constantName, scope.getParent());
	}
}
