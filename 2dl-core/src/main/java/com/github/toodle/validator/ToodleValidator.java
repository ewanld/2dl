package com.github.toodle.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.toodle.model.Definition;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.github.toodle.services.ToodleVisitorWithContext;
import com.github.visitorj.VisitResult;

public class ToodleValidator extends ToodleVisitorWithContext {
	private final Map<String, Type> typeSchemas;
	private final Type schemaForUnknownType;
	private final Set<String> allowedGlobalModifiers;
	private final List<String> violations = new ArrayList<>();

	public ToodleValidator(Collection<Definition> schemaDefinitions) {
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
		final String defName = context.getClosest(Definition.class).getName();

		Type typeSchema = typeSchemas.get(typeName);
		if (typeSchema == null) typeSchema = schemaForUnknownType;
		if (typeSchema == null) {
			error("Unknown type: " + typeName);
			return VisitResult.CONTINUE;
		}
		final Map<String, Definition> allowedAnnotations = getAllowedAnnotations(typeSchema);

		// validate type params count
		{
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

		// validate 'composite' annotation
		{
			final boolean composite = typeSchema.getAnnotation("composite") != null;

			final boolean composite_actual = !type.getChildren().isEmpty();
			if (!composite && composite_actual) {
				error("%s: no subdefinitions expected", defName);
			}
		}

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
			if (annotationParametersType.getName().equals("empty")) {
				validateParamCount(defName, annotation, 0);
			} else if (!annotationParametersType.getName().equals("variadic")) {
				validateParamCount(defName, annotation, 1);
			}

			// validate annotation parameters type
			if (annotationParametersType.getName().equals("any")) {
				// no op
			} else if (annotationParametersType.getName().equals("empty")) {
				annotation.getStringParams();
			} else if (annotationParametersType.getName().equals("string")) {
				annotation.getStringParams();
			} else if (annotationParametersType.getName().equals("number")) {
				annotation.getBigDecimalParams();
			} else if (annotationParametersType.getName().equals("int")) {
				annotation.getIntParams();
			} else if (annotationParametersType.getName().equals("variadic")) {
				validateParamType(typeName, annotation, annotationParametersType);
			} else {
				error("%s, annotation %s: invalid type for parameters: %", defName, annotation.getName(),
						annotationParametersType.getName());
			}
		}

		return VisitResult.CONTINUE;
	}

	private void error(String message, Object... args) {
		violations.add(String.format(message, args));
	}

	private Type getBaseType(Type typeSchema) {
		final TypeAnnotation extends_a = typeSchema.getAnnotation("extends");
		final Type baseType = extends_a == null ? null : typeSchemas.get(extends_a.getStringParams().get(0));
		if (extends_a != null && baseType == null) error("Unknown type: %s", extends_a.getStringParams().get(0));
		return baseType;
	}

	private Map<String, Definition> getAllowedAnnotations(Type typeSchema) {
		final Map<String, Definition> res = new HashMap<>(typeSchema.getChildrenOfType("annotation"));
		final Type baseType = getBaseType(typeSchema);
		if (baseType != null) {
			res.putAll(getAllowedAnnotations(baseType));
		}
		return res;
	}

	private void validateParamCount(String typeName, TypeAnnotation annotation, int expectedParameterCount) {
		if (annotation.getObjectParams().size() != expectedParameterCount) {
			error("%s, annotation %s: expected 1 parameter, got %s", typeName, annotation.getName(),
					annotation.getObjectParams().size());
		}
	}

	private void validateParamType(String typeName, TypeAnnotation annotation, Type expectedType) {
		if (expectedType.getName().equals("any")) {
			// no op
		} else if (expectedType.getName().equals("empty")) {
			annotation.getStringParams();
		} else if (expectedType.getName().equals("string")) {
			annotation.getStringParams();
		} else if (expectedType.getName().equals("number")) {
			annotation.getBigDecimalParams();
		} else if (expectedType.getName().equals("int")) {
			annotation.getIntParams();
		} else if (expectedType.getName().equals("variadic")) {
			validateParamType(typeName, annotation, expectedType.getTypeParams().get(0));
		} else {
			error("%s, annotation %s: invalid type for parameters: %", typeName, annotation.getName(),
					expectedType.getName());
		}
	}

	@Override
	protected VisitResult onVisit(Definition definition, String identifier) {
		final Type parentType = context.getClosest(Type.class);
		final Set<String> allowedModifiers;
		if (parentType == null) {
			allowedModifiers = allowedGlobalModifiers;
		} else {
			final Type parentSchema = typeSchemas.get(parentType.getName());
			if (parentSchema == null) {
				error("Unknown type: %s", parentType.getName());
				return VisitResult.SKIP_CHILDREN;
			}
			allowedModifiers = parentSchema.getChildrenOfType("modifier").keySet();
		}
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