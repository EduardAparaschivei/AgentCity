@echo off
setlocal enabledelayedexpansion
cd /d %~dp0

TITLE CitySim Advanced Launcher

REM ==========================================
REM CONFIGURARE NUME FISIERE
REM ==========================================
set "JAR_NAME=CitySim.jar"
set "PYTHON_SCRIPT=llm_service\server_llm.py"

REM -----------------------
REM 1. Check Java
REM -----------------------
echo [1/5] Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
  echo [EROARE] Java nu este instalat sau nu este in PATH.
  pause
  exit /b 1
)

REM -----------------------
REM 2. Check Python launcher
REM -----------------------
echo [2/5] Checking Python...
where py >nul 2>&1
if errorlevel 1 (
  where python >nul 2>&1
  if errorlevel 1 (
    echo [EROARE] Python nu este instalat.
    echo Instaleaza Python 3.10+ si bifeaza "Add to PATH".
    pause
    exit /b 1
  ) else (
    set "PYTHON=python"
  )
) else (
  REM Preferam 'py -3' pentru a forta versiunea 3
  set "PYTHON=py -3"
)

REM -----------------------
REM 3. Paths Setup
REM -----------------------
REM Folosim folderul curent (.) pentru ca acolo ai fisierele
set "ROOT_DIR=%~dp0"
set "VENV_DIR=%ROOT_DIR%.venv"
set "VENV_PY=%VENV_DIR%\Scripts\python.exe"
set "REQ=%ROOT_DIR%requirements.txt"
set "ENVFILE=%ROOT_DIR%.env"

REM -----------------------
REM 4. Ensure venv exists
REM -----------------------
if not exist "%VENV_PY%" (
  echo [INFO] Creating python venv...
  %PYTHON% -m venv "%VENV_DIR%"
  if errorlevel 1 (
    echo [EROARE] Nu am putut crea folderul .venv.
    pause
    exit /b 1
  )
)

REM -----------------------
REM 5. Install requirements
REM -----------------------
if exist "%REQ%" (
  echo [INFO] Checking/Installing requirements...
  "%VENV_PY%" -m pip install --upgrade pip -q
  "%VENV_PY%" -m pip install -r "%REQ%" -q
  if errorlevel 1 (
    echo [EROARE] Instalarea dependentelor a esuat.
    pause
    exit /b 1
  )
) else (
  echo [WARN] Nu am gasit requirements.txt. Sarim peste instalare.
)

REM -----------------------
REM 6. Check .env (Optional)
REM -----------------------
if not exist "%ENVFILE%" (
  echo [INFO] Nu am gasit fisierul .env.
  echo Se va crea un fisier .env gol.
  type nul > "%ENVFILE%"
)

REM -----------------------
REM 6.5. UPDATE API KEY (NOU)
REM -----------------------
echo.
echo ========================================================
echo  CONFIGURARE GEMINI API
echo ========================================================
echo Daca doresti sa schimbi cheia API din server_llm.py, introdu-o mai jos.
echo Daca vrei sa pastrezi cheia existenta, apasa doar ENTER.
echo.
set /p "USER_API_KEY=Introdu API Key: "

if not "%USER_API_KEY%"=="" (
    echo [INFO] Actualizez cheia in %PYTHON_SCRIPT%...
    
    REM Folosim PowerShell pentru a inlocui textul in fisier fara a strica formatarea
    powershell -Command "(Get-Content '%PYTHON_SCRIPT%') -replace 'API_KEY = \".*\"', 'API_KEY = \"%USER_API_KEY%\"' | Set-Content '%PYTHON_SCRIPT%'"
    
    echo [OK] Cheia a fost actualizata cu succes!
) else (
    echo [INFO] Se pastreaza cheia existenta.
)
echo.

REM -----------------------
REM 7. Start Python LLM service
REM -----------------------
echo [START] Pornire Server AI...
REM /min porneste fereastra minimizata
start "CitySim Brain" /min "%VENV_PY%" "%PYTHON_SCRIPT%"

REM Asteptam putin sa porneasca Flask
timeout /t 5 >nul

REM -----------------------
REM 8. Start Java app
REM -----------------------
echo [START] Pornire Aplicatie Java...
if exist "%JAR_NAME%" (
    java -jar "%JAR_NAME%"
) else (
    echo [EROARE] Nu gasesc fisierul %JAR_NAME%!
    echo Asigura-te ca ai generat Artifact-ul din IntelliJ.
    pause
)

REM La iesirea din Java, scriptul se termina.
REM Serverul Python ramane deschis in background.
echo.
echo Aplicatia s-a inchis.