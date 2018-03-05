package com.github.toodle.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.toodle.model.TypeDefinition;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.github.toodle.services.ToodleVisitorWithContext;
import com.github.visitorj.VisitResult;

public class ToodleSchema extends ToodleVisitorWithContext {
	private final Map<String, Type> typeSchemas;
	private final Type schemaForUnknownType;
	private final Set<String> allowedGlobalModifiers;
	private final List<String> violations = new ArrayList<>();

	public ToodleSchema(Type schemaRootType) {
		final Collection<TypeDefinition> schemaDefinitions = schemaRootType.getSubDefinitions();
		this.typeSchemas = schemaDefinitions.stream().filter(d -> d.getType().getName().equals("type"))
				.collect(Collectors.toMap(TypeDefinition::getName, TypeDefinition::getType));
		this.allowedGlobalModifiers = schemaDefinitions.stream().filter(d -> d.getType().getName().equals("modifier"))
				.map(d -> d.getName()).collect(Collectors.toSet());
		schemaForUnknownType = this.typeSchemas.get("*");
	}

	public boolean validate(Type rootType) {
		final Collection<TypeDefinition> definitions = rootType.getSubDefinitions();
		definitions.forEach(d -> d.accept(this));
		return violations.isEmpty();
	}

	@Override
	protected VisitResult onVisit(TypeAnnotation constraint, String identifier) {
		return VisitResult.CONTINUE;
	}

	@Override
	protected VisitResult onVisit(Type type, String identifier) {
		final String typeName = type.getName();
		final TypeDefinition definition = context.getClosest(TypeDefinition.class);

		final Type typeSchema = getSchema(typeName);
		if (typeSchema == null) {
			error(definition, "Unknown type: %s", typeName);
			return VisitResult.CONTINUE;
		}

		validateAbstractModifier(definition, type, typeSchema);
		validateTypeParamCount(definition, type, typeSchema);
		validateCompositeAnnotation(definition, type, typeSchema);
		validateTypeAnnotations(definition, type, typeSchema);

		return VisitResult.CONTINUE;
	}

	private Type getSchema(String typeName) {
		Type typeSchema = typeSchemas.get(typeName);
		if (typeSchema == null) typeSchema = schemaForUnknownType;
		return typeSchema;
	}

	private void validateAbstractModifier(TypeDefinition definition, Type type, Type typeSchema) {
		if (typeSchema.getAnnotation("abstract") != null) error(definition,
				"cannot be defined of type '%s' because '%s' is abstract.", type.getName(), type.getName());
	}

	private void validateTypeAnnotations(final TypeDefinition definition, Type type, Type typeSchema) {
		final Map<String, TypeDefinition> allowedAnnotations = getAllowedAnnotations(typeSchema);
		// validate that all required type annotations are present
		final List<String> requiredAnnotations = allowedAnnotations.values().stream()
				.filter(d -> d.getType().getAnnotation("required") != null).map(TypeDefinition::getName)
				.collect(Collectors.toList());
		final Set<String> actualAnnotations = type.getAnnotations().keySet();
		for (final String requiredAnnotation : requiredAnnotations) {
			if (!actualAnnotations.contains(requiredAnnotation)) {
				error(definition, "a required annotation '%s' is missing", requiredAnnotation);
			}
		}

		// validate type annotations
		for (final TypeAnnotation annotation : type.getAnnotations().values()) {
			final TypeDefinition annotationSchema_def = allowedAnnotations.get(annotation.getName());
			// validate that the annotation is allowed
			if (annotationSchema_def == null) {
				error(definition, "the annotation '%s' is not allowed", annotation.getName());
				continue;
			}
			final Type annotationSchema = annotationSchema_def.getType();
			final Type annotationParametersType = annotationSchema.getTypeParams().get(0);

			// validate annotation parameters count
			if (annotationParametersType.getName().equals("bool")) {
				validateParamCount(definition, annotation, 0, 1);
			} else if (!annotationParametersType.getName().equals("variadic")) {
				validateParamCount(definition, annotation, 1);
			}

			// validate annotation parameters type
			validateParamType(definition, annotation, annotationParametersType);
		}
	}

