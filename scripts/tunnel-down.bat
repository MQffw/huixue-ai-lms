@echo off
chcp 65001 >nul
rem ============================================================
rem  Huixuetong - stop the public tunnel started by tunnel.bat
rem
rem  tunnel.bat runs cloudflared in the foreground, so normally
rem  Ctrl+C in that window (or closing it) is enough to stop it.
rem  Use this script when the window is gone but the process
rem  survived - e.g. the console was force-closed, or the tunnel
rem  was started from an editor terminal.
rem
rem  Stopping the tunnel does NOT stop the frontend or backend.
rem ============================================================
cd /d "%~dp0.."

setlocal enabledelayedexpansion
set "FOUND=0"

for /f "tokens=2 delims=," %%p in ('tasklist /fi "imagename eq cloudflared.exe" /fo csv /nh 2^>nul') do (
  set "FOUND=1"
  set "TUNNEL_PID=%%~p"
  echo [..] Stopping cloudflared PID !TUNNEL_PID!
  taskkill /pid !TUNNEL_PID! /f >nul 2>&1
)

if "!FOUND!"=="0" (
  echo [OK] No cloudflared process running - tunnel is already closed.
) else (
  echo [OK] Tunnel closed. The public https://*.trycloudflare.com URL is now dead.
  echo      Frontend and backend are untouched.
)

if not "%NOPAUSE%"=="1" pause
exit /b 0
