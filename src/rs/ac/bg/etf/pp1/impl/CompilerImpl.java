package rs.ac.bg.etf.pp1.impl;


import java_cup.runtime.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

import java.lang.*;

public class CompilerImpl {
    //type definition
    private static final int BOOL_TYPE = 6;
    private static final Struct boolType = new Struct(BOOL_TYPE);
    
    private static boolean errorFlag = false;
    //scopes
    private Scope universeScope;
    private Scope currentScope;
    //
    private Obj currentProgram;
    private Obj currentMethod;
    private static boolean returnFlag;
    //
    private Struct currentType;
    private Struct currentClass;
    //
    public static int constCnt = 0;
    public static int globalVarCnt = 0;
    public static int localVarCnt = 0;
    public static int globalArrayCnt = 0;
    public static int localArrayCnt = 0;
    public static int classCnt = 0;
    
    
    
    public void reportError(String msg, int line) {
        errorFlag = true;
        System.err.println("ERROR: " + msg + " on line : " + line);
    }

    public void reportInfo(String msg, int line) {
        System.out.println("INFO: " + msg + " on line : " + line);
    }
	
    public void reportError(String msg) {
        errorFlag = true;
        System.err.println("ERROR: " + msg);
    }

	public void reportInfo(String msg) {
        System.out.println("INFO: " + msg);
    }
        
    public void setCurrentType(Object type){
        this.currentType = (Struct)type;
    }
    
    public void startProgram(String programName){
        reportInfo("Semantic analysis has started");
        //define universe scope
        universeScope = Tab.currentScope();
        //define types
        Tab.insert(Obj.Type, "bool", boolType);
        //define program
        currentProgram = Tab.insert(Obj.Prog, programName, Tab.noType);
        //increments scope level
        Tab.openScope();
        //sets current scope
        currentScope = Tab.currentScope();
               
    }
    
    public void endProgram(){
        
        if(errorFlag){
            reportInfo("Semantic analysis has failed");
        }else{
            reportInfo("Semantic analysis finished successfully");
        }
        
        Tab.chainLocalSymbols(currentProgram);
        Tab.closeScope();
    }
    
    //==========================================================================
    
    public void defineConst(String constName, Object value, int line){
        if(Tab.currentScope().findSymbol(constName) != null){
            reportError("Name " + constName + " is already defined", line);
        }
        else{
            Tab.insert(Obj.Con, constName, currentType);
            constCnt++;
        }
    }
    
    public void defineVar(String varName, boolean isGlobal, int line){
        if(Tab.currentScope().findSymbol(varName) != null){
            reportError("Name " + varName + " is already defined",line);
        }else{
            Tab.insert(Obj.Var, varName, currentType);
            if(isGlobal)
                globalVarCnt++;
            else
                localVarCnt++;
        }
    }
    
    public void defineArray(String arrayName, boolean isGlobal, int line){
        if(Tab.currentScope().findSymbol(arrayName) != null){
            reportError("Name " + arrayName + " is already defined",line);
        }else{
            Tab.insert(Obj.Var, arrayName, currentType);
            if(isGlobal)
                globalArrayCnt++;
            else
                localArrayCnt++;
        }
    }
    
    //==========================================================================    
    public void defineMethod(Object mType, String methodName, int line){
        currentMethod = null;
        returnFlag = false;
        Struct methodType = (Struct)mType;
        if(methodType == null){
            methodType = Tab.noType;
        }
        
        if(Tab.currentScope().findSymbol(methodName) != null){
            reportError("Method " + methodName + " is already defined",line);
        }else{
            if(methodName.equals("main") && methodType != Tab.noType){
                reportError("Main method return type not void ",line);
                return;
            }
            
            currentMethod = Tab.insert(Obj.Meth, methodName, methodType);
            Tab.openScope();
            currentScope = Tab.currentScope();
            
        }
    }
    
    public void returnMatched(Object t, int line){
        Struct type = (Struct)t;
        returnFlag = true;
        if(type==null)
            type=Tab.noType;
        
        if (currentMethod == null)
            return;

        if(type != Tab.noType && Tab.noType.equals(currentMethod.getType())) {           
            reportError("Returning value in void method", line);
        }else if(type != Tab.noType && !type.assignableTo(currentMethod.getType())){
            reportError("Wrong type of return value ", line);
        }
                

    }
    
    public void endMethod(int line){
        if(currentMethod != null) {        
            if(currentMethod.getType() != Tab.noType && returnFlag == false){
                reportError("Return statement is missing",line);
            }
            Tab.chainLocalSymbols(currentMethod);
            Tab.closeScope();
            returnFlag = false;
            currentMethod = null;
        }
        

    }
    //==========================================================================
    //==========================================================================
    public void defineClass(String className, int line){
        if(Tab.currentScope().findSymbol(className) != null){
            reportError("Class " + className + " is already defined",line);
        }else{
            Tab.insert(Obj.Type, className, currentType);
            Tab.openScope(); 
            currentClass = new Struct(Struct.Class);
            classCnt++;
        }
    }
    
    public void endClass(){
        if(currentClass!=null){
            Tab.chainLocalSymbols(currentClass);
            Tab.closeScope();
            currentClass = null;
        }
    }
    

    

    //==========================================================================
    public Struct findType(String typeName, int line){
        Obj obj = Tab.find(typeName);
        if(obj == Tab.noObj || obj.getKind() != Obj.Type){
            reportError("Unknown type " + typeName,line);
        }
        return obj.getType();
    }
    
    

    
    //==========================================================================
    public Obj getObj(String objName){
        return Tab.find(objName);
    }
    
    public void addIncrement(Object d, int line){
        Obj designator = (Obj)d;
        if(designator == Tab.noObj){
            reportError("Increment statement not valid",line);
        }
        if(!designator.getType().equals(Tab.intType)) {
            reportError("Variable " + designator.getName() + " must be of type int", line);
        }
    }
     
    public void addDecrement(Object d, int line){
        Obj designator = (Obj)d;
        if(designator == Tab.noObj){
            reportError("Increment statement not valid",line);
        }
        if(!designator.getType().equals(Tab.intType)) {
            reportError("Variable " + designator.getName() + " must be of type int", line);
        }
    }
    
}  
