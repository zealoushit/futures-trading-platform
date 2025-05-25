# 交易中台服务 API 接口文档

## 基础信息

- **服务地址**: `http://localhost:8080`
- **API前缀**: `/api`
- **WebSocket地址**: `ws://localhost:8080/ws`
- **内容类型**: `application/json`
- **字符编码**: `UTF-8`

## 通用响应格式

所有API接口都返回统一的响应格式：

```json
{
  "success": true,           // 请求是否成功
  "code": 200,              // 状态码
  "message": "操作成功",     // 响应消息
  "data": {},               // 响应数据
  "timestamp": 1640995200000 // 时间戳
}
```

### 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 500 | 服务器内部错误 |

## 1. 交易相关接口

### 1.1 获取交易状态

**接口地址**: `GET /api/trading/status`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "获取状态成功",
  "data": {
    "connected": true,        // 是否连接到交易前置机
    "loggedIn": false,        // 是否已登录
    "timestamp": 1640995200000
  }
}
```

### 1.2 用户登录

**接口地址**: `POST /api/trading/login`

**请求参数**: 无（配置文件中已设置用户信息）

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "登录成功",
  "data": null
}
```

### 1.3 用户登出

**接口地址**: `POST /api/trading/logout`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

### 1.4 下单

**接口地址**: `POST /api/trading/order`

**请求参数**:
```json
{
  "instrumentId": "rb2405",    // 合约代码
  "direction": "0",            // 买卖方向: 0-买, 1-卖
  "offsetFlag": "0",           // 开平标志: 0-开仓, 1-平仓, 3-平今
  "price": 3500.0,             // 价格
  "volume": 1                  // 数量
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "下单成功",
  "data": null
}
```

### 1.5 撤单

**接口地址**: `POST /api/trading/cancel`

**请求参数**:
```json
{
  "orderRef": "000001",        // 报单引用
  "frontId": 1,                // 前置编号
  "sessionId": 123456          // 会话编号
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "撤单成功",
  "data": null
}
```

### 1.6 查询持仓

**接口地址**: `GET /api/trading/position`

**请求参数**:
- `instrumentId` (可选): 合约代码，不传则查询所有持仓

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "查询持仓成功",
  "data": [
    {
      "instrumentId": "rb2405",
      "direction": "0",          // 持仓方向
      "position": 10,            // 持仓量
      "todayPosition": 5,        // 今仓
      "yesterdayPosition": 5,    // 昨仓
      "avgPrice": 3500.0,        // 平均价格
      "positionCost": 35000.0,   // 持仓成本
      "margin": 7000.0           // 保证金
    }
  ]
}
```

### 1.7 查询资金账户

**接口地址**: `GET /api/trading/account`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "查询资金账户成功",
  "data": {
    "accountId": "123456",
    "balance": 100000.0,         // 账户余额
    "available": 80000.0,        // 可用资金
    "margin": 20000.0,           // 占用保证金
    "frozenMargin": 0.0,         // 冻结保证金
    "commission": 50.0,          // 手续费
    "profit": 1000.0,            // 盈亏
    "currency": "CNY"            // 币种
  }
}
```

### 1.8 查询报单

**接口地址**: `GET /api/trading/orders`

**请求参数**:
- `instrumentId` (可选): 合约代码

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "查询报单成功",
  "data": [
    {
      "orderSysId": "20240101000001",
      "orderRef": "000001",
      "instrumentId": "rb2405",
      "direction": "0",
      "offsetFlag": "0",
      "price": 3500.0,
      "volume": 10,
      "tradedVolume": 5,
      "orderStatus": "1",          // 报单状态: 0-全部成交, 1-部分成交, 3-未成交, 5-已撤销
      "insertTime": "09:30:00",
      "updateTime": "09:30:05"
    }
  ]
}
```

### 1.9 查询成交

**接口地址**: `GET /api/trading/trades`

**请求参数**:
- `instrumentId` (可选): 合约代码

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "查询成交成功",
  "data": [
    {
      "tradeId": "20240101000001",
      "orderRef": "000001",
      "instrumentId": "rb2405",
      "direction": "0",
      "offsetFlag": "0",
      "price": 3500.0,
      "volume": 5,
      "tradeTime": "09:30:05"
    }
  ]
}
```

