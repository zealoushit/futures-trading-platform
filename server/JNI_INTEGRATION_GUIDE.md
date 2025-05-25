# 飞马交易API JNI集成指南

## 概述

本指南详细说明如何将飞马交易API通过JNI集成到Java中台服务中，为Vue+Electron前端提供完整的交易功能。

## 架构图

```
┌─────────────────┐    WebSocket/REST    ┌─────────────────┐    JNI    ┌─────────────────┐
│  Vue + Electron │ ◄─────────────────► │  Java Middleware │ ◄────────► │   Femas C++ API │
│     Frontend    │                     │     Service     │            │   (USTPtraderapi)│
└─────────────────┘                     └─────────────────┘            └─────────────────┘
```

## 功能特性

### 🔄 **交易功能**
- ✅ 用户认证和登录/登出
- ✅ 报单录入和撤销
- ✅ 实时报单和成交回报
- ✅ 持仓、资金、报单、成交查询
- ✅ 合约信息查询

### 📈 **行情功能**
- ✅ 行情登录和连接管理
- ✅ 实时行情订阅/退订
- ✅ 五档深度行情推送
- ✅ 合约信息查询

### 🌐 **前端接口**
- ✅ REST API接口
- ✅ WebSocket实时推送
- ✅ 跨域支持(CORS)
- ✅ 统一错误处理

## 文件结构

```
project/
├── src/main/java/com/trading/
│   ├── jni/
│   │   ├── FemasTraderApi.java      # 交易API JNI接口
│   │   └── FemasMarketApi.java      # 行情API JNI接口
│   ├── service/
│   │   ├── TradingService.java      # 交易服务
│   │   └── MarketService.java       # 行情服务
│   └── controller/
│       ├── TradingController.java   # 交易REST控制器
│       └── MarketController.java    # 行情REST控制器
├── jni/
│   ├── headers/                     # JNI生成的头文件
│   └── cpp/
│       ├── FemasTraderJNI.h        # C++包装头文件
│       └── FemasTraderJNI.cpp      # C++包装实现
├── lib/                            # 本地库文件
│   ├── FemasTraderJNI.dll         # JNI包装库
│   ├── USTPtraderapi.dll          # 飞马交易API
│   └── USTPmdapi.dll              # 飞马行情API
└── frontend-integration-example.js # 前端集成示例
```

## 安装和配置

### 1. 环境要求

- **Java 8 JDK**
- **Visual Studio 2019** (或兼容的C++编译器)
- **飞马交易API V1.32** 
- **Maven 3.6+**

### 2. 配置飞马API路径

编辑 `build-jni.bat` 文件，设置正确的路径：

```batch
set FEMAS_API_PATH=C:\Femas\1.32飞马交易api\FEMF_V1.32.r2_03a_Api_Win64_20231025_09c84740d\femasapi
```

### 3. 编译JNI库

```bash
# 生成JNI头文件并编译C++库
.\build-jni.bat
```

### 4. 配置连接参数

编辑 `src/main/resources/application.yml`：

```yaml
trading:
  front-address: "tcp://your-trading-server:17001"
  md-address: "tcp://your-market-server:17002"
  broker-id: "your-broker-id"
  user-id: "your-user-id"
  password: "your-password"
  investor-id: "your-investor-id"
  auth-code: "your-auth-code"
  flow-path: "./flow/"
```

## API接口文档

### 交易API

#### REST接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/trading/status` | 获取连接状态 |
| POST | `/api/trading/login` | 用户登录 |
| POST | `/api/trading/logout` | 用户登出 |
| POST | `/api/trading/order` | 下单 |
| POST | `/api/trading/cancel` | 撤单 |
| GET | `/api/trading/position` | 查询持仓 |
| GET | `/api/trading/account` | 查询资金 |
| GET | `/api/trading/orders` | 查询报单 |
| GET | `/api/trading/trades` | 查询成交 |

#### WebSocket推送

| 主题 | 描述 |
|------|------|
| `/topic/connection` | 连接状态变化 |
| `/topic/login` | 登录状态变化 |
| `/topic/orders` | 报单回报 |
| `/topic/trades` | 成交回报 |

### 行情API

