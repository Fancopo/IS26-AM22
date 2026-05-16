@echo off
REM Avvia il server MESOS. Richiede server.jar nella stessa cartella.
cd /d "%~dp0"

where java >nul 2>nul
if %ERRORLEVEL%==0 (
    set "JAVA_CMD=java"
) else if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java"
) else (
    echo Java non trovato. Installare Java 25+ e aggiungerlo al PATH,
    echo oppure impostare la variabile d'ambiente JAVA_HOME.
    pause >nul
    exit /b 1
)

"%JAVA_CMD%" -Dfile.encoding=UTF-8 -jar server.jar
echo.
echo Server terminato. Premere un tasto per chiudere.
pause >nul
