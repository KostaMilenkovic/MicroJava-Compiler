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
		CompilerImpl impl = new CompilerImpl() ;
		
		Reader br = null;
		try {

			File sourceCode = null;
			
			sourceCode = new File("test/examples/example1.mj");
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());
			
			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);
			
			
			
			System.out.println("==============SEMANTICKA ANALIZA================");
			
			Parser p = new Parser(lexer);
	        Symbol s = p.parse();  //pocetak parsiranja
	        
	        System.out.println("================================================");
	        
//	        log.info("Print calls = " + p.printCallCount);
	        
	        
//	        System.out.println("==============SINTAKSNA ANALIZA=================");
//	        System.out.println("Broj konstanti: " + impl.constCnt + "\n");
//	        System.out.println("Broj globalnih promenljivih: " + impl.globalVarCnt + "\n");
//	        System.out.println("Broj loknalnih promenljivih: " + impl.localVarCnt + "\n");	        	        
//	        System.out.println("Broj globalnih nizova: " + impl.globalArrayCnt + "\n");
//	        System.out.println("================================================");
	        
//	        Tab.dump();
//	        
//	        if (!impl.errorFlag) {
//	        	File objFile = new File("test/program.obj");
//	        	
//	        	String filename = sourceCode.getName().substring(0, sourceCode.getName().lastIndexOf('.')) + ".obj";
//	        	String path = sourceCode.getPath().substring(0, sourceCode.getPath().lastIndexOf('\\'));
//	        	File objFile = new File(path + '\\' + filename);
//	        	
//	        	if (objFile.exists())
//	        		objFile.delete();
//	        	Code.write(new FileOutputStream(objFile));
//	        	
//	        	log.info("Parsiranje uspesno zavrseno!");
//	        }
//	        else {
//	        	log.error("Parsiranje nije uspesno zavrseno!");
//	        }
	        
		} 
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}

	}
	
	
}
