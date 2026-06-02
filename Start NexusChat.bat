@echo off
title NexusChat Server
color 0B
cd /d "%~dp0"

echo.
echo  ============================================
echo    NexusChat - Starting...
echo  ============================================
echo.

REM --- Java 21 (edit path if installed elsewhere) ---
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] Java 21 not found at:
    echo   %JAVA_HOME%
    echo.
    echo Install JDK 21 from https://adoptium.net/
    echo Then edit JAVA_HOME in this file if needed.
    echo.
    pause
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM --- Build JAR if missing ---
set "JAR=target\multithreaded-chat-1.0.0.jar"
if not exist "%JAR%" (
    echo [INFO] First run - building app (may take 1-2 minutes)...
    echo.
    set "MVN="
    if exist ".mvn\wrapper\maven-wrapper.jar" set "MVN=mvnw.cmd"
    if "%MVN%"=="" if exist "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.1\plugins\maven\lib\maven3\bin\mvn.cmd" (
        set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.1\plugins\maven\lib\maven3\bin\mvn.cmd"
    )
    if "%MVN%"=="" (
        echo [ERROR] Maven not found. Install Maven or open project in IntelliJ once.
        pause
        exit /b 1
    )
    call "%MVN%" clean package -DskipTests -q
    if errorlevel 1 (
        echo [ERROR] Build failed.
        pause
        exit /b 1
    )
    echo [OK] Build complete.
    echo.
)

REM --- Optional: set Gemini API key here ---
REM set "GEMINI_API_KEY=your-key-here"

REM --- Open browser after server starts ---
start "" cmd /c "ping 127.0.0.1 -n 9 >nul && start http://localhost:8080"

echo [INFO] Server running at http://localhost:8080
echo [INFO] Login: demo / demo123
echo [INFO] Press Ctrl+C to stop the server.
echo.

java -jar "%JAR%"

echo.
echo Server stopped.
pause
