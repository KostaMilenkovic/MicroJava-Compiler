package rs.ac.bg.etf.pp1.impl;

import JFlex.sym;
import java_cup.runtime.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;
import org.apache.log4j.Logger;
import rs.etf.pp1.mj.runtime.Code;
import java.lang.*;
import java.util.EnumMap;
import java.util.Map;


public class CompilerImpl {	
    public Integer printCallCount = 0;
    private Boolean mainIsDefined = false;
    public static boolean errorDetected = false;
    
    public Scope globalScope;
    public Obj currentProgram;
    public Scope currentScope;
    
    public Struct currentVarType;
    public Struct currentConstType;
    public Struct currentMethodType;
    public Struct currentExtendedClassType;
    
    public Obj currentMethod;
    public Obj currentDesignator;
    public Obj currentClass;
    
    public String currentMethodName;
    public String currentClassName;

    public int currentMethodNameLine;
    public Boolean currentMethodIsStatic;
    public Boolean currentMethodHasReturn;
    public boolean methodForClass;
    
    public boolean isInForStatement;
    public boolean fromDesignator;
    public boolean inAssign;

    public int mulOpLeftCount;
    public int mulOpRightCount;
    public int addOpLeftCount;
    public int addOpRightCount;
    
    private EnumMap<Position, Integer> elementCounter = new EnumMap<Position, Integer>(Position.class);
    
    public enum Position {
        GLOBAL_CONST,
        GLOBAL_VAR,
        GLOBAL_ARRAY,
        GLOBAL_METHOD,
        MAIN_VAR,
        ARRAY_ITEM_CALLS,
        METHOD_ARGUMENT,
        BLOCK_STATEMENTS,
        CLASS_CREATIONS,
        CLASS_FIELD_CALLS,
        CLASS_METHOD_DEFINITIONS,
        CLASS_METHOD_CALLS
    }
    
    public enum DefinitionScope {
        GLOBAL,
        LOCAL,
        CLASS,
        ELSE
    }

    public Logger log = Logger.getLogger(getClass());
    
    public void reportInfo(String message, int line) {
        log.info(message + " on line : " + line);
    }
    
    public void reportInfo(String message) {
        log.info(message);
    }
    
    public void reportError(String message, int line) {
        errorDetected = true;
        System.out.println("ERROR: " + message + " on line : " + line);
        log.error(message + " on line : " + line);
    }
    
    public void reportError(String message) {
        errorDetected = true;
        System.err.println("ERROR : " + message);
        System.err.flush();
        log.error(message);
    }
    
    public void unrecoveredSyntaxError(Symbol curToken) throws java.lang.Exception {
        reportFatalError("Syntax error on token " + curToken.toString());
    }
    
    public void reportFatalError(String message) throws java.lang.Exception {
      reportError("FATAL ERROR :  " + message);
    }
    //==========================================================================
    //PROGRAM
    //==========================================================================
    public void startProgram(String progName) {
        Tab.insert(Obj.Prog, progName, Tab.noType);
        reportInfo("Compiling " + progName + "...");
        Tab.init();
        Tab.currentScope().addToLocals(new Obj(Obj.Type, "bool", new Struct(Struct.Bool)));
        Tab.currentScope().addToLocals(new Obj(Obj.Type, "string", new Struct(Struct.Array, Tab.charType)));
        Tab.currentScope().addToLocals(new Obj(Obj.Type, "intArray", new Struct(Struct.Array, Tab.intType)));
        
        Obj sysTmp = new Obj(Obj.Var, "_sys_tmp", new Struct(Struct.Int));
        
        globalScope = Tab.currentScope();
        currentProgram = Tab.insert(Obj.Prog, progName, Tab.noType);
        
        Tab.openScope();
        currentScope = Tab.currentScope();
        
        Tab.insert(sysTmp.getKind(), sysTmp.getName(), sysTmp.getType());
        
    }
    
    public void endProgram() {
        if(!mainIsDefined) {
            reportError("Main function not defined");
        }
        countElements();
        
        Code.dataSize = Tab.currentScope().getnVars();
        Tab.chainLocalSymbols(currentProgram);
        Tab.closeScope();
    }
    
