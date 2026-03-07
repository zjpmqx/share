@echo off
setlocal
cd /d "%~dp0"

set "BACKEND_PORT=18081"
set "FRONTEND_PORT=3001"

echo [1/6] 正在释放后端端口 %BACKEND_PORT% ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%BACKEND_PORT%" ^| findstr "LISTENING"') do (
  echo 正在结束后端进程 PID=%%p
  taskkill /F /PID %%p >nul 2>nul
)

echo [2/6] 正在释放前端端口 %FRONTEND_PORT% ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%FRONTEND_PORT%" ^| findstr "LISTENING"') do (
  echo 正在结束前端进程 PID=%%p
  taskkill /F /PID %%p >nul 2>nul
)

echo [3/6] 正在构建后端 ...
cd /d "%~dp0campus-secondhand-backend"
call mvn -DskipTests package
if errorlevel 1 (
  echo 后端构建失败，已停止启动。
  exit /b 1
)

echo [4/6] 正在构建前端 ...
cd /d "%~dp0campus-secondhand-web"
call npm run build
if errorlevel 1 (
  echo 前端构建失败，已停止启动。
  exit /b 1
)

echo [5/6] 正在启动后端 ...
start "Campus Backend" powershell -NoExit -ExecutionPolicy Bypass -File "%~dp0start-backend.ps1"
timeout /t 3 /nobreak >nul

echo [6/6] 正在启动前端 ...
start "Campus Frontend" powershell -NoExit -ExecutionPolicy Bypass -File "%~dp0start-frontend.ps1"

echo 启动完成。
endlocal
