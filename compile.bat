@echo off
REM Compile all Java files in the project
setlocal enabledelayedexpansion

set JAVA_HOME=C:\Program Files\Java\jdk-21
set CLASSPATH=
set SRC_DIR=src\main\java
set OUT_DIR=bin\src\main\java

echo Compiling Java files...

REM Find all Java files and compile them
for /r "%SRC_DIR%" %%F in (*.java) do (
    echo Compiling: %%F
    "%JAVA_HOME%\bin\javac" -d "%OUT_DIR%" -sourcepath "%SRC_DIR%" "%%F"
)

echo Compilation complete!
pause
