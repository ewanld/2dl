package com.github.toodle.antlr;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.unbescape.java.JavaEscape;

import com.github.toodle.ToodleLexer;
import com.github.toodle.ToodleListener;
import com.github.toodle.ToodleParser.Alias_definitionContext;
import com.github.toodle.ToodleParser.AnnotationContext;
import com.github.toodle.ToodleParser.ArrayContext;
import com.github.toodle.ToodleParser.Let_definitionContext;
import com.github.toodle.ToodleParser.DefinitionContext;
import com.github.toodle.ToodleParser.DefinitionsContext;
import com.github.toodle.ToodleParser.ExprContext;
import com.github.toodle.ToodleParser.StatementContext;
import com.github.toodle.ToodleParser.StringContext;
import com.github.toodle.ToodleParser.TypeContext;
import com.github.toodle.ToodleParser.TypeParamsContext;
import com.github.toodle.model.Expr;
import com.github.toodle.model.SourceLocation;
import com.github.toodle.model.Type;
import com.github.toodle.model.TypeAnnotation;
import com.github.toodle.model.TypeDefinition;
import com.github.toodle.model.Var;

public class MyToodleListener implements ToodleListener {
	public static final String ROOT_TYPE_NAME = "$root";
	private final Type rootType = new Type(ROOT_TYPE_NAME, null);
	private Type currentType = rootType;
	private TypeAnnotation currentTypeAnnotation;
	private Expr currentVarValue;
	private final Deque<Scope> scopes = new ArrayDeque<>();
	private static final String variablePrefix = "$";

	public enum Scope {
		TYPE_DEFINITION, TYPE_PARAM, ALIAS_DEFINITION, VAR_DEFINITION
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
			final Type parent = currentType.getParent();
			if (parent == null) {
				System.out.println("null");
			}

			final TypeDefinition definition = new TypeDefinition(name, modifiers, currentType);
			definition.setLocation(new SourceLocation("", ctx.start.getLine()));
			parent.getSubDefinitions().add(definition);
			currentType = parent;
		}
	}

	private Expr ctxToExpr(Object ctx) {
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
			return new Expr(s);

		} else if (ctx instanceof ExprContext) {
			final ExprContext ctx_expr = (ExprContext) ctx;
			final Expr child = ctxToExpr(ctx_expr.getChild(0));
			return child;

		} else if (ctx instanceof ArrayContext) {
			final ArrayContext ctx_array = (ArrayContext) ctx;
			final List<Expr> children = new ArrayList<>();
			// ignore 1st and last token ('[' and ']')
			for (int i = 1; i < ctx_array.getChildCount() - 1; i++) {
				final ParseTree child = ctx_array.getChild(i);
				children.add(ctxToExpr(child));
			}
			return new Expr(children);

		} else if (ctx instanceof TerminalNode) {
			final TerminalNode terminalNode = (TerminalNode) ctx;
			final int terminalType = terminalNode.getSymbol().getType();
			if (terminalType == ToodleLexer.NUMBER) {
				return new Expr(new BigDecimal(terminalNode.getText()));
			} else if (terminalType == ToodleLexer.VARIABLE) {
				return new Expr(new Var(terminalNode.getText().substring(variablePrefix.length())));
			} else {
				throw new RuntimeException("Unexpected token: " + terminalNode.getSymbol().getText());
			}

		} else {
			throw new RuntimeException("Unknown type: " + ctx.getClass());
		}
	}

	@Override
	public void enterDefinitions(DefinitionsContext ctx) {
		scopes.push(Scope.TYPE_DEFINITION);
	}

	@Override
	public void exitDefinitions(DefinitionsContext ctx) {
		final Scope popped = scopes.pop();
		assert popped == Scope.TYPE_DEFINITION;
	}

	@Override
	public void enterType(TypeContext ctx) {
		final Type child = new Type(currentType);
		if (scopes.peek() == Scope.TYPE_PARAM) {
			currentType.getTypeParams().add(child);
		}
		currentType = child;
	}

	@Override
	public void exitType(TypeContext ctx) {
		final String name = ctx.getChild(0).getText();
		currentType.setName(name);

		if (scopes.peek() == Scope.TYPE_PARAM) {
			currentType = currentType.getParent();
		}
	}

	@Override
	public void enterAnnotation(AnnotationContext ctx) {
		currentTypeAnnotation = new TypeAnnotation();
	}

	@Override
	public void exitAnnotation(AnnotationContext ctx) {
		final String name = ctx.getChild(0).getText();
		currentTypeAnnotation.setName(name);
		currentType.getAnnotations().put(name, currentTypeAnnotation);
	}

	@Override
	public void enterExpr(ExprContext ctx) {
	}

	@Override
	public void exitExpr(ExprContext ctx) {
		if (scopes.peek() == Scope.VAR_DEFINITION) {
			currentVarValue = ctxToExpr(ctx);
		} else {
			currentTypeAnnotation.getExprParams_mutable().add(ctxToExpr(ctx));
		}
	}

	public Type getRootType() {
		return rootType;
	}

	@Override
	public void enterTypeParams(TypeParamsContext ctx) {
		scopes.push(Scope.TYPE_PARAM);
	}

	@Override
	public void exitTypeParams(TypeParamsContext ctx) {
		final Scope popped = scopes.pop();
		assert popped == Scope.TYPE_PARAM;
	}

	@Override
	public void enterStatement(StatementContext ctx) {
		// no op
	}

	@Override
	public void exitStatement(StatementContext ctx) {
		// no op

	}

	@Override
	public void enterAlias_definition(Alias_definitionContext ctx) {
		scopes.push(Scope.ALIAS_DEFINITION);
	}

	@Override
	public void exitAlias_definition(Alias_definitionContext ctx) {
		final String aliasName = ctx.getToken(ToodleLexer.IDENT, 0).getText();
		final Type parent = currentType.getParent();
		parent.addAliasDefinition(aliasName, currentType);
		currentType = parent;
		final Scope popped = scopes.pop();
		assert popped == Scope.ALIAS_DEFINITION;
	}

	@Override
	public void enterLet_definition(Let_definitionContext ctx) {
		scopes.push(Scope.VAR_DEFINITION);
		// no op
	}

	@Override
	public void exitLet_definition(Let_definitionContext ctx) {
		final Scope popped = scopes.pop();
		assert popped == Scope.VAR_DEFINITION;
		final String name = ctx.getToken(ToodleLexer.VARIABLE, 0).getText().substring(variablePrefix.length());
		currentType.addVarDefinition(name, currentVarValue);

	}

	@Override
	public void enterArray(ArrayContext ctx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exitArray(ArrayContext ctx) {
		// TODO Auto-generated method stub
	}
}
