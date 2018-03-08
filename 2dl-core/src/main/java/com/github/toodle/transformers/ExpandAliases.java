package com.github.toodle.transformers;

import java.util.Map;
import java.util.Map.Entry;

import com.github.toodle.model.AliasDefinition;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.github.toodle.model.TypeDefinition;

public class ExpandAliases {

	public void execute(Type rootType) {
		expandAliases(rootType, rootType);
	}

	private void expandAliases(Type type, Type scope) {
		// process the current type
		Type alias = null;
		// looping is required in case of an alias referencing another
		while ((alias = getAlias(type.getName(), scope)) != null) {
			type.setName(alias.getName());
			for (final Entry<String, TypeAnnotation> e : alias.getAnnotations().entrySet()) {
				if (type.getAnnotation(e.getKey()) != null) continue;
				type.getAnnotations().put(e.getKey(), e.getValue());
			}
		}

		// recursively process type parameters
		type.getTypeParams().forEach(t -> expandAliases(t, scope));

		// recursively process sub-definitions
		type.getSubDefinitions().stream().map(TypeDefinition::getType).forEach(t -> expandAliases(t, type));
	}

	/**
	 * Return the alias with the specified name, or {@code null} if no such alias exists.
	 */
	private Type getAlias(String aliasName, Type scope) {
		final Map<String, AliasDefinition> aliases = scope.getAliasDefinitionMap();
		final AliasDefinition alias = aliases.get(aliasName);
		if (alias != null) return alias.getValue();
		if (scope.getParent() == null) return null;
		return getAlias(aliasName, scope.getParent());
	}
}
