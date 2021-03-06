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
				final Expr expanded = getVar(param.getAsVar().getName(), scope);
				if (expanded == null) {
					throw new RuntimeException("Unknown constant: " + param.toLiteral());
				}
				param = expanded;
			}
			typeAnnotation.getExprParams_mutable().set(i, param);
		}
	}

	/**
	 * Return the alias with the specified name, or {@code null} if no such alias exists.
	 */
	private Expr getVar(String varName, Type scope) {
		final Map<String, VarDefinition> vars = scope.getVarDefinitionMap();
		final VarDefinition var = vars.get(varName);
		if (var != null) return var.getValue();
		if (scope.getParent() == null) return null;
		return getVar(varName, scope.getParent());
	}
}
