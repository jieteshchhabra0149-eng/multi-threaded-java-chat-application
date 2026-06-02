@echo off
title NexusChat Server
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -jar "target\multithreaded-chat-1.0.0.jar"
pause
