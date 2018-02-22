package com.github.toodle.services;

import java.util.Collection;
import java.util.Map;

import com.github.toodle.model.Definition;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ToodleToJsonConverter {

	public JsonElement toJson(Collection<Definition> definitions) {
		final JsonArray res = new JsonArray();
		definitions.forEach(d -> res.add(toJson(d)));
		return res;
	}

	public JsonElement toJson(Definition definition) {
		final JsonObject res = new JsonObject();
		res.add("name", new JsonPrimitive(definition.getName()));
		if (!definition.getModifiers().isEmpty()) res.add("modifiers", stringsToJson(definition.getModifiers()));
		res.add("type", toJson(definition.getType()));
		return res;
	}

	public JsonElement toJson(Type type) {
		final JsonObject res = new JsonObject();
		res.add("name", new JsonPrimitive(type.getName()));
		if (!type.getTypeParams().isEmpty()) res.add("typeParams", typesToJson(type.getTypeParams()));
		if (!type.getChildren().isEmpty()) res.add("subDefinitions", toJson(type.getChildren()));
		if (!type.getAnnotations().isEmpty()) res.add("annotations", annotationsToJson(type.getAnnotations()));
		return res;
	}

	private JsonElement annotationsToJson(Map<String, TypeAnnotation> annotations) {
		final JsonObject res = new JsonObject();
		annotations.entrySet().forEach(e -> res.add(e.getKey(), toJson(e.getValue())));
		return res;
	}

	public JsonElement toJson(TypeAnnotation annotation) {
		final JsonArray res = new JsonArray();
		for (final Object o : annotation.getObjectParams()) {
			if (o instanceof Number) {
				res.add((Number) o);
			} else if (o instanceof String) {
				res.add((String) o);
			} else {
				assert false;
			}
		}
		return res;
	}

	public JsonElement typesToJson(Collection<Type> types) {
		final JsonArray res = new JsonArray();
		types.forEach(t -> res.add(toJson(t)));
		return res;
	}

	public JsonElement stringsToJson(Collection<String> strings) {
		final JsonArray res = new JsonArray();
		strings.forEach(s -> res.add(s));
		return res;
	}

}
