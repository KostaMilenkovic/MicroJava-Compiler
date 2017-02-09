package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;
%%

%{
    // ukljucivanje informacije o poziciji tokena
    private Symbol new_symbol(int type) {
        return new Symbol(type, yyline+1, yycolumn);
    }

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
"\r" {}
"\n" {}
"\r\n" {}
"\f" {}
"program" {return new_symbol(sym.PROG, yytext());}
"new"   {return new_symbol(sym.NEW, yytext());}
"break" {return new_symbol(sym.BREAK, yytext());}
"continue" {return new_symbol(sym.CONTINUE, yytext());}
"else" {return new_symbol(sym.ELSE, yytext());}
"const" {return new_symbol(sym.CONST, yytext());}
"if" {return new_symbol(sym.IF, yytext());}
"read" {return new_symbol(sym.READ, yytext());}
"for" {return new_symbol(sym.FOR, yytext());}
"extends" {return new_symbol(sym.EXTENDS, yytext());}
"class" {return new_symbol(sym.CLASS, yytext());}
"print" {return new_symbol(sym.PRINT, yytext());}
"return" {return new_symbol(sym.RETURN, yytext());}
"void" {return new_symbol(sym.VOID, yytext());}
"static" {return new_symbol(sym.STATIC, yytext());}
";" {return new_symbol(sym.SEMI_COMMA, yytext());}
"," {return new_symbol(sym.COMMA, yytext());}
"." {return new_symbol(sym.DOT, yytext());}
"(" {return new_symbol(sym.LPAREN, yytext());}
")" {return new_symbol(sym.RPAREN, yytext());}
"{" {return new_symbol(sym.LBRACE, yytext());}
"}" {return new_symbol(sym.RBRACE, yytext());}
"[" {return new_symbol(sym.LSQUARE, yytext());}
"]" {return new_symbol(sym.RSQUARE, yytext());}
"++" {return new_symbol(sym.INC, yytext());}
"--" {return new_symbol(sym.DEC, yytext());}
"+" {return new_symbol(sym.ADD, yytext());}
"-" {return new_symbol(sym.SUB, yytext());}
"*" {return new_symbol(sym.MUL, yytext());} 
"/" {return new_symbol(sym.DIV, yytext());}
"%" {return new_symbol(sym.MOD, yytext());}
"==" {return new_symbol(sym.EQ, yytext());}
"!=" {return new_symbol(sym.NEQ, yytext());}
">=" {return new_symbol(sym.GEQ, yytext());}
"<=" {return new_symbol(sym.LEQ, yytext());}
">" {return new_symbol(sym.GT, yytext());}
"<" {return new_symbol(sym.LT, yytext());}
"=" {return new_symbol(sym.ASSIGN, yytext());}
"+=" {return new_symbol(sym.ASSIGN_PLUS, yytext());}
"-=" {return new_symbol(sym.ASSIGN_MINUS, yytext());}
"*=" {return new_symbol(sym.ASSIGN_MUL, yytext());}
"/=" {return new_symbol(sym.ASSIGN_DIV, yytext());}
"%=" {return new_symbol(sym.ASSIGN_MOD, yytext());}
"&&" {return new_symbol(sym.AND, yytext());}
"||" {return new_symbol(sym.OR, yytext());}
"#" {return new_symbol(sym.NEW_OP, yytext());}

"//" {yybegin(COMMENT);}

<COMMENT>. {yybegin(COMMENT);}
<COMMENT>"\r\n" {yybegin(YYINITIAL);}
<COMMENT>"\r" {yybegin(YYINITIAL);}
<COMMENT>"\n" {yybegin(YYINITIAL);}

("true" | "false") {return new_symbol (sym.BOOL, Boolean.valueOf(yytext()));}
[0-9]+ {return new_symbol(sym.NUMBER, new Integer (yytext()));}
([a-z]|[A-Z])[a-z|A-Z|0-9|_]* {return new_symbol (sym.IDENT, yytext());}
"'"."'" {return new_symbol (sym.CHAR, new Character (yytext().charAt(1)));}





. {System.err.println("Lexical error ("+yytext()+") on line "+(yyline+1));}