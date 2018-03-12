package com.github.toodle.transformers;

import java.util.Map;

import com.github.toodle.model.VarDefinition;
import com.github.toodle.model.Expr;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.github.toodle.model.TypeDefinition;

public class ExpandVars {
	public void execute(Type rootType) {
		expandVars(rootType, rootType);
	}

	private void expandVars(Type type, Type scope) {
		// process the current type
		for (final TypeAnnotation ta : type.getAnnotations().values()) {
			expandVars(ta, scope);
		}

		// recursively process type parameters
		type.getTypeParams().forEach(t -> expandVars(t, scope));

		// recursively process sub-definitions
		type.getSubDefinitions().stream().map(TypeDefinition::getType).forEach(t -> expandVars(t, type));
	}

	private void expandVars(TypeAnnotation typeAnnotation, Type scope) {
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
		final Map<String, VarDefinition> constants = scope.getConstDefinitionMap();
		final VarDefinition constant = constants.get(constantName);
		if (constant != null) return constant.getValue();
		if (scope.getParent() == null) return null;
		return getConstant(constantName, scope.getParent());
	}
}
