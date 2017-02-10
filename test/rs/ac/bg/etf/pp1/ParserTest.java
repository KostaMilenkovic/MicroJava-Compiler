package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.impl.CompilerImpl;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class ParserTest {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws Exception {
		
		Logger log = Logger.getLogger(ParserTest.class);
		Reader br = null;
		try {

                    File sourceCode = null;

                    sourceCode = new File("test/examples/example1.mj");
                    System.out.print("BUILDING FROM SOURCE FILE: " + sourceCode.getAbsolutePath());

                    br = new BufferedReader(new FileReader(sourceCode));
                    Yylex lexer = new Yylex(br);
                    Parser p = new Parser(lexer);
                    Symbol s = p.parse(); 
                    String outputFileName = "mydist/program.obj";
                    File objFile = new File(outputFileName);
                    if(objFile.exists()) 
                        objFile.delete();
                    
                    if (p.impl.errorDetected) {
                        log.error("BUILD FAILURE");
                        System.err.print("BUILD FAILURE\n");
                    }
                    else {
                        Code.write(new FileOutputStream(new File(outputFileName)));
                        log.info("BUILD SUCCESSFULL");
                    }
		} 
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}
	}

}
