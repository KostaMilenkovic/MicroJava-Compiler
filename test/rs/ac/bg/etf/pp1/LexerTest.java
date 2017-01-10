package rs.ac.bg.etf.pp1;

import java.io.FileReader;

import java_cup.runtime.Symbol;


public class LexerTest {
	public static void main(String[] args) {
            try {
                    Yylex lexer = new Yylex(new FileReader("test/examples/example1.mj"));
                    Symbol symbol;
                    do
                    {
                        symbol = lexer.next_token();
                        System.out.println(symbol.sym + " " + symbol.value);
                    }
                    while(symbol.sym != sym.EOF);
            } catch (Exception e) {
                    e.printStackTrace();
            }
	}
}