	private void validateTypeParamCount(final TypeDefinition definition, Type type, Type typeSchema) {
		final TypeAnnotation typeParamCount_a = typeSchema.getAnnotation("typeParamCount");
		final TypeAnnotation minTypeParamCount_a = typeSchema.getAnnotation("minTypeParamCount");
		final TypeAnnotation maxTypeParamCount_a = typeSchema.getAnnotation("maxTypeParamCount");

		final Integer typeParamCount = typeParamCount_a != null ? typeParamCount_a.getIntParams().get(0) : null;
		// @formatter:off
		final int minTypeParamCount =
				minTypeParamCount_a != null
				? minTypeParamCount_a.getIntParams().get(0)
				: typeParamCount != null ? typeParamCount : 0;
		final int maxTypeParamCount =
				maxTypeParamCount_a != null
				? (maxTypeParamCount_a.getIntParams().get(0) < 0 ? Integer.MAX_VALUE : maxTypeParamCount_a.getIntParams().get(0))
				: typeParamCount != null ? typeParamCount : 0;
		// @formatter:on

		final int typeParamCount_actual = type.getTypeParams().size();
		if (typeParamCount != null && typeParamCount != typeParamCount_actual) {
			error(definition, "expected %s type parameters, got %s", typeParamCount, typeParamCount_actual);
		}
		if (typeParamCount_actual < minTypeParamCount) {
			error(definition, "expected at least %s type parameters, got %s", minTypeParamCount,
					typeParamCount_actual);
		}
		if (typeParamCount_actual > maxTypeParamCount) {
			error(definition, "expected at most %s type parameters, got %s", maxTypeParamCount,
					typeParamCount_actual);
		}
	}

	private void validateCompositeAnnotation(final TypeDefinition definition, Type type, Type typeSchema) {
		final TypeAnnotation composite_a = typeSchema.getAnnotation("composite");
		final boolean composite = composite_a != null;

		// validate that !composite imply no sub-definitions
		final boolean composite_actual = !type.getSubDefinitions().isEmpty();
		if (!composite && composite_actual) {
			error(definition, "no subdefinitions expected");
		}

		if (composite) {
			// validate sub-definition allowed types
			final List<String> allowedSubTypes = composite_a.getStringParams();
			if (!allowedSubTypes.isEmpty()) {
				for (final TypeDefinition d : type.getSubDefinitions()) {
					if (!isSubstitute(d.getType().getName(), allowedSubTypes))
						error(d, "type is %s, allowed types in this context are: %s", d.getType().getName(),
								allowedSubTypes.stream().collect(Collectors.joining(", ")));
				}
			}
		}
	}

	private void error(TypeDefinition definition, String message, Object... args) {
		violations.add("Line " + definition.getLocation().getLine() + ": " + definition.getName() + ": "
				+ String.format(message, args));
	}

	/**
	 * Returns whether {@code typeName} is either equal to {@code expectedTypeName}, or is a subtype of it.
	 */
	public boolean isSubstitute(String typeName, String expectedTypeName) {
		if (expectedTypeName.equals(typeName)) return true;
		final Type typeSchema = getSchema(typeName);
		if (typeSchema == null) return false;
		final String typeSchema_parent = getSupertypeName(typeSchema);
		return typeSchema_parent == null ? false : isSubstitute(typeSchema_parent, expectedTypeName);
	}

	/**
	 * Returns whether {@code typeName} is a valid substitute for any of the {@code expectedTypeNames}.
	 */
	public boolean isSubstitute(String typeName, Collection<String> expectedTypeNames) {
		return expectedTypeNames.stream().anyMatch(et -> isSubstitute(typeName, et));
	}

	public String getSupertypeName(Type typeSchema) {
		final TypeAnnotation extends_a = typeSchema.getAnnotation("extends");
		if (extends_a == null) return null;
		final String supertypeName = extends_a.getStringParams().get(0);
		return supertypeName;
	}

	public Type getSupertype(Type typeSchema) {
		final String supertypeName = getSupertypeName(typeSchema);
		final Type supertype = typeSchemas.get(supertypeName);
		return supertype;
	}

