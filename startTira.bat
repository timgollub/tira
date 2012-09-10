set CLASSPATH=bin;lib\jersey-bundle-1.12.jar;lib\jsr311-api-1.1.1.jar;lib\thirdparty-json-11.jar;
REM echo %CLASSPATH%
REM mkdir bin
REM javac -sourcepath src src\tira\*.java -d bin
java tira.TiraNode
