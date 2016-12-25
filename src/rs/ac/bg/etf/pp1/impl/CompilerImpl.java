package rs.ac.bg.etf.pp1.impl;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java_cup.runtime.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

import java.lang.*;

public class CompilerImpl {
    //type definition
    private static final int BOOL_TYPE = 6;
    private static final Struct boolType = new Struct(BOOL_TYPE);
    
    public static boolean errorFlag = false;
    //scopes
    private Scope universeScope;
    private Scope currentScope;
    //
    public Obj currentProgram = null;
    public Scope programScope = null;
    public Obj currentMethod = null;
    public Obj mainMethod = null;
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
    //
    public int globalDataPtr = 0;
    public int localDataPtr = 0;
    
    public static final int operationCodes[] = {0,Code.add, Code.sub, Code.mul, Code.div, Code.rem};
    
    public CodeGenerator codeGenerator = new CodeGenerator();
    
    public void reportError(String msg, int line) {
        errorFlag = true;
        System.err.println("ERROR: " + msg + " on line : " + line);
    }

    public void reportInfo(String msg, int line) {
        //System.out.println("INFO: " + msg + " on line : " + line);
    }
	
    public void reportError(String msg) {
        errorFlag = true;
        System.err.println("ERROR: " + msg);
    }

    public void reportInfo(String msg) {
        //System.out.println("INFO: " + msg);
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
        programScope = currentScope;       
    }
    
    public void endProgram(){
        if(mainMethod == null){
            reportError("Main method not defined");
        }
        
        Code.dataSize = Tab.currentScope().getnVars();
        Tab.chainLocalSymbols(currentProgram);
        Tab.closeScope();

        if(errorFlag){
            reportInfo("Semantic analysis has failed");
        }else{
            reportInfo("Semantic analysis finished successfully");
        }
        codeGenerator.dump();
    }
    
    //==========================================================================
    
    public void defineConst(String constName, Object value, int line){
        if(Tab.currentScope().findSymbol(constName) != null){
            reportError("Name " + constName + " is already defined", line);
            return;
        }
        
        //code generation
        int adr = 0;
        if(value instanceof Integer)
            adr = (Integer)value;
        else if (value instanceof Character)
            adr = (int)(Character)value;
        else if (value instanceof Boolean)
            adr = 1;
        //===============
        
        Tab.insert(Obj.Con, constName, currentType).setAdr(adr);
        constCnt++;
        
    }
    
    public void defineVar(String varName, boolean isGlobal, int line){
        if(Tab.currentScope().findSymbol(varName) != null){
            reportError("Name " + varName + " is already defined",line);
            return;
        }
        Obj var = Tab.insert(Obj.Var, varName, currentType);
        
        if(isGlobal){
            var.setAdr(globalDataPtr);
            globalDataPtr++;
            globalVarCnt++;
        }
        else{
            var.setAdr(localDataPtr);
            localDataPtr++;
            localVarCnt++;
        }
        
    }
    
    public void defineArray(String arrayName, boolean isGlobal, int line){
        if(Tab.currentScope().findSymbol(arrayName) != null){
            reportError("Name " + arrayName + " is already defined",line);
            return;
        }
        Struct arrayType = new Struct(Struct.Array,currentType);
        Obj arrayVar = Tab.insert(Obj.Var, arrayName, arrayType);
        
        if(isGlobal){
            arrayVar.setAdr(globalDataPtr);
            globalDataPtr++;
            globalArrayCnt++;
        }
        else{
            arrayVar.setAdr(localDataPtr);
            localDataPtr++;
            localArrayCnt++;
        }
        
    }
    
    public Struct createArray(Object t, Object e, int line){
        Struct type = (Struct)t;
        Struct expression = (Struct)e;
        
        if(!expression.assignableTo(Tab.intType)){
            reportError("Array size expression must return an int value",line);
            return Tab.noType;
        }
        
        Code.put(Code.newarray);
        
        if (type.getKind() == Struct.Char)    
            Code.put(0);
        if (type.getKind() == Struct.Int)
            Code.put(1);
        
        
        Struct arrayType = new Struct(Struct.Array,type);
        
        return arrayType;
        
    }
    
