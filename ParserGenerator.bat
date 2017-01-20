cd lib
DEL ..\src\rs\ac\bg\etf\pp1\sym.java
DEL ..\src\rs\ac\bg\etf\pp1\Parser.java
java -jar java-cup-11a.jar -destdir ..\src\rs\ac\bg\etf\pp1 -parser Parser ..\spec\parser.cup
pause