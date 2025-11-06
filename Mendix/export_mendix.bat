@echo off
REM ============================================================================
REM Batch script to export Mendix database metadata to CaseTalk jcatalog format
REM ============================================================================

echo.
echo ========================================================================
echo Mendix to CaseTalk Metadata Exporter
echo ========================================================================
echo.

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java and try again
    pause
    exit /b 1
)

REM Configuration
set JDBC_URL=jdbc:postgresql://localhost:5432/mendix
set DB_USER=postgres
set DB_PASS=secret
set OUTPUT_FILE=mendix_export.jcatalog
set INCLUDE_SYSTEM=false

REM Ask user for configuration
echo Default configuration:
echo   JDBC URL: %JDBC_URL%
echo   Username: %DB_USER%
echo   Password: ******
echo   Output:   %OUTPUT_FILE%
echo.
set /p CONFIRM="Use default configuration? (Y/N): "

if /i "%CONFIRM%" NEQ "Y" (
    echo.
    echo Enter custom configuration:
    echo.
    set /p JDBC_URL="JDBC URL (e.g., jdbc:postgresql://localhost:5432/mendix): "
    set /p DB_USER="Database username: "
    set /p DB_PASS="Database password: "
    set /p OUTPUT_FILE="Output file name (e.g., mendix.jcatalog): "
    set /p INCLUDE_SYSTEM="Include system tables? (true/false): "
)

echo.
echo Exporting metadata...
echo ========================================================================

REM Compile Java file if needed
if not exist StandaloneMendixExporter.class (
    echo Compiling Java exporter...
    javac -cp ".;json-20231013.jar" StandaloneMendixExporter.java
    if errorlevel 1 (
        echo ERROR: Failed to compile Java exporter
        echo Make sure json-20231013.jar is in the same directory
        pause
        exit /b 1
    )
)

REM Run the exporter
java -cp ".;json-20231013.jar;postgresql-42.7.1.jar" StandaloneMendixExporter "%JDBC_URL%" "%DB_USER%" "%DB_PASS%" "%OUTPUT_FILE%" %INCLUDE_SYSTEM%

if errorlevel 1 (
    echo.
    echo ERROR: Export failed
    echo.
    echo Common issues:
    echo   - JDBC driver not found: Make sure the appropriate JDBC jar is in the directory
    echo     * PostgreSQL: postgresql-42.7.1.jar
    echo     * SQL Server: mssql-jdbc-12.4.2.jre11.jar
    echo     * MySQL: mysql-connector-j-8.2.0.jar
    echo   - Cannot connect: Check your JDBC URL, username, and password
    echo   - Database not accessible: Make sure the database server is running
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================================================
echo SUCCESS! Metadata exported to: %OUTPUT_FILE%
echo ========================================================================
echo.
echo Next steps:
echo   1. Copy %OUTPUT_FILE% to your CaseTalk project folder
echo      OR
echo      Copy to %%APPDATA%%\CaseTalk12\ for global access
echo.
echo   2. Open CaseTalk and go to Repository ^> SQL Browser (F12)
echo.
echo   3. The jcatalog file should appear in the connection tree
echo.
echo   4. Right-click on a table and select "Save to Repository"
echo      to import into your CaseTalk model
echo.
echo Press any key to open the output file location...
pause
explorer /select,"%OUTPUT_FILE%"
