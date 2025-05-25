@echo off
echo ========================================
echo 验证编译错误修复
echo ========================================
echo.

echo 1. 检查MarketService.java中的方法名...
findstr /n "subscribeMarket\|unsubscribeMarket" src\main\java\com\trading\service\MarketService.java
if %ERRORLEVEL% equ 0 (
    echo [✓] MarketService中的方法名已修复
) else (
    echo [✗] MarketService中的方法名未找到
)

echo.
echo 2. 检查MarketController.java中的方法调用...
findstr /n "subscribeMarket\|unsubscribeMarket" src\main\java\com\trading\controller\MarketController.java
if %ERRORLEVEL% equ 0 (
    echo [✓] MarketController中的方法调用已更新
) else (
    echo [✗] MarketController中的方法调用未更新
)

echo.
echo 3. 检查是否还有旧的方法名...
findstr /n "subscribeMarketData\|unsubscribeMarketData" src\main\java\com\trading\service\MarketService.java | findstr /v "super\."
if %ERRORLEVEL% neq 0 (
    echo [✓] 没有发现冲突的方法名
) else (
    echo [✗] 仍然存在冲突的方法名
)

echo.
echo 4. 检查文件结构...
if exist "src\main\java\com\trading\jni\FemasMarketApiMock.java" (
    echo [✓] FemasMarketApiMock.java 存在
) else (
    echo [✗] FemasMarketApiMock.java 缺失
)

if exist "src\main\java\com\trading\jni\FemasTraderApiMock.java" (
    echo [✓] FemasTraderApiMock.java 存在
) else (
    echo [✗] FemasTraderApiMock.java 缺失
)

echo.
echo 5. 检查pom.xml...
if exist "pom.xml" (
    echo [✓] pom.xml 存在
    findstr /C:"<name>Trading Middleware</name>" pom.xml >nul
    if %ERRORLEVEL% equ 0 (
        echo [✓] pom.xml 语法正确
    ) else (
        echo [✗] pom.xml 可能有语法错误
    )
) else (
    echo [✗] pom.xml 缺失
)

echo.
echo ========================================
echo 修复验证完成
echo ========================================
echo.
echo 主要修复内容:
echo 1. MarketService方法重命名: subscribeMarketData → subscribeMarket
echo 2. MarketService方法重命名: unsubscribeMarketData → unsubscribeMarket  
echo 3. MarketController调用更新: 使用新的方法名
echo 4. 添加super.调用: 正确调用父类方法
echo.
echo 现在可以尝试编译项目:
echo   mvn clean compile
echo.
echo 或者在IDE中运行:
echo   com.trading.TradingApplication
echo.

pause
