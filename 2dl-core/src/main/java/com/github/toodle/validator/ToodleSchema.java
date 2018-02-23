package com.github.toodle.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.toodle.model.Definition;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.github.toodle.services.ToodleVisitorWithContext;
import com.github.visitorj.VisitResult;

public class ToodleSchema extends ToodleVisitorWithContext {
	private final Map<String, Type> typeSchemas;
	private final Type schemaForUnknownType;
	private final Set<String> allowedGlobalModifiers;
	private final List<String> violations = new ArrayList<>();

	public ToodleSchema(Collection<Definition> schemaDefinitions) {
		this.typeSchemas = schemaDefinitions.stream().filter(d -> d.getType().getName().equals("type"))
				.collect(Collectors.toMap(Definition::getName, Definition::getType));
		this.allowedGlobalModifiers = schemaDefinitions.stream().filter(d -> d.getType().getName().equals("modifier"))
				.map(d -> d.getName()).collect(Collectors.toSet());
		schemaForUnknownType = this.typeSchemas.get("*");
	}

	public boolean validate(Collection<Definition> definitions) {
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
		final Definition definition = context.getClosest(Definition.class);
		final String defName = definition.getName();

		final Type typeSchema = getSchema(typeName);
		if (typeSchema == null) {
			error("Unknown type: " + typeName);
			return VisitResult.CONTINUE;
		}

		validateAbstractModifier(defName, type, typeSchema);
		validateTypeParamCount(defName, type, typeSchema);
		validateCompositeAnnotation(defName, type, typeSchema);
		validateTypeAnnotations(defName, type, typeSchema);

		return VisitResult.CONTINUE;
	}

	private Type getSchema(String typeName) {
		Type typeSchema = typeSchemas.get(typeName);
		if (typeSchema == null) typeSchema = schemaForUnknownType;
		return typeSchema;
	}

	private void validateAbstractModifier(String defName, Type type, Type typeSchema) {
		if (typeSchema.getAnnotation("abstract") != null)
			error("%s: cannot be defined of type '%s' because '%s' is abstract.", defName, type.getName(),
					type.getName());
	}

	private void validateTypeAnnotations(final String defName, Type type, Type typeSchema) {
		final Map<String, Definition> allowedAnnotations = getAllowedAnnotations(typeSchema);
		// validate that all required type annotations are present
		final List<String> requiredAnnotations = allowedAnnotations.values().stream()
				.filter(d -> d.getType().getAnnotation("required") != null).map(Definition::getName)
				.collect(Collectors.toList());
		final Set<String> actualAnnotations = type.getAnnotations().keySet();
		for (final String requiredAnnotation : requiredAnnotations) {
			if (!actualAnnotations.contains(requiredAnnotation)) {
				error("%s: a required annotation '%s' is missing", defName, requiredAnnotation);
			}
		}

		// validate type annotations
		for (final TypeAnnotation annotation : type.getAnnotations().values()) {
			final Definition annotationSchema_def = allowedAnnotations.get(annotation.getName());
			// validate that the annotation is allowed
			if (annotationSchema_def == null) {
				error("%s: the annotation '%s' is not allowed", defName, annotation.getName());
				continue;
			}
			final Type annotationSchema = annotationSchema_def.getType();
			final Type annotationParametersType = annotationSchema.getTypeParams().get(0);

			// validate annotation parameters count
			if (annotationParametersType.getName().equals("bool")) {
				validateParamCount(defName, annotation, 0, 1);
			} else if (!annotationParametersType.getName().equals("variadic")) {
				validateParamCount(defName, annotation, 1);
			}

			// validate annotation parameters type
			validateParamType(defName, annotation, annotationParametersType);
		}
	}