    public void countElements() {
        System.out.println("\nELEMENT COUNT : \n===============");
        for (Map.Entry<Position, Integer> entry : elementCounter.entrySet())
        {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println("===============");
        Tab.dump();
    }
    
    public void countElement(Position item) {
        elementCounter.putIfAbsent(item, 0);
        elementCounter.put(item, elementCounter.get(item)+1);
    }
    
    public Struct resolveType(String typeName, int line) {
        Obj obj = Tab.find(typeName);
        if((obj == Tab.noObj) || (obj.getKind() != Obj.Type)) {
            reportError("Type not defined " + typeName, line);
            return Tab.noType;
        }
        return obj.getType();
    }
    //==========================================================================
    //CONST
    //==========================================================================
    
    public void defineConst(String constName, Object constValue, int line) {
        if(Tab.currentScope().findSymbol(constName) != null) {
            reportError("Constant " + constName + " already declared" , line);
            return;
        }
        
        int address = 0;
        int constKind = currentConstType.getKind();

        if(((constKind == Struct.Int)&&!(constValue instanceof Integer))||((constKind == Struct.Bool)&&!(constValue instanceof Boolean))||((constKind == Struct.Char)&&!(constValue instanceof Character))) {
            reportError("Uncompatible value type", line);
        } else {
            if(constValue instanceof Integer) {
                address = (Integer) constValue;
            } else if (constValue instanceof Character) {
                address = (Character) constValue;
            } else if (constValue instanceof Boolean) {
                address = ((Boolean)constValue) == null ? 0 : 1;
            }
            Tab.insert(Obj.Con, constName, currentConstType).setAdr(address);
            countElement(Position.GLOBAL_CONST);
        }
        
    }
    
    //==========================================================================
    //VAR
    //==========================================================================
    
    public void defineVar(String varName, DefinitionScope variableScope, Boolean isArray, int line) {
        if(Tab.currentScope().findSymbol(varName) != null){
            reportError("Variable " + varName + " already declared",line);
            return;
        }

        switch(variableScope) {
            case GLOBAL:
                if(isArray)
                    countElement(Position.GLOBAL_ARRAY);
                else 
                    countElement(Position.GLOBAL_VAR);
                break;
            case LOCAL:
                if((currentMethod!= null)&&(currentMethod.getName().equals("main"))) 
                    countElement(Position.MAIN_VAR);
                break;
            default:
                return;
        }
        
        if(isArray)
            Tab.insert(Obj.Var, varName, new Struct (Struct.Array, currentVarType));
        else
            Tab.insert(Obj.Var, varName, currentVarType);   
    }
    
    //==========================================================================
    //CLASS
    //==========================================================================
    
    //ok
    public void startClass(String className, int line) {
        currentClassName = className;
        if(Tab.currentScope().findSymbol(currentClassName) != null){
            reportError("Class " + currentClassName + " already declared" , line);
            return;
        }
        currentClass = Tab.insert(Obj.Type, currentClassName, new Struct(Struct.Class));
        Tab.openScope(); 
    }
    
    public void endClass() {
        Tab.chainLocalSymbols(currentClass);
        Tab.closeScope();
        currentClass = null;
    }
    
    //==========================================================================
    //METHOD
    //==========================================================================
    
    public void defineMethod() {
        String methodName = currentMethodName;
        Struct methodType = currentMethodType;
        int line = currentMethodNameLine;
        
        currentMethodHasReturn = false;
        
        if(Tab.currentScope().findSymbol(methodName) != null){
            reportError("Method " + methodName + " already declared" , line);
            return;
        }
            if(currentMethodType == null)
                currentMethodType = Tab.noType;
            
            currentMethod = Tab.insert(Obj.Meth, currentMethodName, currentMethodType);
            Tab.openScope();

        
        if(methodForClass) 
            countElement(Position.CLASS_METHOD_DEFINITIONS);
    }
    
    public void startMethod() {
        currentMethod.setAdr(Code.pc);
        if(currentMethodName.equals("main")) {
            mainIsDefined = true;
            Code.mainPc = currentMethod.getAdr();
        }
        Code.put(Code.enter);
        Code.put(currentMethod.getLevel());
        Code.put(Tab.currentScope().getnVars());
    }
    
    public void endMethod() {
        if((currentMethod.getType() != Tab.noType) && (currentMethodHasReturn == false))
            reportError("Return statement missing");
        
        Code.put(Code.exit);
        Code.put(Code.return_);
        
        Tab.chainLocalSymbols(currentMethod);
        Tab.closeScope();
        
        currentMethod = null;
        currentMethodHasReturn = false;
        
    }
    
    //==========================================================================
    //STATEMENT
    //==========================================================================

    public Struct checkConditionTypes(Struct left, Struct right, int line) {

        if(left.getKind() == Obj.Con){
            reportError("Can not compare types "+line);
            return Tab.noType;
        }
        if(!left.assignableTo(right)) {
            reportError("Can not compare types"+line);
            return Tab.noType;
        }
        
        return new Struct(Struct.Bool);
    }
    
    public void checkIfInt(Struct type, int line) {
        if(!type.assignableTo(Tab.intType)) {
            reportError("Type has to be int", line);
        }
    }
    
    public Obj resolveIdentificator(String name, int line) {

        Obj temp = Tab.find(name);
        
        if(temp.equals(Tab.noObj)) {
            reportError("Undefined identifier " + name, line);
            return Tab.noObj;
        }
        
        if (temp.getKind() == Obj.Con) 						
            reportInfo("Constant named " + name + " detected", line);
        else if (temp.getKind() == Obj.Var) 
            if (temp.getLevel() == 0) 
                reportInfo("Global variable " + name + " detected", line);
            else 
                reportInfo("Local variable " + name + " detected ", line);

        return temp;
    }
    
    public void checkReturn(Struct expr) {
        if(expr == null)
            return;

        if(expr.getKind() != currentMethod.getKind())
            reportError("Incompatible return value type");
        
        if(currentMethod!=null)
            currentMethodHasReturn = true;
    }
   
    public void checkIfCondition(Struct condition, int line) {
        if(condition.getKind()!=Struct.Bool) 
            reportError("Condition not of boolean type", line);
    }
    
    public void checkForCondition(Struct condition, int line) {
        if(condition==null)
            return;
        
        if(condition.getKind() != Struct.Bool)
            reportError("Condition not of boolean type", line);

    }
    
    public void checkPrint(Struct expr, Integer number, int line) {
        if(expr == null)return;
        Struct type = (expr.getKind()==Struct.Array)?expr.getElemType():expr;
        
        if(((type != Tab.charType) && (type != Tab.intType)&&(type.getKind() != Struct.Bool))){
            reportError("Expression has to be of type int, char or bool", line);
            return;
        }

        if(type == Tab.intType) {
            Code.loadConst(5);
            Code.put(Code.print);
        } else if (type == Tab.charType) {
            Code.loadConst(1);
            Code.put(Code.bprint);
        }

    }
    
    public void checkRead(Obj designator, int line) {
        if(designator == null)return;
        if(designator == Tab.noObj){
            reportError("Invalid designator", line);
            return;
        }
        int kind = designator.getKind();
        if((kind != Obj.Var)&&(kind != Obj.Fld)&&(kind != Obj.Elem)) {
            reportError("Designator is not of kind var, array element or class field", line);
            return;
        }
        if((designator.getType() != Tab.intType)&&(designator.getType() != Tab.charType)&&(designator.getType().getKind() != Struct.Bool)) {
            reportError("Designator is not of type int, char or bool", line);
            return;
        } 
        
        if(designator.getType() == Tab.intType || designator.getType().getKind() == Struct.Bool) {
            Code.put(Code.read);
            Code.store(designator);
        } else {
            Code.put(Code.bread);
            Code.store(designator);
        }
       
       
    }
    
    public void checkContinue(int line) {
        if(!isInForStatement)
            reportError("Continue statement is not in for loop", line);
    }
    
    public void checkBreak(int line) {
        if(!isInForStatement)
            reportError("Break statement is not in for loop", line);
    }
    
    //==============================================================================
    //DESIGNATOR
    //==============================================================================
    
    public Obj designatorInc(Obj des, int line) {
        if(des == null)return Tab.noObj;
        if(des == Tab.noObj) {
            reportError("Invalid designator", line);
            return Tab.noObj;
        }
        Struct desType = isArray(des.getType())?des.getType().getElemType():des.getType();
        int kind = des.getKind();
        if((kind != Obj.Var)&&(kind != Obj.Fld)&&(kind != Obj.Elem)) {
            reportError("Designator is not of kind var or class field", line);
            return Tab.noObj;
        } 
        if(desType.getKind() != Struct.Int) {
            reportError("Designator is not of type int", line);
            return Tab.noObj;
        }
        
        if (des.getKind() == Obj.Elem)
            Code.put(Code.dup2);

        Code.load(des);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(des);

        return des;
    }
    
    public Obj designatorDec(Obj designator, int line) {

        if(designator == Tab.noObj) {
            reportError("Designator is no object type on line ", line);
            return Tab.noObj;
        }
        
        int kind = designator.getKind();
        if((kind != Obj.Var)&&(kind != Obj.Fld)&&(kind != Obj.Elem)) {
            reportError("Designator is not of kind var or class field on line ", line);
            return Tab.noObj;
        }
        if(designator.getType() != Tab.intType) {
            reportError("Designator is not of type int on line ", line);
            return Tab.noObj;
        }
        if (designator.getKind() == Obj.Elem)
            Code.put(Code.dup2);
        Code.load(designator);
        Code.loadConst(1);
        Code.put(Code.sub);
        Code.store(designator);

        return designator;
    }
    
    public Obj resolveIdentificator(String ident, Obj designatorExtension, int line) {

        if(designatorExtension != null)
            return currentDesignator;
        else 
            return resolveIdentificator(ident, line);

    }
    
    public void setArrayExtension(String ident, int line) {
        Obj temp = Tab.find(ident);
        
        if(temp.equals(Tab.noObj))
            reportError("Undefined identifier" + ident, line);

        if(temp.getType().getKind() == Struct.Array)
            currentDesignator = temp;
    }
    
    public Obj resolveArray(int line) {
        if(currentDesignator==null)
            return Tab.noObj;
        if(!(currentDesignator.getType().getKind() == Struct.Array)){
            reportError("Sentence is not of kind array", line);
            return Tab.noObj;
        } 
        Code.load(currentDesignator);
        currentDesignator = new Obj(Obj.Elem,currentDesignator.getName(),new Struct(Struct.Array, currentDesignator.getType().getElemType()));

        return currentDesignator;
    }
    
    public Struct designatorCall(Obj designator, Obj ActPars, int line) {

        if(designator.getKind() != Obj.Meth) {
            reportError("Designator is not of kind Method", line);
            return Tab.noType;
        }
        reportInfo("Method of name " + designator.getName() + " detected ", line);

        int destinationAddress = designator.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(destinationAddress);

        if(designator.getType() != Tab.noType)
            Code.put(Code.pop);
 
        if(currentMethod.getName().equals("main")) 
            countElement(Position.GLOBAL_METHOD);
        
        return designator.getType();
    }
    
    public Struct assign(Obj des, Integer op, Struct expr, int line) {
        Struct res = null;
        if(des == null || expr == null)return Tab.noType;
        
        Struct desType = isArray(des.getType())?des.getType().getElemType():des.getType();
        Struct exprType = isArray(expr)?expr.getElemType():expr;
        
        if(des.getKind() == Obj.Con) {
            reportError("Left part of equation is a constant on line "+line);
            return Tab.noType;
        }
        if(!desType.assignableTo(exprType)){
            reportError("Types are incompatible on line "+line);
            return Tab.noType;
        }
           
        res = des.getType();
        if(isArray(des.getType()))
            reportInfo("Array element detected", line);

        if(op.intValue() == 0) {
            Code.store(des);
        } else {
            if(isArray(des.getType())) {
                insertIntoStackOnAssign(des);
            } else {
                Obj rightOperand = Tab.find("_sys_tmp");
                Code.store(rightOperand);
                Code.load(des);
                Code.load(rightOperand);
            }
            Code.put(op.intValue());
            Code.store(des);
        }
        return res;
    }
    
    public Obj assignLeft(Obj left, Integer opCode, Obj right, int line){
        Obj res = null;
        boolean leftIsArray = isArray(left.getType());
        boolean rightIsArray = isArray(right.getType());
        Struct leftType = (leftIsArray)?left.getType().getElemType():left.getType();
        Struct rightType = (rightIsArray)?right.getType().getElemType():right.getType();
        return null;
    }

    
    public Obj addLeft(Obj termList, Integer operation, Obj term, int line) {
        Obj res = null;
        if(termList == null || term == null)return Tab.noObj;
        boolean termIsArray = isArray(term.getType());
        Struct termType = (term.getType().getKind() == Struct.Array)?term.getType().getElemType():term.getType();
        Struct termListType = (termList.getType().getKind() == Struct.Array)?termList.getType().getElemType():termList.getType();
        if((term == Tab.noObj)||(termList == Tab.noObj)) {
            reportError("Term is not of any type. Line ", line);
        } else if((!termType.assignableTo(Tab.intType))||(!termListType.assignableTo(Tab.intType))) {
            reportError("Term is not of type int. Line ", line);
            res = Tab.noObj;
        } else {
            Code.put(operation.intValue());
            
            res = new Obj(term.getKind(),"", termType);
        }
        addOpLeftCount--;
        return res;
    }
    
    public Obj mulLeft(Obj term, Integer operation, Obj factor, int line) {		
        Obj res = null;
        if(factor == null || term == null)return Tab.noObj;
        boolean termIsArray = isArray(term.getType());
        boolean factorIsArray = isArray(factor.getType());
        Struct termType = (termIsArray)?term.getType().getElemType():term.getType();
        Struct factorType = (factorIsArray)?factor.getType().getElemType():factor.getType();
       
        if((term.getType() == Tab.noType) || (factor.getType() == Tab.noType)) {
            reportError("Operands are not of any type. Line ", line);
        } else if((!termType.assignableTo(Tab.intType)) || (!factorType.assignableTo(Tab.intType))) {
            reportError("Operands are not of type int. Line ", line);
            res = Tab.noObj;   
        } else {
            
            if(operation.intValue() != 1234){
                Code.put(operation.intValue());
            }else{
                Obj rightOperand = Tab.find("_sys_tmp");
                Code.store(rightOperand);
                Code.put(Code.dup);
                Code.put(Code.dup);
                Code.put(Code.mul);
                Code.put(Code.mul);
                Code.load(rightOperand);
                Code.load(rightOperand);
                Code.load(rightOperand);
                Code.put(Code.mul);
                Code.put(Code.mul);
                Code.put(Code.add);
            }
            
            
            res = new Obj(term.getKind(),"", termType);
        }
        mulOpLeftCount--;
        return res;
    }
    
        
    public Obj addRight(Obj term, Integer operation, Obj termList, int line) {
        Obj res = null;
        if(termList == null || term == null)return Tab.noObj;
        boolean termIsArray = isArray(term.getType());
        boolean termListIsArray = isArray(termList.getType());
        Struct termType = (term.getType().getKind() == Struct.Array)?term.getType().getElemType():term.getType();
        Struct termListType = (termList.getType().getKind() == Struct.Array)?termList.getType().getElemType():termList.getType();     
        if((term == Tab.noObj)||(termList == Tab.noObj)) {
            reportError("Term is not of any type. Line ", line);
        } else if((!termType.assignableTo(Tab.intType))||(!termListType.assignableTo(Tab.intType))) {
            reportError("Term is not of type int. Line ", line);
            res = Tab.noObj;
        } else {
            if(termListIsArray)
                Code.load(termList);
            if(termIsArray) {
                insertIntoStackOnMul(term);
            }
            Code.put(operation.intValue());
            Code.store(term);
            Code.load(term);
            
            res = new Obj(term.getKind(),"", termType);
        }
        addOpRightCount--;
        return res;
    }
    
    public Obj mulRight(Obj factor, Integer operation, Obj term, int line) {		
        Obj res = null;
        boolean termIsArray = isArray(term.getType());
        boolean factorIsArray = isArray(factor.getType());
        Struct termType = (termIsArray)?term.getType().getElemType():term.getType();
        Struct factorType = (factorIsArray)?factor.getType().getElemType():factor.getType();
        
        if((term.getType() == Tab.noType) || (factor.getType() == Tab.noType)) {
            reportError("Operands are not of any type. Line ", line);
        } else if((!termType.assignableTo(Tab.intType)) || (!factorType.assignableTo(Tab.intType))) {
            reportError("Operands are not of type int. Line ", line);
            res = Tab.noObj;   
        } else {
            if(termIsArray)
                Code.load(term);
            
            if(factorIsArray) 
                insertIntoStackOnMul(factor);

            Code.put(operation.intValue());
            Code.store(factor);
            Code.load(factor);
            
            res = new Obj(factor.getKind(),"", factorType);
        }
        mulOpRightCount--;
        return res;
    }
    
    public void checkTermListForArray(Obj termList, int line) {
        if(termList == null)
            return;
        
        if(isArray(termList.getType())&&inAssign&&fromDesignator&&addOpRightCount==0&&mulOpRightCount==0) {
            Code.load(termList);
            reportInfo("Array element detected. Line ", line);
        }
    }
    
    public boolean isArray(Struct item) {       
        return (item.getKind() == Struct.Array)?true:false;
    }
    
    public void insertIntoStackOnAssign(Obj des) {
        Obj rightOperand = Tab.find("_sys_tmp");
        Code.store(rightOperand);
        Code.put(Code.dup2);
        Code.load(des);
        Code.load(rightOperand);
    }
    
    public void insertIntoStackOnMul(Obj des) {   
        Obj rightOperand = Tab.find("_sys_tmp");
        Code.store(rightOperand);
        Code.put(Code.dup2);
        Code.put(Code.dup2); 
        Code.load(des);
        Code.load(rightOperand);
    }
    
    public void loadIfArray(Obj obj, int line){
        if(obj == null)return;
        if(isArray(obj.getType())) {
            Code.load(obj);  
            reportInfo("Array element detected",line); 
        } 
    }
    
//==============================================================================
//FACTOR
//==============================================================================

    public Obj loadInteger(Integer number, int line) {
        Obj temp = Tab.insert(Obj.Con, "", Tab.intType);
        temp.setAdr(number.intValue());
        reportInfo("Constant of value " + number + " detected", line);
        Code.load(temp);        
        return temp;
    }
    
    public Obj loadChar(Character ch, int line) {
        Obj temp = new Obj(Obj.Con, "", Tab.charType);
        temp.setAdr(ch.charValue());
        Tab.insert(temp.getKind(), temp.getName(), temp.getType());  
        reportInfo("Constant " + ch + " detected ", line);
        Code.load(temp);
        return temp;
    }
    
    public Obj loadBoolean(Boolean b, int line) {
        Obj boolObj = Tab.find("bool");
        Obj temp = new Obj(Obj.Con, "", boolObj.getType());
        Tab.insert(temp.getKind(), temp.getName(), temp.getType());
        temp.setAdr((b.booleanValue()==true)?1:0);
        reportInfo("Constant of value \"" + b + "\" has been detected on line ", line);
        Code.load(temp);
        return temp;
    }
    
    public Obj loadExpression(Struct expr, int line) {
        return new Obj(expr.getKind(), "", expr);
    }
    
    public Obj loadArray(Struct type, Struct expr, int line) {

        if(!Tab.intType.equals(expr)) {
            reportError("Expression must be of type int", line);
            return Tab.noObj;
        }
        Code.put(Code.newarray);
        if (type == Tab.charType) 
            Code.put(0); 
        else 
            Code.put(1);
            
        return new Obj(Obj.Elem,"",new Struct(Struct.Array, type));
    }
    
    public Obj loadClass(Struct type, int line) {
        if(!type.assignableTo(new Struct(Struct.Class))) {
            reportError("Element must be class", line);
            return Tab.noObj;
        }
        return new Obj(Struct.Class,"",new Struct(Struct.Class));
    }
    
    public Obj loadDesignator(Obj designator, int line) {
        if(designator == null)
            return null;
        if(designator == Tab.noObj)
            return Tab.noObj;
        
        if(!isArray(designator.getType())) 
            Code.load(designator);
        else if (isArray(designator.getType()) && !inAssign) 
            Code.load(designator);

        fromDesignator = true;
        return designator;
    }
    
    public Obj loadMethod(Obj designator, int line) {
        
        if(Obj.Meth != designator.getKind()) {
            reportError("Method " + designator.getName()+" is undefined", line);
            return Tab.noObj;
        }
        Obj temp = Tab.find(designator.getName());
        if(temp == null)
            return Tab.noObj;
        
        if(temp == null)
            reportError("Method is undefined", line);
        else if (temp.getType() == Tab.noType)
            reportError("Method does not return value", line);
        else {
            int address = designator.getAdr() - Code.pc;
            Code.put(Code.call);
            Code.put2(address);
        }
           
        return temp;
    }
}