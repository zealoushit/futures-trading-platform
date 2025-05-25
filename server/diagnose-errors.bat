@echo off
echo ========================================
echo 交易中台服务错误诊断工具
echo ========================================
echo.

echo 1. 检查Java环境...
java -version
if %ERRORLEVEL% neq 0 (
    echo [错误] Java未安装或配置错误
    echo 解决方案: 安装Java 8 JDK并配置JAVA_HOME环境变量
    echo.
) else (
    echo [正常] Java环境正常
    echo.
)

echo 2. 检查Maven环境...
mvn -version
if %ERRORLEVEL% neq 0 (
    echo [警告] Maven未安装
    echo 解决方案: 安装Maven 3.6+并配置PATH环境变量
    echo.
) else (
    echo [正常] Maven环境正常
    echo.
)

echo 3. 检查项目文件结构...
echo.
echo 核心文件检查:
if exist "pom.xml" (
    echo [✓] pom.xml 存在
) else (
    echo [✗] pom.xml 缺失
)

if exist "src\main\java\com\trading\TradingApplication.java" (
    echo [✓] 主启动类存在
) else (
    echo [✗] 主启动类缺失
)

if exist "src\main\resources\application.yml" (
    echo [✓] 配置文件存在
) else (
    echo [✗] 配置文件缺失
)

echo.
echo 4. 检查常见编译错误...
echo.

REM 检查pom.xml语法
echo 检查pom.xml语法...
findstr /C:"<n>" pom.xml >nul
if %ERRORLEVEL% equ 0 (
    echo [错误] pom.xml中存在错误的标签 ^<n^>，应该是 ^<name^>
    echo 解决方案: 修复pom.xml中的标签错误
) else (
    echo [正常] pom.xml语法正常
)

echo.
echo 5. 检查JNI相关问题...
echo.

if exist "src\main\java\com\trading\jni\FemasTraderApiMock.java" (
    echo [✓] 模拟JNI类存在 - 可以正常启动
) else (
    echo [✗] 模拟JNI类缺失 - 可能导致启动失败
)

if exist "lib\FemasTraderJNI.dll" (
    echo [✓] JNI库文件存在
) else (
    echo [警告] JNI库文件不存在 - 使用模拟模式运行
)

echo.
echo 6. 尝试编译测试...
echo.

if exist "mvn" (
    echo 尝试Maven编译...
    mvn clean compile -q
    if %ERRORLEVEL% equ 0 (
        echo [✓] 编译成功
    ) else (
        echo [✗] 编译失败，请查看详细错误信息
        echo.
        echo 运行以下命令查看详细错误:
        echo mvn clean compile
    )
) else (
    echo [跳过] Maven不可用，无法进行编译测试
)

echo.
echo 7. 常见问题解决方案...
echo.
echo 问题1: Java环境问题
echo   - 下载并安装Java 8 JDK
echo   - 配置JAVA_HOME环境变量
echo   - 将%%JAVA_HOME%%\bin添加到PATH
echo.
echo 问题2: Maven环境问题  
echo   - 下载并安装Maven 3.6+
echo   - 配置MAVEN_HOME环境变量
echo   - 将%%MAVEN_HOME%%\bin添加到PATH
echo.
echo 问题3: JNI库加载失败
echo   - 项目已配置为使用模拟模式
echo   - 可以正常启动和测试API接口
echo   - 实际部署时需要编译真实的JNI库
echo.
echo 问题4: 端口占用
echo   - 默认端口8080可能被占用
echo   - 修改application.yml中的server.port配置
echo.
echo 问题5: 配置文件错误
echo   - 检查application.yml语法
echo   - 确保缩进使用空格而不是Tab
echo.
echo ========================================
echo 诊断完成
echo ========================================
echo.
echo 如果所有检查都正常，可以尝试运行:
echo   mvn spring-boot:run
echo.
echo 或者在IDE中直接运行:
echo   com.trading.TradingApplication
echo.

pause
