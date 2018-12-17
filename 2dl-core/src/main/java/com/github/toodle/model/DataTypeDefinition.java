package com.github.toodle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataTypeDefinition {
	private final String name;
	private final List<DataTypeParamDefinition> paramTypes = new ArrayList<>();
	private final DataTypeDefinition superType;

	public enum Variance {
		COVARIANT, CONTRAVARIANT
	}

	public DataTypeDefinition(String name, DataTypeDefinition superType,
			Collection<DataTypeParamDefinition> paramTypes) {
		this.name = name;
		this.paramTypes.addAll(paramTypes);
		this.superType = superType;
	}

	public DataTypeDefinition(String name, DataTypeDefinition superType, DataTypeParamDefinition... paramTypes) {
		this(name, superType, Arrays.asList(paramTypes));
	}

	public boolean isTopType() {
		return superType == null;
	}

	public DataTypeDefinition getSuperType() {
		return superType;
	}

	public List<DataTypeParamDefinition> getParamTypes() {
		return paramTypes;
	}

	public DataTypeParamDefinition getParamType(int index) {
		return paramTypes.get(index);
	}

	public String getName() {
		return name;
	}

	/**
	 * @param a
	 * @param b
	 * @return
	 * @see <a href="http://will.thimbleby.net/algorithms/doku.php?id=lowest_common_ancestor">will.thimbleby.net</a>
	 */
	public static DataTypeDefinition lowestCommonAncestor(DataTypeDefinition a, DataTypeDefinition b) {
		// Find all of a's parents.
		final Set<DataTypeDefinition> a_parents = new HashSet<>();
		while (a != null) {
			a_parents.add(a);
			a = a.getSuperType();
		}

		// Find the first intersection with b's parents.
		while (b != null) {
			if (a_parents.contains(b)) return b;
			b = b.getSuperType();
		}

		return null;
	}

}