    public String getArrayElem(String ident,Object e,int line){
        Struct expression = (Struct)e;
        Obj obj = getObj(ident,line);
        Code.load(obj);
        
        if(!expression.assignableTo(Tab.intType)){
            reportError("Array index expression must be of int type",line);
            return ident + "[]";
        }
        
        return ident + "[]";
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
            //loads method 
            
            
            if(methodName.equals("main")){
                mainMethod = currentMethod;
                currentMethod.setAdr(Code.pc);
                Code.mainPc = currentMethod.getAdr();
            }else{
                currentMethod.setAdr(Code.pc);
            }
            
            Tab.openScope();
            currentScope = Tab.currentScope();
            
        }
    }
    
    public void returnMatched(Object t, int line){
        Struct type = ((Struct)t);
        returnFlag = true;
        if(type==null)
            type=Tab.noType;
        
        if (currentMethod == null)
            return;

        if(type != Tab.noType && Tab.noType.equals(currentMethod.getType())) {           
            reportError("Returning value in void method", line);
            return;
        }
        if(type != Tab.noType && !type.assignableTo(currentMethod.getType())){
            reportError("Wrong type of return value ", line);
            return;
        }
                

    }
    
    public void enterMethod(){
        //enter global method
        if(currentMethod != null)
            codeGenerator.enterMethod(currentMethod, true);
    }
    
    public void defineMethodArg(Object aType, String argName, int line){
        Struct argType = (Struct)aType;
        Tab.insert(Obj.Var, argName, argType);
    }
    
    public void endMethod(int line){
        if(currentMethod != null) {        
            if(currentMethod.getType() != Tab.noType && returnFlag == false){
                reportError("Return statement is missing",line);
                returnFlag = false;
                currentMethod = null;
                
            }

            Tab.chainLocalSymbols(currentMethod);
            Tab.closeScope();
            
            //push return instruction to expression stack
            Code.put(Code.exit);
            Code.put(Code.return_);
            
        }
       
        returnFlag = false;
        currentMethod = null;

    }
    //==========================================================================
    //==========================================================================
    public void defineClass(String className, int line){
        if(Tab.currentScope().findSymbol(className) != null){
            reportError("Class " + className + " is already defined",line);
            return;
        }
        currentClass = new Struct(Struct.Class);
        Tab.insert(Obj.Type, className, currentClass);
        Tab.openScope(); 
        classCnt++;
        
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
            return Tab.noType;
        }
        return obj.getType();
    }
    
    

    
    //==========================================================================
    public Obj getObj(String objName, int line){

        //arrays
        if (objName.contains("[]")){
            
            String elem = objName.substring(0, objName.lastIndexOf("[]"));
            Obj base = Tab.find(elem);
            
            if(base.equals(Tab.noObj))
                return Tab.noObj;
            
            return new Obj(Obj.Elem, "", base.getType().getElemType());
        }
        Obj objFound = Tab.find(objName);
        
        if(objFound.equals(Tab.noObj)){
            reportError("Unknown identifier " + objName,line);
            return Tab.noObj;
        }
        return objFound;
    }
    
    public void addIncrement(Object obj, int line){
        Obj designator = (Obj)obj;
        
        if(designator == Tab.noObj){
            reportError("Increment statement not valid",line);
            return;
        }
        if(!designator.getType().equals(Tab.intType)) {
            reportError("Identifier must be of type int", line);
            return;
        }
        //push increment instruction on stack
        Code.load(designator);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(designator);
    }
     
    public void addDecrement(Object obj, int line){
        Obj designator = (Obj)obj;
        if(designator == Tab.noObj){
            reportError("Decrement statement not valid",line);
            return;
        }
        if(!designator.getType().equals(Tab.intType)) {
            reportError("Identifier must be of type int", line);
            return;
        }
        //push decrement instruction on stack
        Code.load(designator);
        Code.loadConst(1);
        Code.put(Code.sub);
        Code.store(designator);
    }
    
    public void addPrint(Object obj, Object len, int line){
        Struct designator = (Struct)obj;
        Integer length = (Integer)len;
        if(designator != Tab.charType && designator != Tab.intType){
            reportError("Print paremeter must be of type int or char",line);
            return;            
        }
        if(designator == Tab.intType){
            //push print int on expression stack
            Code.loadConst(length);
            Code.put(Code.print);
        }else{
            //push print char on expression stack
            Code.loadConst(length);
            Code.put(Code.bprint);
        }
        
    }
    
    public void addRead(Object obj, int line){
        Obj designator = (Obj)obj;
        if(designator == null){
            reportError("TODO: null exception ",line);
            return;
        }
        if(designator.getKind()!=Obj.Var){
            reportError("Read parameter " + designator.getName() + " is not a variable",line);
            return;
        }
        
        if (designator.getType() == Tab.intType){
                Code.put(Code.read);
                Code.store(designator);
        }
        else{
                Code.put(Code.bread);
                Code.store(designator);
        }
    }
    
    public Struct checkInteger(Object t, int line) {
        Struct type = (Struct)t;
        if (type != Tab.intType)
        {
                reportError("Expression return type must be of type int ", line);
                return Tab.noType;
        }
        return type;
    }
    
    public void loadConstInteger(Integer constInteger){
        Code.loadConst(constInteger);
    }
    
    public void loadConstChar(Character constChar){
        Code.loadConst(constChar);
    }
    
    public void loadConstBool(Boolean constBool){
        Code.loadConst(constBool?1:0);
    }
    
    public void addAssign(Object d,Object op, Object e,int line){
        Obj designator = (Obj)d;
        Struct expression = (Struct)e;
        Integer operation = (Integer)op;
        
        if(designator.getKind()==Obj.Con){
            reportError("Expression is not assignable to constant " + designator.getName(),line);
            return;
        }
        
        if(!expression.assignableTo(designator.getType())){
            reportError("Expression is not assignable to designator " + designator.getName(),line);
            return;
        }
        
        if(operation == 0){ // assign operation
            Code.store(designator);
        }else{
            Obj tempObj = new Obj(Obj.Var,"",Tab.intType);
            Code.store(tempObj);
            Code.load(designator);
            Code.load(tempObj);
            Code.put(operation);
            Code.store(designator);
        }

        
    }
    
    public void loadVariable(Object obj){
        Obj variable = (Obj)obj;
        //push variable on expression stack
        Code.load(variable);
    }
    
    public Struct addOperation(Object l, Object op, Object r, int line){
        Struct left = (Struct)l;
        Struct right = (Struct)r;
        Integer operation = (Integer)op;
        
        if(left != Tab.intType || right != Tab.intType){
            reportError("Opearators must be of type int",line);
            return Tab.noType;
        }
        
        Code.put(operation.intValue());
        return Tab.intType;
    }
    
    
    public void addNegation(){
        Code.put(Code.neg);
    }
    
    public void dump(){
        if (CompilerImpl.errorFlag) {
            reportError("BUILD FAILED");
            return;
        }
        try {
            reportInfo("BUILD SUCCESSFUL");
            OutputStream output = new FileOutputStream("mydist/program.obj");
            Code.write(output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}  
