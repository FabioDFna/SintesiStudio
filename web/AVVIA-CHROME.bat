@echo off
chcp 65001 >nul
cd /d "%~dp0"

set "NODE="

if exist "%ProgramFiles%\cursor\resources\app\resources\helpers\node.exe" (
  set "NODE=%ProgramFiles%\cursor\resources\app\resources\helpers\node.exe"
)

if exist "%LocalAppData%\Programs\cursor\resources\app\resources\helpers\node.exe" (
  set "NODE=%LocalAppData%\Programs\cursor\resources\app\resources\helpers\node.exe"
)

where node >nul 2>&1
if %ERRORLEVEL%==0 set "NODE=node"

if "%NODE%"=="" (
  echo.
  echo  Node.js non trovato.
  echo  Installa da https://nodejs.org  ^(versione LTS^)
  echo  oppure apri questo progetto con Cursor e riprova.
  echo.
  pause
  exit /b 1
)

echo Avvio server SintesiStudio...
start "" "http://localhost:3000"
"%NODE%" server.js
pause
