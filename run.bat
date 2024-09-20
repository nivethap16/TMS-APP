@echo off

rem Set the classpath to include the current directory and all JARs in the lib directory
set CLASSPATH=.;lib\*;

rem Compile the Java source file
javac -cp "%CLASSPATH%" src\RunProgram.java -d .

rem Run the compiled Java program
java -cp "%CLASSPATH%" RunProgram
