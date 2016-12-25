/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rs.ac.bg.etf.pp1.impl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

/**
 *
 * @author milenkok
 */
public class CodeGenerator{
    private OutputStream output;
    
    public CodeGenerator(){
        
    }
    
    public void dump(){
        
    }
    
      
    public void enterMethod(Obj method, boolean isGlobal)
    {
        method.setAdr(Code.pc);
        Code.put(Code.enter);

        if (isGlobal)
            Code.put(method.getLevel());
        else
            Code.put(method.getLevel() + 1);

        if (isGlobal)
            Code.put(Tab.currentScope().getnVars());
        else
            Code.put(Tab.currentScope().getnVars());	
    }

}