### 1.10 查询合约

**接口地址**: `GET /api/trading/instrument/{instrumentId}`

**路径参数**:
- `instrumentId`: 合约代码

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "查询合约成功",
  "data": {
    "instrumentId": "rb2405",
    "instrumentName": "螺纹钢2405",
    "exchangeId": "SHFE",
    "productId": "rb",
    "priceTick": 1.0,            // 最小变动价位
    "volumeMultiple": 10,        // 合约乘数
    "minMarginRatio": 0.08       // 最小保证金率
  }
}
```

## 2. 行情相关接口

### 2.1 获取行情状态

**接口地址**: `GET /api/market/status`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "获取行情状态成功",
  "data": {
    "connected": true,
    "loggedIn": true,
    "subscribedInstruments": ["rb2405", "cu2405"],
    "timestamp": 1640995200000
  }
}
```

### 2.2 行情登录

**接口地址**: `POST /api/market/login`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "行情登录成功",
  "data": null
}
```

### 2.3 订阅行情

**接口地址**: `POST /api/market/subscribe`

**请求参数**:
```json
{
  "instruments": ["rb2405", "cu2405"]  // 合约代码数组
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "订阅行情成功",
  "data": null
}
```

### 2.4 退订行情

**接口地址**: `POST /api/market/unsubscribe`

**请求参数**:
```json
{
  "instruments": ["rb2405"]  // 合约代码数组
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "退订行情成功",
  "data": null
}
```

### 2.5 查询合约信息

**接口地址**: `GET /api/market/instrument/{instrumentId}`

**路径参数**:
- `instrumentId`: 合约代码

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "查询合约成功",
  "data": {
    "instrumentId": "rb2405",
    "instrumentName": "螺纹钢2405",
    "exchangeId": "SHFE",
    "productId": "rb",
    "priceTick": 1.0,
    "volumeMultiple": 10,
    "minMarginRatio": 0.08
  }
}
```

### 2.6 获取订阅列表

**接口地址**: `GET /api/market/subscriptions`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "获取订阅列表成功",
  "data": ["rb2405", "cu2405"]
}
```

## 3. 系统接口

### 3.1 健康检查

**接口地址**: `GET /api/trading/health`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "服务运行正常",
  "data": "OK"
}
```

### 3.2 版本信息

**接口地址**: `GET /api/trading/version`

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "获取版本信息成功",
  "data": {
    "service": "Trading Middleware",
    "version": "1.0.0",
    "api": "Femas V1.32",
    "java": "1.8.0_XXX"
  }
}
```

## 4. WebSocket 实时推送

### 4.1 连接WebSocket

**连接地址**: `ws://localhost:8080/ws`

**协议**: STOMP over SockJS

