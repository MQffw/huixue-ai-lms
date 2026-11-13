@echo off
chcp 65001 >nul
rem ============================================================
rem  Huixuetong - stop dev middleware
rem
rem    dev-down.bat           stop containers, KEEP data (volumes survive)
rem    dev-down.bat stop      same as above
rem    dev-down.bat reset     DESTROY data and re-init from deploy/mysql-init
rem ============================================================
cd /d "%~dp0.."

if /i "%~1"=="reset" goto reset

echo [..] Stopping containers (data is kept in volumes)...
docker compose -f docker-compose.dev.yml down
echo [OK] Stopped. Run dev-up.bat to start again.
echo.
if /i not "%NOPAUSE%"=="1" pause
exit /b 0

:reset
echo.
echo ************************************************************
echo  WARNING: this DELETES hxt-dev-mysql and hxt-dev-redis volumes.
echo  All local test data will be lost and re-initialized from
echo  deploy\mysql-init\*.sql on the next start.
echo ************************************************************
echo.
choice /c YN /m "Continue"
if errorlevel 2 (
  echo [OK] Cancelled, nothing changed.
  pause
  exit /b 0
)

docker compose -f docker-compose.dev.yml down -v
echo [OK] Volumes removed. Run dev-up.bat to rebuild the database.
echo.
pause
exit /b 0
