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
        if (CompilerImpl.errorFlag) {
            return;
        }
        try {
                output = new FileOutputStream("mydist/proba.obj");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    

    
    public void dump(){
        Code.write(output);
    }
    
    public void loadConstInteger(Integer constInteger){
        //insert unnamed const integer in tab and add him an address
        Obj objConstNumber = Tab.insert(Obj.Con,"",Tab.intType);
        objConstNumber.setAdr(constInteger.intValue());
        //load it on expresion stack
        Code.load(objConstNumber);
    }
    
    public void loadMainMethod(Obj mainMethod){
        mainMethod.setAdr(Code.pc);
        Code.mainPc = mainMethod.getAdr();
    }
    
    public void loadMethod(Obj method){
        method.setAdr(Code.pc);
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
    
    public void endMethod(){
        
    }
    
    public void loadVariable(Obj variable){
        
    }
}
