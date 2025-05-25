# 交易中台服务 (Trading Middleware)

基于Java 8的交易中台服务，用于对接飞马交易API，为Vue+Electron前端提供交易功能。

## 项目架构

```
src/
├── main/
│   ├── java/com/trading/
│   │   ├── TradingApplication.java      # Spring Boot启动类
│   │   ├── config/                      # 配置类
│   │   │   ├── TradingConfig.java       # 交易配置
│   │   │   └── WebSocketConfig.java     # WebSocket配置
│   │   ├── controller/                  # REST控制器
│   │   │   └── TradingController.java   # 交易API控制器
│   │   ├── service/                     # 业务服务
│   │   │   └── TradingService.java      # 交易服务
│   │   ├── jni/                         # JNI接口
│   │   │   └── FemasTraderApi.java      # 飞马API封装
│   │   └── model/                       # 数据模型
│   │       └── ApiResponse.java         # 统一响应模型
│   └── resources/
│       └── application.yml              # 应用配置
└── test/                               # 测试代码
```

## 技术栈

- **Java 8** - 基础运行环境
- **Spring Boot 2.7.14** - Web框架（Java 8兼容版本）
- **WebSocket** - 实时通信
- **JNI** - 调用C++交易API
- **Maven** - 依赖管理

## 功能特性

### 1. 交易功能
- 用户登录/登出
- 报单录入
- 报单撤销
- 持仓查询
- 资金查询
- 成交查询

### 2. 实时推送
- 报单回报推送
- 成交回报推送
- 连接状态推送
- 错误信息推送

### 3. REST API
- `/api/trading/login` - 用户登录
- `/api/trading/logout` - 用户登出
- `/api/trading/order` - 下单
- `/api/trading/cancel` - 撤单
- `/api/trading/status` - 获取状态
- `/api/trading/health` - 健康检查

### 4. WebSocket推送
- `/topic/connection` - 连接状态
- `/topic/login` - 登录状态
- `/topic/orders` - 报单回报
- `/topic/trades` - 成交回报

## 配置说明

在 `application.yml` 中配置交易参数：

```yaml
trading:
  front-address: "tcp://127.0.0.1:17001"  # 交易前置机地址
  broker-id: "0001"                       # 经纪商代码
  user-id: "your_user_id"                 # 用户代码
  password: "your_password"               # 密码
  investor-id: "your_investor_id"         # 投资者代码
  app-id: "trading_app"                   # 应用标识
  auth-code: "your_auth_code"             # 认证码
  user-product-info: "TradingMiddleware"  # 产品信息
  flow-path: "./flow/"                    # 流文件路径
```

## JNI集成步骤

### 1. 生成JNI头文件

```bash
# 编译Java类
javac -cp "target/classes" src/main/java/com/trading/jni/FemasTraderApi.java

# 生成头文件
javah -cp "target/classes" -jni com.trading.jni.FemasTraderApi
```

### 2. 创建C++包装库

需要创建一个C++动态库 `FemasTraderJNI.dll`，包装飞马交易API：

```cpp
// 示例结构
#include "com_trading_jni_FemasTraderApi.h"
#include "USTPFtdcTraderApi.h"

class TraderSpiImpl : public CUstpFtdcTraderSpi {
    // 实现回调方法
};

// 实现JNI方法
JNIEXPORT jboolean JNICALL Java_com_trading_jni_FemasTraderApi_createTraderApi
  (JNIEnv *env, jobject obj, jstring flowPath) {
    // 创建API实例
}
```

### 3. 部署本地库

将以下文件放入 `java.library.path` 或项目根目录：
- `USTPtraderapi.dll` - 飞马交易API
- `FemasTraderJNI.dll` - JNI包装库

## 运行说明

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 运行服务

```bash
mvn spring-boot:run
```

或者：

```bash
java -jar target/trading-middleware-1.0.0.jar
```

### 3. 访问服务

- REST API: http://localhost:8080/api/trading/
- WebSocket: ws://localhost:8080/ws
- 健康检查: http://localhost:8080/api/trading/health

## 前端集成示例

### WebSocket连接

```javascript
// 连接WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // 订阅报单回报
    stompClient.subscribe('/topic/orders', function(message) {
        const order = JSON.parse(message.body);
        console.log('Order update:', order);
    });
    
    // 订阅成交回报
    stompClient.subscribe('/topic/trades', function(message) {
        const trade = JSON.parse(message.body);
        console.log('Trade update:', trade);
    });
});
```

### REST API调用

```javascript
// 登录
fetch('http://localhost:8080/api/trading/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    }
}).then(response => response.json())
  .then(data => console.log('Login result:', data));

// 下单
fetch('http://localhost:8080/api/trading/order', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        instrumentId: 'rb2405',
        direction: '0',      // 买
        offsetFlag: '0',     // 开仓
        price: 3500.0,
        volume: 1
    })
}).then(response => response.json())
  .then(data => console.log('Order result:', data));
```

## 性能优化

### 1. 连接池配置
- Tomcat线程池：最大200线程
- Redis连接池：最大8连接

### 2. 异步处理
- 使用 `CompletableFuture` 处理异步请求
- WebSocket推送采用非阻塞方式

### 3. 内存管理
- 使用 `ConcurrentHashMap` 管理并发状态
- 及时清理完成的异步请求

## 注意事项

1. **Java 8兼容性**：所有代码都使用Java 8语法和API
2. **线程安全**：使用线程安全的集合类和原子操作
3. **错误处理**：完善的异常处理和错误回调
4. **资源管理**：正确释放JNI资源和API连接
5. **配置管理**：敏感信息应使用环境变量或加密配置

## 开发计划

- [ ] 完成JNI C++包装库开发
- [ ] 添加行情API支持
- [ ] 实现数据持久化
- [ ] 添加风控模块
- [ ] 性能监控和日志分析
- [ ] 单元测试和集成测试
