@echo off
echo ========================================
echo 编译测试脚本
echo ========================================
echo.

REM 检查Java环境
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [错误] Java未安装或未配置在PATH中
    pause
    exit /b 1
)

echo [信息] Java环境正常
echo.

REM 检查Maven环境
mvn -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [警告] Maven未安装，无法进行编译测试
    echo 请安装Maven后重试，或在IDE中直接运行项目
    pause
    exit /b 1
)

echo [信息] Maven环境正常
echo.

echo [信息] 开始编译项目...
mvn clean compile

if %ERRORLEVEL% equ 0 (
    echo.
    echo [成功] 编译成功！
    echo.
    echo 现在可以启动项目:
    echo   mvn spring-boot:run
    echo.
    echo 或者在IDE中运行主类:
    echo   com.trading.TradingApplication
    echo.
    
    set /p choice="是否立即启动项目? (y/n): "
    if /i "%choice%"=="y" (
        echo.
        echo [信息] 启动项目...
        mvn spring-boot:run
    )
) else (
    echo.
    echo [错误] 编译失败！
    echo 请检查上面的错误信息并修复后重试
    echo.
)

pause
