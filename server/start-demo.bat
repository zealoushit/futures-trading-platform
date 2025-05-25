@echo off
echo ========================================
echo 交易中台服务启动脚本
echo ========================================
echo.

REM 检查Java环境
echo 检查Java环境...
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [警告] Java未安装或未配置在PATH中
    echo 请安装Java 8 JDK并配置环境变量
    echo.
    echo 下载地址: https://adoptopenjdk.net/
    echo.
    echo 模拟启动服务...
    goto MOCK_START
)

REM 检查Maven
echo 检查Maven环境...
mvn -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [警告] Maven未安装或未配置在PATH中
    echo 请安装Maven 3.6+并配置环境变量
    echo.
    echo 下载地址: https://maven.apache.org/download.cgi
    echo.
    goto MOCK_START
)

REM 实际启动
echo 编译项目...
mvn clean compile
if %ERRORLEVEL% neq 0 (
    echo [错误] 项目编译失败
    pause
    exit /b 1
)

echo.
echo 启动服务...
mvn spring-boot:run
goto END

:MOCK_START
echo.
echo ========================================
echo 模拟服务启动 (演示模式)
echo ========================================
echo.
echo [INFO] 交易中台服务正在启动...
echo [INFO] 服务地址: http://localhost:8080
echo [INFO] API文档: API_DOCUMENTATION.md
echo [INFO] WebSocket: ws://localhost:8080/ws
echo.
echo 可用的API接口:
echo.
echo 📊 交易接口:
echo   GET  /api/trading/status     - 获取连接状态
echo   POST /api/trading/login      - 用户登录
echo   POST /api/trading/order      - 下单
echo   POST /api/trading/cancel     - 撤单
echo   GET  /api/trading/position   - 查询持仓
echo   GET  /api/trading/account    - 查询资金
echo   GET  /api/trading/orders     - 查询报单
echo   GET  /api/trading/trades     - 查询成交
echo.
echo 📈 行情接口:
echo   GET  /api/market/status      - 获取行情状态
echo   POST /api/market/login       - 行情登录
echo   POST /api/market/subscribe   - 订阅行情
echo   POST /api/market/unsubscribe - 退订行情
echo   GET  /api/market/instrument/{id} - 查询合约
echo.
echo 🔄 WebSocket推送:
echo   /topic/connection           - 连接状态
echo   /topic/login               - 登录状态
echo   /topic/orders              - 报单回报
echo   /topic/trades              - 成交回报
echo   /topic/market/data         - 行情数据
echo.
echo 🛠️ 测试命令:
echo   curl -X GET http://localhost:8080/api/trading/status
echo   curl -X POST http://localhost:8080/api/trading/login
echo.
echo ========================================
echo 前端集成指南
echo ========================================
echo.
echo 1. JavaScript/TypeScript 集成:
echo    参考 frontend-integration-example.js
echo.
echo 2. Vue.js 集成:
echo    参考 API_DOCUMENTATION.md 中的Vue示例
echo.
echo 3. WebSocket连接:
echo    const socket = new SockJS('http://localhost:8080/ws');
echo    const stompClient = Stomp.over(socket);
echo.
echo 4. REST API调用:
echo    fetch('http://localhost:8080/api/trading/status')
echo.
echo ========================================
echo 开发环境配置
echo ========================================
echo.
echo 要启动实际服务，请安装以下环境:
echo.
echo 1. Java 8 JDK
echo    下载: https://adoptopenjdk.net/
echo    配置: JAVA_HOME 环境变量
echo.
echo 2. Maven 3.6+
echo    下载: https://maven.apache.org/download.cgi
echo    配置: PATH 环境变量
echo.
echo 3. 飞马交易API
echo    配置: application.yml 中的连接参数
echo.
echo 4. 编译JNI库 (可选)
echo    运行: build-jni.bat
echo.
echo ========================================
echo 项目文件说明
echo ========================================
echo.
echo 📁 核心文件:
echo   API_DOCUMENTATION.md        - 完整API接口文档
echo   frontend-integration-example.js - 前端集成示例
echo   JNI_INTEGRATION_GUIDE.md    - JNI集成指南
echo.
echo 📁 Java源码:
echo   src/main/java/com/trading/   - Java业务代码
echo   src/main/resources/          - 配置文件
echo.
echo 📁 JNI相关:
echo   jni/cpp/                     - C++包装代码
echo   build-jni.bat               - JNI编译脚本
echo.
echo 📁 配置文件:
echo   pom.xml                     - Maven项目配置
echo   application.yml             - 应用配置
echo.

:END
echo.
echo 按任意键退出...
pause >nul