**连接示例**:
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // 订阅主题
    stompClient.subscribe('/topic/orders', function(message) {
        const data = JSON.parse(message.body);
        console.log('报单回报:', data);
    });
});
```

### 4.2 交易相关推送

#### 4.2.1 连接状态推送

**主题**: `/topic/connection`

**推送时机**: 交易前置机连接状态变化时

**消息格式**:
```json
{
  "success": true,
  "code": 200,
  "message": "交易前置机连接成功",
  "data": null,
  "timestamp": 1640995200000
}
```

#### 4.2.2 登录状态推送

**主题**: `/topic/login`

**推送时机**: 用户登录状态变化时

**消息格式**:
```json
{
  "success": true,
  "code": 200,
  "message": "登录成功",
  "data": null,
  "timestamp": 1640995200000
}
```

#### 4.2.3 报单回报推送

**主题**: `/topic/orders`

**推送时机**: 收到报单状态变化时

**消息格式**:
```json
{
  "success": true,
  "code": 200,
  "message": "报单回报",
  "data": {
    "orderSysId": "20240101000001",
    "orderRef": "000001",
    "instrumentId": "rb2405",
    "direction": "0",
    "offsetFlag": "0",
    "price": 3500.0,
    "volume": 10,
    "orderStatus": "1"
  },
  "timestamp": 1640995200000
}
```

#### 4.2.4 成交回报推送

**主题**: `/topic/trades`

**推送时机**: 收到成交回报时

**消息格式**:
```json
{
  "success": true,
  "code": 200,
  "message": "成交回报",
  "data": {
    "tradeId": "20240101000001",
    "orderRef": "000001",
    "instrumentId": "rb2405",
    "direction": "0",
    "offsetFlag": "0",
    "price": 3500.0,
    "volume": 5,
    "tradeTime": "09:30:05"
  },
  "timestamp": 1640995200000
}
```

### 4.3 行情相关推送

#### 4.3.1 行情连接状态推送

**主题**: `/topic/market/connection`

**推送时机**: 行情前置机连接状态变化时

**消息格式**:
```json
{
  "success": true,
  "code": 200,
  "message": "行情前置机连接成功",
  "data": null,
  "timestamp": 1640995200000
}
```

#### 4.3.2 实时行情数据推送

**主题**: `/topic/market/data`

**推送时机**: 收到行情数据时

**消息格式**:
```json
{
  "success": true,
  "code": 200,
  "message": "行情数据",
  "data": {
    "instrumentId": "rb2405",
    "updateTime": "09:30:05",
    "lastPrice": 3500.0,
    "volume": 12345,
    "turnover": 43225000.0,
    "openInterest": 98765.0,
    "bid": {
      "price1": 3499.0, "volume1": 100,
      "price2": 3498.0, "volume2": 200,
      "price3": 3497.0, "volume3": 150,
      "price4": 3496.0, "volume4": 300,
      "price5": 3495.0, "volume5": 250
    },
    "ask": {
      "price1": 3500.0, "volume1": 120,
      "price2": 3501.0, "volume2": 180,
      "price3": 3502.0, "volume3": 160,
      "price4": 3503.0, "volume4": 220,
      "price5": 3504.0, "volume5": 190
    },
    "upperLimitPrice": 3850.0,
    "lowerLimitPrice": 3150.0,
    "preClosePrice": 3450.0,
    "openPrice": 3480.0,
    "highestPrice": 3520.0,
    "lowestPrice": 3470.0
  },
  "timestamp": 1640995200000
}
```

#### 4.3.3 特定合约行情推送

**主题**: `/topic/market/data/{instrumentId}`

**推送时机**: 收到特定合约行情数据时

**消息格式**: 与通用行情数据格式相同

## 5. 错误处理

### 5.1 错误响应格式

```json
{
  "success": false,
  "code": 400,
  "message": "参数错误",
  "data": null,
  "timestamp": 1640995200000
}
```

### 5.2 常见错误码

| 错误码 | 错误信息 | 说明 |
|--------|----------|------|
| 400 | 参数错误 | 请求参数格式不正确 |
| 401 | 用户未登录 | 需要先登录 |
| 500 | 服务器内部错误 | 系统异常 |
| 1001 | 未连接到交易前置机 | 网络连接问题 |
| 1002 | 下单失败 | 交易相关错误 |
| 1003 | 查询失败 | 查询相关错误 |

## 6. 前端集成示例

### 6.1 JavaScript/TypeScript 示例

```javascript
// API客户端类
class TradingApiClient {
    constructor(baseUrl = 'http://localhost:8080/api') {
        this.baseUrl = baseUrl;
    }

    async request(method, url, data = null) {
        const options = {
            method,
            headers: { 'Content-Type': 'application/json' },
        };
        if (data) options.body = JSON.stringify(data);

        const response = await fetch(`${this.baseUrl}${url}`, options);
        const result = await response.json();

        if (!result.success) {
            throw new Error(result.message);
        }
        return result;
    }

    // 交易接口
    async login() { return this.request('POST', '/trading/login'); }
    async getStatus() { return this.request('GET', '/trading/status'); }
    async placeOrder(order) { return this.request('POST', '/trading/order', order); }
    async queryPosition() { return this.request('GET', '/trading/position'); }

    // 行情接口
    async subscribeMarket(instruments) {
        return this.request('POST', '/market/subscribe', { instruments });
    }
}

// WebSocket客户端
class TradingWebSocket {
    constructor() {
        this.socket = new SockJS('http://localhost:8080/ws');
        this.stompClient = Stomp.over(this.socket);
    }

    connect() {
        return new Promise((resolve, reject) => {
            this.stompClient.connect({}, resolve, reject);
        });
    }

