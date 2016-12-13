package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;


%%

%{
		// ukljucivanje informacije o poziciji tokena
		private Symbol new_symbol(int type) {
				return new Symbol(type, yyline+1, yycolumn);
		}
		// ukljucivanje informacije o poziciji tokena
		private Symbol new_symbol(int type, Object value) {
				return new Symbol(type, yyline+1, yycolumn, value);
		}
%}

%cup

%xstate COMMENT

%eofval{ 
return new_symbol(sym.EOF);
%eofval}

%line
%column

%%
" " {}
"\b" {}
"\t" {}
"\r\n" {}
"\f" {}
"program" {return new_symbol(sym.PROG);}
"new"   {return new_symbol(sym.NEW);}
"break" {return new_symbol(sym.BREAK);}
"continue" {return new_symbol(sym.CONTINUE);}
"else" {return new_symbol(sym.ELSE);}
"const" {return new_symbol(sym.CONST);}
"if" {return new_symbol(sym.IF);}
"read" {return new_symbol(sym.READ);}
"for" {return new_symbol(sym.FOR);}
"extends" {return new_symbol(sym.EXTENDS);}
"class" {return new_symbol(sym.CLASS);}
"print" {return new_symbol(sym.PRINT);}
"return" {return new_symbol(sym.RETURN);}
"void" {return new_symbol(sym.VOID);}
"static" {return new_symbol(sym.STATIC);}
"++" {return new_symbol(sym.INC);}
"--" {return new_symbol(sym.DEC);}
"+" {return new_symbol(sym.ADD);}
"-" {return new_symbol(sym.SUB);}
"*" {return new_symbol(sym.MUL);} 
"/" {return new_symbol(sym.DIV);}
"%" {return new_symbol(sym.MOD);}
"==" {return new_symbol(sym.EQ);}
"!=" {return new_symbol(sym.NEQ);}
">=" {return new_symbol(sym.BGE);}
"<=" {return new_symbol(sym.BLE);}
">" {return new_symbol(sym.BGR);}
"<" {return new_symbol(sym.BLS);}
"=" {return new_symbol(sym.ASSIGN);}
"+=" {return new_symbol(sym.ASSIGN_ADD);}
"-=" {return new_symbol(sym.ASSIGN_SUB);}
"*=" {return new_symbol(sym.ASSIGN_MUL);}
"/=" {return new_symbol(sym.ASSIGN_DIV);}
"%=" {return new_symbol(sym.ASSIGN_MOD);}
";" {return new_symbol(sym.SEMI_COMMA);}
"," {return new_symbol(sym.COMMA);}
"(" {return new_symbol(sym.LPAREN);}
")" {return new_symbol(sym.RPAREN);}
"{" {return new_symbol(sym.LBRACE);}
"}" {return new_symbol(sym.RBRACE);}
"[" {return new_symbol(sym.LSQUARE);}
"]" {return new_symbol(sym.RSQUARE);}
"&&" {return new_symbol(sym.AND);}
"||" {return new_symbol(sym.OR);}
"." {return new_symbol(sym.DOT);}

"//" {yybegin(COMMENT);}

<COMMENT>. {yybegin(COMMENT);}
<COMMENT>"\r\n" {yybegin(YYINITIAL);}
("true" | "false") {return new_symbol (sym.BOOLCONST, Boolean.valueOf(yytext()));}
[0-9]+ {return new_symbol(sym.NUMBER, new Integer (yytext()));}
([a-z]|[A-Z])[a-z|A-Z|0-9|_]* {return new_symbol (sym.IDENT, yytext());}
"'"[\040-\176]"'" {return new_symbol (sym.CHARCONST, new Character (yytext().charAt(1)));}





. {System.err.println("Lexical error ("+yytext()+") on line "+(yyline+1));}