@echo off
setlocal

set "BASE_DIR=%~dp0"
set "MVN_CMD="

if defined OPS_MAVEN_CMD (
    if exist "%OPS_MAVEN_CMD%" (
        set "MVN_CMD=%OPS_MAVEN_CMD%"
    )
)

if not defined MVN_CMD (
    if exist "D:\maven\apache-maven-3.9.9\bin\mvn.cmd" (
        set "MVN_CMD=D:\maven\apache-maven-3.9.9\bin\mvn.cmd"
    )
)

if not defined MVN_CMD (
    if defined MAVEN_HOME (
        if exist "%MAVEN_HOME%\bin\mvn.cmd" (
            set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
        )
    )
)

if not defined MVN_CMD (
    where mvn.cmd >nul 2>nul
    if %ERRORLEVEL%==0 set "MVN_CMD=mvn.cmd"
)

if not defined MVN_CMD (
    where mvn >nul 2>nul
    if %ERRORLEVEL%==0 set "MVN_CMD=mvn"
)

if not defined MVN_CMD (
    echo Maven executable not found. Set OPS_MAVEN_CMD or install Maven.
    exit /b 1
)

call "%MVN_CMD%" -f "%BASE_DIR%pom.xml" %*
exit /b %ERRORLEVEL%
