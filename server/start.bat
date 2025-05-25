@echo off
echo Starting Trading Middleware Service...

REM 设置Java环境变量
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_XXX
set PATH=%JAVA_HOME%\bin;%PATH%

REM 设置JVM参数
set JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.library.path=./lib

REM 创建必要的目录
if not exist "logs" mkdir logs
if not exist "flow" mkdir flow
if not exist "lib" mkdir lib

REM 检查Java版本
java -version
if %ERRORLEVEL% neq 0 (
    echo Error: Java not found or not properly configured
    pause
    exit /b 1
)

REM 编译项目
echo Compiling project...
call mvn clean compile
if %ERRORLEVEL% neq 0 (
    echo Error: Compilation failed
    pause
    exit /b 1
)

REM 运行应用
echo Starting application...
java %JAVA_OPTS% -jar target/trading-middleware-1.0.0.jar

pause
