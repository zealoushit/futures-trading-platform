@echo off
echo ========================================
echo 交易中台服务启动 (使用Java直接运行)
echo ========================================
echo.

REM 检查Java环境
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [错误] Java未安装或未配置在PATH中
    echo 请安装Java 8 JDK并配置环境变量
    echo.
    pause
    exit /b 1
)

echo [信息] Java环境检查通过
echo.

REM 创建必要的目录
if not exist "target" mkdir target
if not exist "target\classes" mkdir target\classes
if not exist "logs" mkdir logs
if not exist "flow" mkdir flow

echo [信息] 正在编译Java源码...

REM 设置类路径 - 使用Spring Boot的依赖
set SPRING_BOOT_VERSION=2.7.14
set CLASSPATH=.

REM 如果有Maven，尝试使用Maven编译
mvn -version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [信息] 使用Maven编译项目...
    mvn clean compile
    if %ERRORLEVEL% neq 0 (
        echo [错误] Maven编译失败
        pause
        exit /b 1
    )
    
    echo [信息] 启动Spring Boot应用...
    mvn spring-boot:run
) else (
    echo [警告] Maven未安装，无法编译项目
    echo.
    echo 请安装以下环境后重试:
    echo 1. Java 8 JDK
    echo 2. Maven 3.6+
    echo.
    echo 或者使用IDE (如IntelliJ IDEA) 直接运行项目
    echo.
    echo 主类: com.trading.TradingApplication
    echo.
    pause
)

echo.
echo 按任意键退出...
pause >nul
