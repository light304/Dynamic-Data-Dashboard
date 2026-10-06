@echo off
rem Windows launcher: double-click to start the dashboard.

cd /d "%~dp0"

where node >nul 2>nul
if errorlevel 1 (
    echo ERROR: Node.js was not found.
    echo Install it from https://nodejs.org and try again.
    pause
    exit /b 1
)

node launcher.js
