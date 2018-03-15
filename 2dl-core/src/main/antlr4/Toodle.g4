grammar Toodle;

@header {
	package com.github.toodle;
}

definitions
	: NL* statement? (NL+ statement)* NL*
	;

statement
	: definition | alias_definition | let_definition
	;

alias_definition
	:
	'alias' IDENT '=' type
	;

let_definition
	:
	'let' VARIABLE '=' expr
	;

definition
	: IDENT+ (':' type)?
	;

type
	: IDENT ('<' typeParams '>')? annotation* ('{' definitions '}')?
	;

typeParams
	: type (',' type)*
	;

annotation
	: IDENT ('(' expr (',' expr)* ')')?
	;

expr
	: NUMBER | string | VARIABLE
	;

string
	: IDENT | QUOTED_STRING | MULTILINE_STRING
	;

fragment INT: '0' | [1-9] [0-9]*;
fragment ESC: '\\' (["\\/bfnrt] | UNICODE);
fragment UNICODE: 'u' HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];
fragment EXP: [Ee] [+\-]? INT;

MULTILINE_STRING: '"""' (ESC | ~ ["\\])* '"""';
QUOTED_STRING: '"' (ESC | ~ ["\\])* '"';
NUMBER: '-'? INT ('.' [0-9] +)? EXP?;
VARIABLE: '$' [a-zA-Z0-9_\-*]+;
IDENT: [a-zA-Z0-9_*]+;
NL: [\r\n];
WS: [ \t]+ -> skip ;
CONTINUATION_LINE: '\\' [\r\n]+ -> skip;
COMMENT:            '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT:       '//' ~[\r\n]*    -> channel(HIDDEN);