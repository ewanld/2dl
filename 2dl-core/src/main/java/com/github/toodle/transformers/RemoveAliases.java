package com.github.toodle.transformers;

import com.github.toodle.model.Type;
import com.github.toodle.services.SimpleToodleVisitor;
import com.github.visitorj.VisitResult;

public class RemoveAliases extends SimpleToodleVisitor {
	private static final RemoveAliasesVisitor visitor = new RemoveAliasesVisitor();

	public void execute(Type rootType) {
		rootType.accept(visitor);
	}

	private static class RemoveAliasesVisitor extends SimpleToodleVisitor {

		@Override
		public VisitResult visit(Type type, String identifier) {
			type.getAliasDefinitions().clear();
			return VisitResult.CONTINUE;
		}
	}
}
