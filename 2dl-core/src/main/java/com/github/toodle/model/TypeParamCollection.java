package com.github.toodle.model;

import java.util.Collection;

import com.github.toodle.services.ToodleVisitor;
import com.github.visitorj.VisitEvent;
import com.github.visitorj.VisitResult;
import com.github.visitorj.VisitableList;
import com.github.visitorj.CompositeVisitable;

public class TypeParamCollection extends CompositeVisitable<ToodleVisitor> {

	public TypeParamCollection(Collection<Type> typeParams) {
		super(new VisitableList<>(typeParams));
	}

	@Override
	public void event(VisitEvent event, ToodleVisitor visitor) {
		visitor.event(event, this);
	}

	@Override
	public VisitResult visit(ToodleVisitor visitor, String identifier) {
		return visitor.visit(this, identifier);
	}

}