    subscribe(topic, callback) {
        this.stompClient.subscribe(topic, (message) => {
            callback(JSON.parse(message.body));
        });
    }
}

// 使用示例
const api = new TradingApiClient();
const ws = new TradingWebSocket();

// 连接WebSocket
await ws.connect();

// 订阅推送
ws.subscribe('/topic/orders', (data) => {
    console.log('报单回报:', data);
});

// 调用API
await api.login();
const status = await api.getStatus();
console.log('状态:', status.data);
```

### 6.2 Vue.js 集成示例

```vue
<template>
  <div class="trading-terminal">
    <div class="status-bar">
      <span :class="{'connected': isConnected}">
        交易连接: {{ isConnected ? '已连接' : '未连接' }}
      </span>
      <span :class="{'logged-in': isLoggedIn}">
        登录状态: {{ isLoggedIn ? '已登录' : '未登录' }}
      </span>
    </div>

    <div class="order-form">
      <input v-model="orderForm.instrumentId" placeholder="合约代码" />
      <select v-model="orderForm.direction">
        <option value="0">买</option>
        <option value="1">卖</option>
      </select>
      <input v-model.number="orderForm.price" placeholder="价格" />
      <input v-model.number="orderForm.volume" placeholder="数量" />
      <button @click="placeOrder">下单</button>
    </div>

    <div class="market-data">
      <div v-for="(data, instrumentId) in marketData" :key="instrumentId">
        {{ instrumentId }}: {{ data.lastPrice }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      isConnected: false,
      isLoggedIn: false,
      marketData: {},
      orderForm: {
        instrumentId: '',
        direction: '0',
        offsetFlag: '0',
        price: 0,
        volume: 1
      },
      apiClient: null,
      wsClient: null
    };
  },

  async mounted() {
    // 初始化API客户端
    this.apiClient = new TradingApiClient();
    this.wsClient = new TradingWebSocket();

    // 连接WebSocket
    await this.wsClient.connect();
    this.setupWebSocketSubscriptions();

    // 获取初始状态
    await this.refreshStatus();
  },

  methods: {
    setupWebSocketSubscriptions() {
      this.wsClient.subscribe('/topic/connection', (data) => {
        this.isConnected = data.success;
      });

      this.wsClient.subscribe('/topic/login', (data) => {
        this.isLoggedIn = data.success;
      });

      this.wsClient.subscribe('/topic/market/data', (data) => {
        this.marketData[data.data.instrumentId] = data.data;
      });
    },

    async refreshStatus() {
      try {
        const result = await this.apiClient.getStatus();
        this.isConnected = result.data.connected;
        this.isLoggedIn = result.data.loggedIn;
      } catch (error) {
        console.error('获取状态失败:', error);
      }
    },

    async placeOrder() {
      try {
        await this.apiClient.placeOrder(this.orderForm);
        this.$message.success('下单成功');
      } catch (error) {
        this.$message.error('下单失败: ' + error.message);
      }
    }
  }
};
</script>
```

## 7. 测试工具

### 7.1 Postman 测试集合

可以导入以下Postman集合进行API测试：

```json
{
  "info": {
    "name": "Trading Middleware API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "获取交易状态",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/api/trading/status",
          "host": ["{{baseUrl}}"],
          "path": ["api", "trading", "status"]
        }
      }
    },
    {
      "name": "用户登录",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "url": {
          "raw": "{{baseUrl}}/api/trading/login",
          "host": ["{{baseUrl}}"],
          "path": ["api", "trading", "login"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    }
  ]
}
```

### 7.2 curl 测试命令

```bash
# 获取状态
curl -X GET http://localhost:8080/api/trading/status

# 用户登录
curl -X POST http://localhost:8080/api/trading/login

# 下单
curl -X POST http://localhost:8080/api/trading/order \
  -H "Content-Type: application/json" \
  -d '{
    "instrumentId": "rb2405",
    "direction": "0",
    "offsetFlag": "0",
    "price": 3500.0,
    "volume": 1
  }'

# 查询持仓
curl -X GET http://localhost:8080/api/trading/position

# 订阅行情
curl -X POST http://localhost:8080/api/market/subscribe \
  -H "Content-Type: application/json" \
  -d '{"instruments": ["rb2405", "cu2405"]}'
```