	private void validateTypeParamCount(final String defName, Type type, Type typeSchema) {
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
			error("%s: expected %s type parameters, got %s", defName, typeParamCount, typeParamCount_actual);
		}
		if (typeParamCount_actual < minTypeParamCount) {
			error("%s: expected at least %s type parameters, got %s", defName, minTypeParamCount,
					typeParamCount_actual);
		}
		if (typeParamCount_actual > maxTypeParamCount) {
			error("%s: expected at most %s type parameters, got %s", defName, maxTypeParamCount,
					typeParamCount_actual);
		}
	}

	private void validateCompositeAnnotation(final String defName, Type type, Type typeSchema) {
		final TypeAnnotation composite_a = typeSchema.getAnnotation("composite");
		final boolean composite = composite_a != null;

		// validate that !composite imply no sub-definitions
		final boolean composite_actual = !type.getChildren().isEmpty();
		if (!composite && composite_actual) {
			error("%s: no subdefinitions expected", defName);
		}

		if (composite) {
			// validate sub-definition allowed types
			final List<String> allowedSubTypes = composite_a.getStringParams();
			if (!allowedSubTypes.isEmpty()) {
				for (final Definition d : type.getChildren()) {
					if (!isOfAnyType(d.getType().getName(), allowedSubTypes))
						error("%s: type is %s, allowed types in this context are: %s", d.getName(),
								d.getType().getName(), allowedSubTypes.stream().collect(Collectors.joining(", ")));
				}
			}
		}
	}

	private void error(String message, Object... args) {
		violations.add(String.format(message, args));
	}

	public boolean isOfType(String typeName, String expectedTypeName) {
		if (expectedTypeName.equals(typeName)) return true;
		final Type typeSchema = getSchema(typeName);
		if (typeSchema == null) return false;
		final String typeSchema_parent = getBaseTypeName(typeSchema);
		return typeSchema_parent == null ? false : isOfType(typeSchema_parent, expectedTypeName);
	}

	public boolean isOfAnyType(String typeName, Collection<String> expectedTypeNames) {
		return expectedTypeNames.stream().anyMatch(et -> isOfType(typeName, et));
	}

	public String getBaseTypeName(Type typeSchema) {
		final TypeAnnotation extends_a = typeSchema.getAnnotation("extends");
		if (extends_a == null) return null;
		final String baseTypeName = extends_a.getStringParams().get(0);
		return baseTypeName;
	}

	public Type getBaseType(Type typeSchema) {
		final String baseTypeName = getBaseTypeName(typeSchema);
		final Type baseType = typeSchemas.get(baseTypeName);
		return baseType;
	}

	public Map<String, Definition> getAllowedAnnotations(Type typeSchema) {
		final Map<String, Definition> res = new HashMap<>(typeSchema.getChildrenOfType("annotation"));
		final Type baseType = getBaseType(typeSchema);
		if (baseType != null) {
			res.putAll(getAllowedAnnotations(baseType));
		}
		return res;
	}

	public Set<String> getAllowedModifiers(final Type parentSchema) {
		if (parentSchema == null) return allowedGlobalModifiers;
		final HashSet<String> res = new HashSet<>(parentSchema.getChildrenOfType("modifier").keySet());
		final Type baseType = getBaseType(parentSchema);
		if (baseType != null) {
			res.addAll(getAllowedModifiers(baseType));
		}
		return res;
	}

	private void validateParamCount(String defName, TypeAnnotation annotation, int expectedParamCount) {
		final int paramCount = annotation.getObjectParams().size();
		if (paramCount != expectedParamCount) {
			error("%s, annotation %s: expected %s parameters, got %s", defName, annotation.getName(),
					expectedParamCount, paramCount);
		}
	}

	private void validateParamCount(String defName, TypeAnnotation annotation, int minParamCount, int maxParamCount) {
		final int paramCount = annotation.getObjectParams().size();
		if (paramCount < minParamCount || paramCount > maxParamCount) {
			error("%s, annotation %s: expected between %s and %s parameters, got %s", defName, annotation.getName(),
					minParamCount, maxParamCount, paramCount);
		}
	}

	private void validateParamType(String defName, TypeAnnotation annotation, Type expectedType) {
		if (expectedType.getName().equals("any")) {
			// no op
		} else if (expectedType.getName().equals("bool")) {
			final List<String> params = annotation.getStringParams();
			// if params is empty, we assume a 'true' value
			for (final String param : params) {
				if (!param.equals("true") && !param.equals("false")) {
					error("%s, annotation %s: was expecting 'true' or 'false', got '%s'", defName,
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
				if (!enumValues_allowed.contains(value))
					error("%s, annotation %s: invalid enum value '%s'. Must be one of: %s", defName,
							annotation.getName(), value,
							enumValues_allowed.stream().collect(Collectors.joining(", ")));
			}
		} else if (expectedType.getName().equals("variadic")) {
			validateParamType(defName, annotation, expectedType.getTypeParams().get(0));
		} else {
			error("%s, annotation %s: invalid type for parameters: %s", defName, annotation.getName(),
					expectedType.getName());
		}
	}

	@Override
	protected VisitResult onVisit(Definition definition, String identifier) {
		final Type parentType = context.getClosest(Type.class);
		final Set<String> allowedModifiers;
		Type parentSchema = null;
		if (parentType == null) {
			parentSchema = null;
		} else {
			parentSchema = typeSchemas.get(parentType.getName());
			if (parentSchema == null) {
				error("Unknown type: %s", parentType.getName());
				return VisitResult.SKIP_CHILDREN;
			}
		}
		allowedModifiers = getAllowedModifiers(parentSchema);
		for (final String modifier : definition.getModifiers()) {
			if (!allowedModifiers.contains(modifier))
				error("%s: Invalid modifier: %s", definition.getName(), modifier);
		}
		return VisitResult.CONTINUE;
	}

	public List<String> getViolations() {
		return violations;
	}

}
