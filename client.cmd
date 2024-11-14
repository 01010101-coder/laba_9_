@echo off
if "%1"=="" (
goto error
) else (
if "%2"=="" goto error
)

start "Client %1" java -ea -esa csdev.client.ClientMain %*
goto :eof

:error
echo Invalid number of arguments
echo Syntax: nic username [host]
goto :eof
