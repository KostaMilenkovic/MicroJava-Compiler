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
        private static CompilerImpl impl;

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws Exception {
		
		Logger log = Logger.getLogger(ParserTest.class);
		impl = new CompilerImpl();
		
		Reader br = null;
		try {

                File sourceCode = null;

                sourceCode = new File("test/examples/example1.mj");
                log.info("Compiling source file: " + sourceCode.getAbsolutePath());

                br = new BufferedReader(new FileReader(sourceCode));
                Yylex lexer = new Yylex(br);


                System.out.println("==============SEMANTICAL ANALYSIS================");

                Parser p = new Parser(lexer);
                Symbol s = p.parse();  //pocetak parsiranja


                System.out.println("================================================");
	        
//	        log.info("Print calls = " + p.printCallCount);
	        
	        
	        //dumpCount();
	        
	        //Tab.dump();
                
                impl.dump();
	        
		} 
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}

	}
        
        private static void dumpCount(){
            System.out.println("==============SINTATICAL ANALYSIS===============");
            System.out.println("Number of constants: " + impl.constCnt + "\n");
            System.out.println("Number of global variables: " + impl.globalVarCnt + "\n");
            System.out.println("Number of local variables: " + impl.localVarCnt + "\n");	        	        
            System.out.println("Number of global arrays: " + impl.globalArrayCnt + "\n");
            System.out.println("Number of local arrays: " + impl.localArrayCnt + "\n");
            System.out.println("Classes defined: " + impl.classCnt + "\n");
            System.out.println("==============================================");
        }
	
	
}