	public Map<String, TypeDefinition> getAllowedAnnotations(Type typeSchema) {
		final Map<String, TypeDefinition> res = new HashMap<>(typeSchema.getSubDefinitionsOfType("annotation"));
		final String supertypeName = getSupertypeName(typeSchema);
		final Type supertype = typeSchemas.get(supertypeName);
		if (supertype != null) {
			res.putAll(getAllowedAnnotations(supertype));
		}
		return res;
	}

	public Set<String> getAllowedModifiers(final Type parentSchema) {
		if (parentSchema == null) return allowedGlobalModifiers;
		final HashSet<String> res = new HashSet<>(parentSchema.getSubDefinitionsOfType("modifier").keySet());
		final String supertypeName = getSupertypeName(parentSchema);
		final Type supertype = typeSchemas.get(supertypeName);
		if (supertype != null) {
			res.addAll(getAllowedModifiers(supertype));
		}
		return res;
	}

	private void validateParamCount(TypeDefinition definition, TypeAnnotation annotation, int expectedParamCount) {
		final int paramCount = annotation.getObjectParams().size();
		if (paramCount != expectedParamCount) {
			error(definition, "annotation %s: expected %s parameters, got %s", annotation.getName(),
					expectedParamCount, paramCount);
		}
	}

	private void validateParamCount(TypeDefinition definition, TypeAnnotation annotation, int minParamCount,
			int maxParamCount) {
		final int paramCount = annotation.getObjectParams().size();
		if (paramCount < minParamCount || paramCount > maxParamCount) {
			error(definition, "annotation %s: expected between %s and %s parameters, got %s", annotation.getName(),
					minParamCount, maxParamCount, paramCount);
		}
	}

	private void validateParamType(TypeDefinition definition, TypeAnnotation annotation, Type expectedType) {
		if (expectedType.getName().equals("primitive")) {
			// no op
		} else if (expectedType.getName().equals("bool")) {
			final List<String> params = annotation.getStringParams();
			// if params is empty, we assume a 'true' value
			for (final String param : params) {
				if (!param.equals("true") && !param.equals("false")) {
					error(definition, "%s, annotation %s: was expecting 'true' or 'false', got '%s'",
							annotation.getName(), param);
				}
			}
		} else if (expectedType.getName().equals("string")) {
			annotation.getStringParams();
		} else if (expectedType.getName().equals("number")) {
			annotation.getBigDecimalParams();
		} else if (expectedType.getName().equals("int")) {
			annotation.getIntParams();
		} else if (expectedType.getName().equals("enum")) {
			final Set<String> enumValues_allowed = new HashSet<>(expectedType.getAnnotation("of").getStringParams());
			final List<String> enumValues_actual = annotation.getStringParams();
			for (final String value : enumValues_actual) {
				if (!enumValues_allowed.contains(value)) error(definition,
						"%s, annotation %s: invalid enum value '%s'. Must be one of: %s", annotation.getName(), value,
						enumValues_allowed.stream().collect(Collectors.joining(", ")));
			}
		} else if (expectedType.getName().equals("variadic")) {
			validateParamType(definition, annotation, expectedType.getTypeParams().get(0));
		} else {
			error(definition, "annotation %s: invalid type for parameters: %s", annotation.getName(),
					expectedType.getName());
		}
	}

	@Override
	protected VisitResult onVisit(TypeDefinition definition, String identifier) {
		final Type parentType = context.getClosest(Type.class);
		final Set<String> allowedModifiers;
		Type parentSchema = null;
		if (parentType == null) {
			parentSchema = null;
		} else {
			parentSchema = typeSchemas.get(parentType.getName());
			if (parentSchema == null) {
				error(definition, "Unknown type: %s", parentType.getName());
				return VisitResult.SKIP_CHILDREN;
			}
		}
		allowedModifiers = getAllowedModifiers(parentSchema);
		for (final String modifier : definition.getModifiers()) {
			if (!allowedModifiers.contains(modifier)) error(definition, "Invalid modifier: %s", modifier);
		}
		return VisitResult.CONTINUE;
	}

	public List<String> getViolations() {
		return violations;
	}

}
