@echo off
chcp 65001 >nul
rem ============================================================
rem  Huixuetong - expose the local dev server to the internet
rem
rem  Runs a Cloudflare quick tunnel in front of http://localhost:5173
rem  and prints a temporary https://xxx.trycloudflare.com URL.
rem  Anyone with that URL can open the site - no public IP, no domain,
rem  no port forwarding on the router.
rem
rem  Prereq : frontend running  (cd frontend ^&^& npm run dev)
rem  Stop   : Ctrl+C, or just close this window
rem  Note   : the URL changes every time you start it
rem ============================================================
cd /d "%~dp0.."

set "CF=%USERPROFILE%\tools\cloudflared.exe"
if not exist "%CF%" (
  echo [FAIL] cloudflared.exe not found at %CF%
  echo.
  echo        Download it once:
  echo        https://github.com/cloudflare/cloudflared/releases/latest
  echo        ^(file: cloudflared-windows-amd64.exe, rename to cloudflared.exe^)
  echo.
  pause
  exit /b 1
)

echo ============================================
echo  Public tunnel  ==^>  http://localhost:5173
echo ============================================
echo.

netstat -ano | findstr ":5173" | findstr LISTENING >nul
if errorlevel 1 (
  echo [WARN] Nothing is listening on 5173 right now.
  echo        Start the frontend first:  cd frontend ^&^& npm run dev
  echo        The tunnel will answer 502 until it is up.
  echo.
)

echo [..] Creating tunnel, wait ~5-10s for the URL below...
echo.

"%CF%" tunnel --url http://localhost:5173 --no-autoupdate

echo.
echo [OK] Tunnel closed.
pause
