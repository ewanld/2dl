grammar Toodle;

@header {
	package com.github.toodle;
}

definitions
	: '\n'* definition? ('\n'+ definition)* '\n'*
	;
	
definition
	: IDENT+ (':' type)?
	;

type
	: IDENT ('<' typeParams '>')? constraint* ('{' definitions '}')?
	;

typeParams
	: type (',' type)*;
	
constraint
	: IDENT ('(' constraintParam (',' constraintParam)* ')')?
	;

constraintParam:
	NUMBER | string
	;

string
	: IDENT | QUOTED_STRING | MULTILINE_STRING
	;

MULTILINE_STRING
	:
	'"""' (ESC | ~ ["\\])* '"""'
	;
	
QUOTED_STRING
   : '"' (ESC | ~ ["\\])* '"'
   ;
   
NUMBER
   : '-'? INT ('.' [0-9] +)? EXP?
   ;
  
fragment INT
   : '0' | [1-9] [0-9]*
   ;
  
fragment ESC
   : '\\' (["\\/bfnrt] | UNICODE)
   ;
   
fragment UNICODE
   : 'u' HEX HEX HEX HEX
   ;
   
fragment HEX
   : [0-9a-fA-F]
   ;

fragment EXP
   : [Ee] [+\-]? INT
   ;
   
IDENT
	: [a-zA-Z0-9_\-*]+
	;

WS : [ \r\t]+ -> skip ;
CONTINUATION_LINE: '\\' [\r\n]+ -> skip;
COMMENT:            '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT:       '//' ~[\r\n]*    -> channel(HIDDEN);