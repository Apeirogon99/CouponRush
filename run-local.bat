@echo off
REM run-scenario.bat

if "%1"=="" (
    echo Usage: run-scenario.bat [1^|2^|3]
    echo   1: DB only
    echo   2: DB + Redis
    echo   3: DB + Redis + Kafka
    exit /b 1
)

set SCENARIO=scenario%1
set SCENARIO_PROFILE=scenario%1
set TEST_SCRIPT=scenario%1-test.js
set LOG_FILE=logs\scenario%1-%date:~0,4%%date:~5,2%%date:~8,2%-%time:~0,2%%time:~3,2%%time:~6,2%.log
set LOG_FILE=%LOG_FILE: =0%

REM 로그 디렉토리 생성
if not exist logs mkdir logs

echo ========================================
echo Starting Scenario %1
echo Log file: %LOG_FILE%
echo ========================================

REM 1. 정리
echo [1/3] Cleaning up previous containers...
docker-compose --profile %SCENARIO% down -v >> %LOG_FILE% 2>&1

REM 2. 빌드 및 실행 (로그 저장)
echo [2/3] Building and starting containers...
docker-compose --profile %SCENARIO% up -d --build >> %LOG_FILE% 2>&1

REM 컨테이너 상태 확인
echo [3/3] Checking container status...
docker-compose --profile %SCENARIO% ps

exit /b 1