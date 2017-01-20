package rs.ac.bg.etf.pp1.impl;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java_cup.runtime.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

import java.lang.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
    private int addOpLeft = 0;
    private int addOpRight = 0;
    private int mulOpLeft = 0;
    private int mulOpRight = 0;
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
    
    public static final int operationCodes[] = {0, Code.add, Code.sub, Code.mul, Code.div, Code.rem};
    private Obj obj1;
    private Obj obj2;
    private Integer index;
    //condition
    private int currentConditionAddress;
    private int conditionOperation;
    private Stack<Integer> fixup  = new Stack<Integer>();
    private Stack<Obj> arrayElemStack = new Stack<Obj>();
    
    private boolean inAssign = false;
    private boolean factorComesFromDesignator = false;
    private Obj currentDesignator = null;
    
    //==========================================================================
    
    public static class IfConstruct extends ConditionReason{
            public boolean hasElse;
            public int fixElse;
    }

    public static class ConditionReason{
            List<OrConditions> orCond = new ArrayList<OrConditions>();
            public int start;
    }
    public static class OrConditions{
            List<ConditionFact> andCond = new ArrayList<ConditionFact>();
    }
    public static class ConditionFact {
            public int startAddress;
            public int fixAddress;
            public int opcode;
    }
    
    //==========================================================================
    //UTIL
    //==========================================================================
    
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
    
     public void incrementAddOpRight(){
        addOpRight++;
    }
    
    public void incrementAddOpLeft(){
        addOpLeft++;
    }
    
    public void incrementMulOpRight(){
        mulOpRight++;
    }
    
    public void incrementMulOpLeft(){
        mulOpLeft++;
    }
    
    //==========================================================================
    //PROGRAM
    //==========================================================================
        
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
        Obj helper = new Obj(Obj.Var, "_tmp", new Struct(Struct.Int));
        Tab.currentScope().addToLocals(new Obj(Obj.Type, "string",  new Struct(Struct.Array, Tab.charType)));
        Tab.currentScope().addToLocals(new Obj(Obj.Type, "intArray",  new Struct(Struct.Array, Tab.intType)));
        Tab.insert(helper.getKind(), helper.getName(), helper.getType());
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
    }
    
    
    
    //==========================================================================
    //DEFINE
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

    //==========================================================================    
    //METHOD
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
        if(currentMethod != null){
            boolean isGlobal = true; //temp; 
            
            currentMethod.setAdr(Code.pc);
            Code.put(Code.enter);

            if (isGlobal)
                Code.put(currentMethod.getLevel());
            else
                Code.put(currentMethod.getLevel() + 1);

            if (isGlobal)
                Code.put(Tab.currentScope().getnVars());
            else
                Code.put(Tab.currentScope().getnVars());	
        }
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
    //CLASS
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
    
    public void addIncrement(Object obj, int line){
        Obj designator = (Obj)obj;
        Struct type = null;
        if(designator.getType().getKind()==Struct.Array)
            type = designator.getType().getElemType();
        else
            type = designator.getType();
        
        if(designator == Tab.noObj){
            reportError("Increment statement not valid",line);
            return;
        }
        if(designator.getKind() != Obj.Var && designator.getKind() != Obj.Elem){
            reportError("Increment statement not valid, identifier is not a variable ",line);
            return;
        }
        if(type.getKind()!=Struct.Int) {
            reportError("Variable must be of type int", line);
            return;
        }
        //push increment instruction on stack
        if (designator.getKind() == Obj.Elem)
            Code.put(Code.dup2);
        Code.load(designator);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(designator);
    }
     
    public void addDecrement(Object obj, int line){
        Obj designator = (Obj)obj;
        Struct type = null;
        if(designator.getType().getKind()==Struct.Array)
            type = designator.getType().getElemType();
        else
            type = designator.getType();
        
        if(designator == Tab.noObj){
            reportError("Increment statement not valid",line);
            return;
        }
        if(designator.getKind() != Obj.Var && designator.getKind() != Obj.Elem){
            reportError("Increment statement not valid, identifier is not a variable ",line);
            return;
        }
        if(type.getKind()!=Struct.Int) {
            reportError("Identifier must be of type int", line);
            return;
        }
        //push decrement instruction on stack
        if (designator.getKind() == Obj.Elem)
            Code.put(Code.dup2);
        Code.load(designator);
        Code.loadConst(1);
        Code.put(Code.sub);
        Code.store(designator);
    }
    
    public void addPrint(Object t, Object len, int line){
        Struct expressionType = (Struct)t;
        Integer length = (Integer)len;
        Struct type = null;
        if(expressionType.getKind()==Struct.Array)
            type = expressionType.getElemType();
        else
            type = expressionType;
        
        if(type.getKind() != Struct.Int && type.getKind() != Struct.Char){
            reportError("Print paremeter must be of type int or char",line);
            return;            
        }
        if(type.getKind() == Struct.Int){
            Code.loadConst(length);
            Code.put(Code.print);
        }else{
            Code.loadConst(length);
            Code.put(Code.bprint);
        }
        
    }
    
    public void addRead(Object obj, int line){
        Obj designator = (Obj)obj;
        
        Struct type = null;
        if(designator.getType().getKind()==Struct.Array)
            type = designator.getType().getElemType();
        else
            type = designator.getType();
        
        if(designator.getKind()!=Obj.Var){
            reportError("Read parameter " + designator.getName() + " is not a variable",line);
            return;
        }
        
        if(type.getKind() != Struct.Int && type.getKind() != Struct.Char){
            reportError("Read paremeter must be of type int or char",line);
            return;            
        }
        
        
        if (type.getKind() == Struct.Int){
                Code.put(Code.read);
                Code.store(designator);
        }
        else{
                Code.put(Code.bread);
                Code.store(designator);
        }
    }

    
    //==========================================================================
    //DESIGNATOR_STATEMENT
    //==========================================================================   
   
    
    public void setInAssign(boolean p){
        inAssign = p;
    }
    
    
    //==========================================================================
    //DESIGNATOR
    //==========================================================================   

    
    //==========================================================================
    //DESIGNATOR_RIGHT
    //==========================================================================    
    

    
    public void setDesignatorArrayExtension(String ident,int line){
        Obj temp = Tab.find(ident);
        if(temp.equals(Tab.noObj)){
            reportError("undefined variable",line);
            return;
        }
        if(temp.getType().getKind()==Struct.Array)
            currentDesignator = temp;
    }    
    
    
    public Obj designatorExtensionResolveArray(int line){
        Obj res = null;
        if(currentDesignator.getType().getKind() != Struct.Array){
            reportError("variable not of array type",line);
            return Tab.noObj;
        }else{
            Code.load(currentDesignator);
            res = new Obj(Obj.Elem, currentDesignator.getName(),new Struct(Struct.Array, currentDesignator.getType().getElemType()));
            currentDesignator = res;
        }
        return res;
    }
    
            
    public Obj designatorResolveIdentificator(String ident, int line){
        Obj res = null;
        if(currentDesignator != null){
            res = currentDesignator;
        }else{
            res = Tab.find(ident);
        }
        currentDesignator = null;
        return res; 
    }    
     
    public void addAssign(Object d,Object op, Object e,int line){
        Obj designator = (Obj)d;
        Struct expression = (Struct)e;
        Integer operation = (Integer)op;
        
        if(designator.getKind()==Obj.Con){
            reportError("Expression is not assignable to constant " + designator.getName(),line);
            return;
        }
        
//        if(designator.getType().getKind()==Struct.Array){
//            if(designator.getType().getElemType().getKind() != Struct.Int){
//                reportError("Expression is not assignable to designator " + designator.getName(),line);
//                return;
//            }
//        }else{
//            if(designator.getType().getKind() != Struct.Int){
//                reportError("Expression is not assignable to designator " + designator.getName(),line);
//                return;
//            }
//        }
        
        if(operation == 0){ // assign operation
            Code.store(designator);
        }else{
            if(designator.getType().getKind()==Struct.Array){
                 Obj rightOperand = Tab.find("_tmp");
                Code.store(rightOperand);
                Code.put(Code.dup2);
                Code.load(designator);
                Code.load(rightOperand);
            } else {
               //generate code for non symmetrical operations on variables
                Obj rightOperand = Tab.find("_tmp");
                Code.store(rightOperand);
                Code.load(designator);
                Code.load(rightOperand);
            }
           
            Code.put(operation);
            Code.store(designator);

        }

        
    }
    
    //==========================================================================
    //EXPRESSION_WITH_SIGN
    //==========================================================================    
    
    
    public void addNegation(){
        Code.put(Code.neg);
    }
    
    public Struct checkInteger(Object t, int line) {
        Struct type = (Struct)t;
        if(type.getKind() == Struct.Array){
            if(type.getElemType() != Tab.intType)
                reportError("Expression return type must be of type int ", line);
        }
        if (type != Tab.intType)
        {
                reportError("Expression return type must be of type int ", line);
        }
        return type;
    }
    
    public void setFromDesignator(boolean p){
        factorComesFromDesignator = p;
    }
    
    public void loadIfArray(Object o){
        Obj obj = (Obj)o;
        if(obj.getType().getKind() == Struct.Array)
            Code.load(obj);
    }
    

    //==========================================================================
    //EXPRESSION
    //==========================================================================
    
    public void checkArrayFactorList(Object f){
        Obj factor = (Obj)f;
        if(factor.getType().getKind()==Struct.Array && factorComesFromDesignator && inAssign)
            Code.load(factor);
    }
    
    public Obj addLeft(Object l, Object op, Object r, int line){
        Obj left = (Obj)l;
        Obj right = (Obj)r;
        Integer operation = (Integer)op;

        Struct typeLeft = null;
        if(left.getType().getKind()==Struct.Array) typeLeft = left.getType().getElemType();
        else typeLeft = left.getType();
        
        Struct typeRight = null;
        if(right.getType().getKind()==Struct.Array) typeRight = right.getType().getElemType();
        else typeRight = right.getType();

        if(typeLeft.getKind() != Struct.Int || typeRight.getKind() != Struct.Int){
            reportError("Opearators must be of type int",line);
            return Tab.noObj;
        }

        //case when operation is assignable
        if(left.getType().getKind()==Struct.Array)
            insertIntoStack(left);
        Code.put(operation);
        Code.store(left);
        Code.load(left);
      
        addOpLeft--;
        return right;
    }
    
    public Obj addRight(Object l, Object op, Object r, int line) {
        Obj left = (Obj)l;
        Obj right = (Obj)r;
        Integer operation = (Integer)op;

        Struct typeLeft = null;
        if(left.getType().getKind()==Struct.Array) typeLeft = left.getType().getElemType();
        else typeLeft = left.getType();
        
        Struct typeRight = null;
        if(right.getType().getKind()==Struct.Array) typeRight = right.getType().getElemType();
        else typeRight = right.getType();

        if(typeLeft.getKind() != Struct.Int || typeRight.getKind() != Struct.Int){
            reportError("Opearators must be of type int",line);
            return Tab.noObj;
        }

        //case when operation is assignable
        if(left.getType().getKind()==Struct.Array)
            insertIntoStack(left);
        Code.put(operation);
        Code.store(left);
        Code.load(left);
      
        addOpLeft--;
        return left;
    }

    
    
    //==========================================================================
    //FACTOR_LIST 
    //==========================================================================
    
    public void checkArrayFactor(Object f){
        Obj factor = (Obj)f;
        if(factor.getType().getKind()==Struct.Array && factorComesFromDesignator && inAssign && mulOpLeft>0)
            Code.load(factor);
    }
    
    public Obj mulLeft(Object l, Object op, Object r, int line){
        Obj left = (Obj)l;
        Obj right = (Obj)r;
        Integer operation = (Integer)op;
        
        Struct typeLeft = null;
        if(left.getType().getKind()==Struct.Array) typeLeft = left.getType().getElemType();
        else typeLeft = left.getType();
        
        Struct typeRight = null;
        if(right.getType().getKind()==Struct.Array) typeRight = right.getType().getElemType();
        else typeRight = right.getType();

        if(typeLeft.getKind() != Struct.Int || typeRight.getKind() != Struct.Int){
            reportError("Opearators must be of type int",line);
            return Tab.noObj;
        }

        if(left.getType().getKind()==Struct.Array){
            Obj rightOperand = Tab.find("_tmp");
            Code.store(rightOperand);
            Code.load(left);
            Code.load(rightOperand);
        }
        Code.put(operation);
        mulOpLeft--;
        return right;
    }
    
    public Obj mulRight(Object l, Object op, Object r, int line){
        Obj left = (Obj)l;
        Obj right = (Obj)r;
        Integer operation = (Integer)op;
        
        Struct typeLeft = null;
        if(left.getType().getKind()==Struct.Array) typeLeft = left.getType().getElemType();
        else typeLeft = left.getType();
        
        Struct typeRight = null;
        if(right.getType().getKind()==Struct.Array) typeRight = right.getType().getElemType();
        else typeRight = right.getType();

        if(typeLeft.getKind() != Struct.Int || typeRight.getKind() != Struct.Int){
            reportError("Opearators must be of type int",line);
            return Tab.noObj;
        }
        //case when operation is assignable
        if(left.getType().getKind()==Struct.Array)
            insertIntoStack(left);
        Code.put(operation);
        Code.store(left);
        Code.load(left);
        
        mulOpRight--;
        return left;
    }
    
    public void insertIntoStack(Obj designator) {
        
        Obj right = Tab.find("_tmp");
        Code.store(right);
        Code.put(Code.dup2);
        Code.put(Code.dup2);
        Code.load(designator);
        Code.load(right);
    }
    
    
    //==========================================================================
    //FACTOR 
    //==========================================================================

     public Obj loadConstInteger(Integer constInteger){
        Obj res = new Obj(Obj.Con,"",Tab.intType);
        res.setAdr(constInteger);
        Code.loadConst(constInteger);
        return res;
    }
    
    public Obj loadConstChar(Character constChar){
        Obj res = new Obj(Obj.Con,"",Tab.charType);
        res.setAdr(constChar);
        Code.loadConst(constChar);
        return res;
    }
    
    public Obj loadConstBool(Boolean constBool){
        Obj res = new Obj(Obj.Con,"",new Struct(Struct.Bool));
        res.setAdr(constBool?1:0);
        Code.loadConst(constBool?1:0);
        return res;
    }
    
    public Obj loadDesignator(Object obj){
        Obj variable = (Obj)obj;
        Obj res = null;
        if(variable == Tab.noObj)
            res = variable;
        else{
            res = variable;
            if(res.getType().getKind() != Struct.Array)
                Code.load(res);
            else if (res.getType().getKind() == Struct.Array && !inAssign)
                Code.load(res);
                
        }
        factorComesFromDesignator = true;
        
        return res;        
    }
    
    public Obj createArray(Object t, Object e, int line){
        Struct type = (Struct)t;
        Struct expression = (Struct)e;
        Obj res = null;
        
        if(!expression.assignableTo(Tab.intType)){
            reportError("Array size expression must return an int value",line);
            return Tab.noObj;
        }
        res = new Obj(Obj.Elem,"",new Struct(Struct.Array,type));
        
        Code.put(Code.newarray);
        if (type.getKind() == Struct.Char)    
            Code.put(0);
        if (type.getKind() == Struct.Int)
            Code.put(1);
        
        return res;
    }
    
    public Obj createExpression(Object e){
        Struct expression = (Struct)e;
        Obj res = new Obj(expression.getKind(),"",expression);
        return res;
    }
    
    //==========================================================================
    //LEVEL 2
    //==========================================================================
    
    public void addConditionFact(Object e, int line){
        Struct expression = (Struct)e;
        if(expression.getKind() != Struct.Bool){
            reportError("Expression must be of boolean type",line);
            return;
        }
        
    }
    
    public void addConditionTerm(){
        Code.putJump(0);
        fixup.push(Code.pc-2);
        
        while(!fixup.empty())
            Code.fixup(fixup.pop());
    }
    
    public void addConditionFact(Object el, Object op, Object er, int line){
        Struct left = (Struct)el;
        Struct right = (Struct)er;
        Integer operation = (Integer)op;
        
        if(left != Tab.intType || right != Tab.intType){
            reportError("Operands are not comparable",line);
            return;
        }
        Code.putFalseJump(operation, 0);
        fixup.push(Code.pc - 2);
        
    }

    
    public void endIf(){
        
        Code.fixup(fixup.pop());
    }
    
    public void endIfPart(){
        Code.pc+=3;
        Code.fixup(fixup.pop());
        Code.pc-=3;
        
        Code.putJump(0);
        fixup.push(Code.pc - 2);
       
    }

    
    

}  
