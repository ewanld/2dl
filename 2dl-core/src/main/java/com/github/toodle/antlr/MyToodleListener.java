package com.github.toodle.antlr;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.unbescape.java.JavaEscape;

import com.github.toodle.ToodleLexer;
import com.github.toodle.ToodleListener;
import com.github.toodle.ToodleParser.ConstraintContext;
import com.github.toodle.ToodleParser.ConstraintParamContext;
import com.github.toodle.ToodleParser.DefinitionContext;
import com.github.toodle.ToodleParser.DefinitionsContext;
import com.github.toodle.ToodleParser.StringContext;
import com.github.toodle.ToodleParser.TypeContext;
import com.github.toodle.ToodleParser.TypeParamsContext;
import com.github.toodle.model.Definition;
import com.github.toodle.model.SourceLocation;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;

public class MyToodleListener implements ToodleListener {
	public static final String ROOT_TYPE_NAME = "$root";
	private final Type rootTypeDef = new Type(ROOT_TYPE_NAME, null);
	private Type currentTypeDef = rootTypeDef;
	private TypeAnnotation currentConstraint;
	private final Deque<Scope> scopes = new ArrayDeque<>();

	public enum Scope {
		DEFINITION, TYPE_PARAM
	}

	@Override
	public void visitTerminal(TerminalNode node) {
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		System.out.println(node);
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
	}

	@Override
	public void enterString(StringContext ctx) {
	}

	@Override
	public void exitString(StringContext ctx) {
	}

	@Override
	public void enterDefinition(DefinitionContext ctx) {
	}

	@Override
	public void exitDefinition(DefinitionContext ctx) {
		final List<TerminalNode> tokens = ctx.getTokens(ToodleLexer.IDENT);
		final List<String> modifiers = tokens.subList(0, tokens.size() - 1).stream().map(TerminalNode::getText)
				.collect(Collectors.toList());
		final String name = tokens.get(tokens.size() - 1).getText();

		// definitions may or may not have a type
		if (ctx.getChildCount() > 2) {
			final Type parent = currentTypeDef.getParent();
			final Definition definition = new Definition(name, modifiers, currentTypeDef);
			definition.setLocation(new SourceLocation("", ctx.start.getLine()));
			parent.getChildren().add(definition);
			currentTypeDef = parent;
		}
	}

	private Object fromContext(Object ctx) {
		final Pattern trimMultilineString = Pattern.compile("^[ \t]+\\|", Pattern.MULTILINE);
		if (ctx instanceof StringContext) {
			final StringContext child_ctx = (StringContext) ctx;
			String s = child_ctx.getText();
			if (s.startsWith("\"\"\"")) {
				s = s.substring(3, s.length() - 3);
				s = trimMultilineString.matcher(s).replaceAll("");
				if (s.startsWith("\n")) s = s.substring(1);
				if (s.endsWith("\n")) s = s.substring(0, s.length() - 1);
				s = JavaEscape.unescapeJava(s);
			} else if (s.startsWith("\"")) {
				s = JavaEscape.unescapeJava(s.substring(1, s.length() - 1));
			}
			return s;

		} else if (ctx instanceof ConstraintParamContext) {
			final ParserRuleContext ctx_expr = (ParserRuleContext) ctx;
			final Object child = fromContext(ctx_expr.getChild(0));
			return child;

		} else if (ctx instanceof TerminalNode) {
			final TerminalNode terminalNode = (TerminalNode) ctx;
			if (terminalNode.getSymbol().getType() == ToodleLexer.NUMBER) {
				return new BigDecimal(terminalNode.getText());
			} else {
				throw new RuntimeException("Unexpected token type: " + terminalNode.getSymbol().getType());

			}
		} else {
			throw new RuntimeException("Unknown type: " + ctx.getClass());
		}
	}

	@Override
	public void enterDefinitions(DefinitionsContext ctx) {
		scopes.push(Scope.DEFINITION);
	}

	@Override
	public void exitDefinitions(DefinitionsContext ctx) {
		scopes.pop();
	}

	@Override
	public void enterType(TypeContext ctx) {
		final Type child = new Type(currentTypeDef);
		switch (scopes.getFirst()) {
		case DEFINITION:
			// child is added to its parent later since we don't have the definition name here.
			break;
		case TYPE_PARAM:
			currentTypeDef.getTypeParams().add(child);
			break;
		}
		currentTypeDef = child;
	}

	@Override
	public void exitType(TypeContext ctx) {
		final String category = ctx.getChild(0).getText();
		currentTypeDef.setCategory(category);

		switch (scopes.getFirst()) {
		case DEFINITION:
			// no op
			break;
		case TYPE_PARAM:
			currentTypeDef = currentTypeDef.getParent();
			break;
		}
	}

	@Override
	public void enterConstraint(ConstraintContext ctx) {
		currentConstraint = new TypeAnnotation();
	}

	@Override
	public void exitConstraint(ConstraintContext ctx) {
		final String name = ctx.getChild(0).getText();
		currentConstraint.setName(name);
		currentTypeDef.getAnnotations().put(name, currentConstraint);
	}

	@Override
	public void enterConstraintParam(ConstraintParamContext ctx) {
	}

	@Override
	public void exitConstraintParam(ConstraintParamContext ctx) {
		currentConstraint.getObjectParams().add(fromContext(ctx));
	}

	public Type getRootType() {
		return rootTypeDef;
	}

	@Override
	public void enterTypeParams(TypeParamsContext ctx) {
		scopes.push(Scope.TYPE_PARAM);
	}

	@Override
	public void exitTypeParams(TypeParamsContext ctx) {
		scopes.pop();
	}

}
