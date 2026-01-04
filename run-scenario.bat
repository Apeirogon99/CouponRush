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
set TEST_SCRIPT=load-test.js
set LOG_FILE=logs\scenario%1-%date:~0,4%%date:~5,2%%date:~8,2%-%time:~0,2%%time:~3,2%%time:~6,2%.log
set LOG_FILE=%LOG_FILE: =0%

REM 로그 디렉토리 생성
if not exist logs mkdir logs

echo ========================================
echo Starting Scenario %1
echo Log file: %LOG_FILE%
echo ========================================

REM 1. 정리
echo [1/6] Cleaning up previous containers...
docker-compose --profile %SCENARIO% --profile app down -v >> %LOG_FILE% 2>&1

REM 2. 빌드 및 실행 (로그 저장)
echo [2/6] Building and starting containers...
docker-compose --profile %SCENARIO% --profile app up -d --build >> %LOG_FILE% 2>&1

REM 컨테이너 상태 확인
echo [3/6] Checking container status...
docker-compose --profile %SCENARIO% --profile app ps

REM 초기 로그 확인 (5초간)
echo [4/6] Checking initial logs...
timeout /t 5 /nobreak >nul
docker-compose --profile %SCENARIO% --profile app logs app

REM 3. 헬스체크
echo [5/6] Waiting for application to be ready...

echo Application is ready!

echo [6/6] Running K6 load test...
docker run --rm ^
    --network host ^
    -v "%cd%/k6:/scripts" ^
    -p 5665:5665 ^
    grafana/k6:latest ^
    run --out web-dashboard=dashboard.html /scripts/%TEST_SCRIPT%

set K6_EXIT_CODE=%errorlevel%

REM 5. 결과
echo.
if %K6_EXIT_CODE% equ 0 (
    echo ========================================
    echo SUCCESS: Scenario %1 passed!
    echo ========================================
) else (
    echo ========================================
    echo FAILED: Scenario %1 test failed!
    echo ========================================
)

echo Cleaning up containers...
docker-compose --profile %SCENARIO% --profile app down -v >> %LOG_FILE% 2>&1

exit /b %K6_EXIT_CODE%

:show_logs_and_exit
echo.
echo ========================================
echo FULL APPLICATION LOGS:
echo ========================================
docker-compose --profile %SCENARIO% --profile app logs app
echo.
echo ========================================
echo CONTAINER STATUS:
echo ========================================
docker-compose --profile %SCENARIO% --profile app ps
echo.
echo Logs saved to: %LOG_FILE%
echo.
echo Cleaning up...
docker-compose --profile %SCENARIO% --profile app down -v >> %LOG_FILE% 2>&1
exit /b 1