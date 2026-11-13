@echo off
chcp 65001 >nul
rem ============================================================
rem  Huixuetong - start dev middleware (MySQL 8 + Redis 7)
rem  Double click this file. It will:
rem    1. wait for Docker Desktop to be ready (up to 120s)
rem    2. start hxt-dev-mysql + hxt-dev-redis
rem    3. wait until MySQL reports healthy
rem  Data lives in named volumes, "down" does NOT lose it.
rem ============================================================
cd /d "%~dp0.."

echo ============================================
echo  Huixuetong dev middleware
echo ============================================
echo.

set /a N=0
:wait_engine
docker info >nul 2>&1
if not errorlevel 1 goto engine_ok
if %N%==0 echo [..] Docker Desktop not ready, waiting up to 120s...
set /a N+=1
if %N% GEQ 60 goto engine_fail
timeout /t 2 /nobreak >nul
goto wait_engine

:engine_fail
echo.
echo [FAIL] Docker engine is not available.
echo        Start Docker Desktop (wait until the whale icon stops spinning), then run this again.
echo.
pause
exit /b 1

:engine_ok
echo [OK] Docker engine ready.
echo.

echo [..] Starting containers...
docker compose -f docker-compose.dev.yml up -d
if errorlevel 1 goto compose_fail

set /a M=0
:wait_mysql
set MS=
for /f "tokens=*" %%s in ('docker inspect -f "{{.State.Health.Status}}" hxt-dev-mysql 2^>nul') do set MS=%%s
if "%MS%"=="healthy" goto mysql_ok
set /a M+=1
if %M% GEQ 60 goto mysql_slow
timeout /t 2 /nobreak >nul
goto wait_mysql

:mysql_slow
echo.
echo [WARN] MySQL is still not healthy after 120s. Last 30 log lines:
docker compose -f docker-compose.dev.yml logs --tail=30 mysql
goto done

:mysql_ok
echo [OK] MySQL healthy.

:done
echo.
docker compose -f docker-compose.dev.yml ps
echo.
echo --------------------------------------------
echo  MySQL : 127.0.0.1:3307   huixuetong / huixuetong123456
echo  Redis : 127.0.0.1:6379   no password
echo --------------------------------------------
echo.
echo Next steps:
echo   1. Run HuixuetongApplication in IDEA (no extra -D flags needed)
echo   2. cd frontend ^&^& npm run dev
echo.
if /i not "%NOPAUSE%"=="1" pause
exit /b 0

:compose_fail
echo.
echo [FAIL] docker compose up failed. Logs:
docker compose -f docker-compose.dev.yml logs --tail=50 mysql
echo.
pause
exit /b 1
