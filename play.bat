@echo off
cd /d "%~dp0"
set FXPATH=lib\javafx-17.0.12\lib

echo === Compiling ===
javac --module-path "%FXPATH%" --add-modules javafx.controls,javafx.graphics -d target src\*.java
if errorlevel 1 (
  echo Compile failed
  pause
  exit /b 1
)

echo === Running ===
java --module-path "%FXPATH%" --add-modules javafx.controls,javafx.graphics -cp target SpaceGameApp
pause