#### REST接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/market/status` | 获取行情连接状态 |
| POST | `/api/market/login` | 行情登录 |
| POST | `/api/market/subscribe` | 订阅行情 |
| POST | `/api/market/unsubscribe` | 退订行情 |
| GET | `/api/market/instrument/{id}` | 查询合约 |

#### WebSocket推送

| 主题 | 描述 |
|------|------|
| `/topic/market/connection` | 行情连接状态 |
| `/topic/market/data` | 实时行情数据 |
| `/topic/market/data/{instrumentId}` | 特定合约行情 |

## 使用示例

### 1. 下单示例

```javascript
// REST API下单
const orderData = {
    instrumentId: 'rb2405',
    direction: '0',      // 买
    offsetFlag: '0',     // 开仓
    price: 3500.0,
    volume: 1
};

const response = await fetch('http://localhost:8080/api/trading/order', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(orderData)
});

const result = await response.json();
console.log('下单结果:', result);
```

### 2. 订阅行情示例

```javascript
// 订阅行情
await fetch('http://localhost:8080/api/market/subscribe', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ instruments: ['rb2405', 'cu2405'] })
});

// WebSocket接收行情
stompClient.subscribe('/topic/market/data', (message) => {
    const marketData = JSON.parse(message.body);
    console.log('行情数据:', marketData.data);
});
```

### 3. 完整前端集成

参考 `frontend-integration-example.js` 文件，包含：
- WebSocket连接管理
- REST API客户端
- Vue组件示例
- 错误处理

## 部署说明

### 1. 启动Java服务

```bash
# 设置库路径
set JAVA_OPTS=-Djava.library.path=./lib

# 启动服务
mvn spring-boot:run
```

### 2. 验证服务

```bash
# 健康检查
curl http://localhost:8080/api/trading/health

# 获取状态
curl http://localhost:8080/api/trading/status
```

### 3. 前端连接

```javascript
// WebSocket连接
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('连接成功:', frame);
});
```

## 故障排除

### 1. JNI库加载失败

**错误**: `UnsatisfiedLinkError: no FemasTraderJNI in java.library.path`

**解决**:
- 确保 `lib/` 目录包含所有DLL文件
- 设置 `-Djava.library.path=./lib`
- 检查DLL依赖关系

### 2. 连接失败

**错误**: 交易前置机连接失败

**解决**:
- 检查网络连接
- 验证前置机地址和端口
- 确认防火墙设置
- 检查认证信息

### 3. 编译错误

**错误**: C++编译失败

**解决**:
- 检查Visual Studio安装
- 验证飞马API路径
- 确认头文件和库文件存在
- 检查编译器版本兼容性

## 性能优化

### 1. 连接池配置

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
```

### 2. 内存管理

- 使用 `ConcurrentHashMap` 管理并发状态
- 及时清理完成的异步请求
- 合理设置JVM堆内存

### 3. 网络优化

- 启用WebSocket压缩
- 合理设置心跳间隔
- 使用连接池复用

## 安全考虑

### 1. 认证和授权

- 实现用户认证机制
- 添加API访问控制
- 使用HTTPS加密传输

### 2. 数据保护

- 敏感信息加密存储
- 日志脱敏处理
- 定期更新认证信息

### 3. 错误处理

- 统一异常处理
- 详细错误日志
- 优雅降级机制

## 监控和日志

### 1. 应用监控

- 连接状态监控
- 交易量统计
- 性能指标收集

### 2. 日志配置

```yaml
logging:
  level:
    com.trading: DEBUG
  file:
    name: logs/trading-middleware.log
    max-size: 100MB
```

### 3. 告警机制

- 连接断开告警
- 交易异常告警
- 系统资源告警

## 总结

通过本JNI集成方案，您可以：

1. ✅ **完整对接飞马API** - 支持交易和行情功能
2. ✅ **提供现代化接口** - REST API + WebSocket推送
3. ✅ **支持前端集成** - Vue/Electron友好的接口设计
4. ✅ **保证系统稳定** - 完善的错误处理和资源管理
5. ✅ **便于维护扩展** - 清晰的架构和文档

项目现在已经具备了完整的交易中台功能，可以开始进行实际的交易业务开发！
