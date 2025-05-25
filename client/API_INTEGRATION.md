# Java服务API对接说明 - 基于实际API文档

## 概述

本文档说明了前端期货交易平台与Java后端服务的API对接方案。前端已经根据您提供的API文档完成了接口封装和STOMP over SockJS WebSocket连接的实现，支持实时行情数据推送和交易功能。

## 配置说明

### 环境配置

在 `.env.development` 和 `.env.production` 文件中配置Java服务地址：

```bash
# 开发环境
VUE_APP_API_BASE_URL=http://localhost:8080
VUE_APP_WS_URL=ws://localhost:8080/ws/market

# 生产环境
VUE_APP_API_BASE_URL=http://your-production-server:8080
VUE_APP_WS_URL=ws://your-production-server:8080/ws/market
```

### API配置

在 `src/config/api.js` 中定义了所有API端点和WebSocket消息类型，便于统一管理。

## API接口规范 - 基于实际API文档

### 1. 交易相关接口

#### 获取交易状态
- **URL**: `GET /api/trading/status`
- **响应格式**:
```json
{
  "success": true,
  "message": "获取状态成功",
  "data": {
    "status": "connected",
    "timestamp": "2024-01-01T09:30:00"
  }
}
```

#### 交易登录
- **URL**: `POST /api/trading/login`
- **响应格式**:
```json
{
  "success": true,
  "message": "登录成功"
}
```

#### 交易登出
- **URL**: `POST /api/trading/logout`
- **响应格式**:
```json
{
  "success": true,
  "message": "登出成功"
}
```

#### 下单
- **URL**: `POST /api/trading/order`
- **请求参数**:
```json
{
  "instrumentId": "CU2401",
  "direction": "0",
  "offsetFlag": "0",
  "price": 68500.0,
  "volume": 10
}
```
- **字段说明**:
  - `direction`: "0"=买, "1"=卖
  - `offsetFlag`: "0"=开仓, "1"=平仓, "3"=平今

#### 撤单
- **URL**: `POST /api/trading/cancel`
- **请求参数**:
```json
{
  "orderRef": "订单引用",
  "instrumentId": "CU2401"
}
```

#### 查询持仓
- **URL**: `GET /api/trading/position`
- **查询参数**: `instrumentId` (可选)
- **响应格式**:
```json
{
  "success": true,
  "data": [
    {
      "instrumentId": "CU2401",
      "posiDirection": "2",
      "position": 10,
      "positionCost": 680000.0,
      "openCost": 680000.0,
      "positionProfit": 5000.0,
      "useMargin": 136000.0
    }
  ]
}
```

#### 查询资金账户
- **URL**: `GET /api/trading/account`
- **响应格式**:
```json
{
  "success": true,
  "data": {
    "available": 1000000.0,
    "currMargin": 136000.0,
    "balance": 1050000.0,
    "positionProfit": 5000.0,
    "closeProfit": 25000.0
  }
}
```

#### 查询报单
- **URL**: `GET /api/trading/orders`
- **查询参数**: `instrumentId` (可选)
- **响应格式**:
```json
{
  "success": true,
  "data": [
    {
      "orderRef": "订单引用",
      "instrumentId": "CU2401",
      "direction": "0",
      "limitPrice": 68500.0,
      "volumeTotalOriginal": 10,
      "volumeTraded": 0,
      "orderStatus": "3",
      "insertTime": "2024-01-01 09:30:00"
    }
  ]
}
```

#### 查询成交
- **URL**: `GET /api/trading/trades`
- **查询参数**: `instrumentId` (可选)
- **响应格式**:
```json
{
  "success": true,
  "data": [
    {
      "instrumentId": "CU2401",
      "direction": "0",
      "price": 68500.0,
      "volume": 10,
      "tradeTime": "2024-01-01 09:30:15"
    }
  ]
}
```

### 2. 行情相关接口

#### 获取行情状态
- **URL**: `GET /api/market/status`

#### 行情登录
- **URL**: `POST /api/market/login`

#### 订阅行情
- **URL**: `POST /api/market/subscribe`
- **请求参数**:
```json
{
  "instruments": ["CU2401", "AL2401"]
}
```

#### 退订行情
- **URL**: `POST /api/market/unsubscribe`
- **请求参数**:
```json
{
  "instruments": ["CU2401", "AL2401"]
}
```

#### 查询合约信息
- **URL**: `GET /api/market/instrument/{instrumentId}`

#### 获取订阅列表
- **URL**: `GET /api/market/subscriptions`

## WebSocket实时数据 - STOMP over SockJS

### 连接地址
```
http://localhost:8080/ws
```

### 连接协议
使用STOMP over SockJS协议，支持自动重连和心跳机制。

**重要说明**: SockJS使用HTTP/HTTPS协议，不是WebSocket的WS/WSS协议。这是因为SockJS会自动选择最佳的传输方式（WebSocket、XHR streaming、XHR polling等）。

### 订阅主题

#### 连接状态主题
- **主题**: `/topic/connection`
- **数据格式**: 连接状态信息

#### 登录状态主题
- **主题**: `/topic/login`
- **数据格式**: 登录状态信息

#### 订单更新主题
- **主题**: `/topic/orders`
- **数据格式**:
```json
{
  "orderRef": "订单引用",
  "instrumentId": "CU2401",
  "direction": "0",
  "limitPrice": 68500.0,
  "volumeTotalOriginal": 10,
  "volumeTraded": 5,
  "orderStatus": "1",
  "insertTime": "2024-01-01 09:30:00"
}
```

#### 成交数据主题
- **主题**: `/topic/trades`
- **数据格式**:
```json
{
  "instrumentId": "CU2401",
  "direction": "0",
  "price": 68500.0,
  "volume": 10,
  "tradeTime": "2024-01-01 09:30:15"
}
```

#### 行情连接状态主题
- **主题**: `/topic/market/connection`
- **数据格式**: 行情连接状态信息

#### 行情数据主题
- **通用主题**: `/topic/market/data`
- **特定合约主题**: `/topic/market/data/{instrumentId}`
- **数据格式**:
```json
{
  "instrumentId": "CU2401",
  "lastPrice": 68500.0,
  "preClosePrice": 68000.0,
  "openPrice": 68200.0,
  "highestPrice": 68800.0,
  "lowestPrice": 68100.0,
  "volume": 15000,
  "turnover": 1027500000.0,
  "updateTime": "2024-01-01 09:30:00"
}
```

### 状态码说明

#### 订单状态 (orderStatus)
- "0": 全部成交
- "1": 部分成交
- "3": 未成交
- "5": 已撤销

#### 买卖方向 (direction)
- "0": 买
- "1": 卖

#### 开平标志 (offsetFlag)
- "0": 开仓
- "1": 平仓
- "3": 平今

## 前端实现特点

### 1. 自动降级
- 如果API调用失败，自动使用模拟数据
- 如果WebSocket连接失败，启动模拟数据更新
- 确保界面始终可用

### 2. 错误处理
- 统一的错误拦截和处理
- 用户友好的错误提示
- 自动重连机制

### 3. 状态管理
- 使用Vuex管理应用状态
- 实时同步WebSocket数据到store
- 支持数据持久化

### 4. 性能优化
- 虚拟滚动处理大量数据
- 防抖处理频繁更新
- 内存泄漏防护

## 部署说明

### 1. 开发环境
```bash
# 启动前端开发服务器
npm run electron:serve

# 确保Java服务运行在 localhost:8080
```

### 2. 生产环境
```bash
# 构建生产版本
npm run electron:build

# 修改 .env.production 中的服务器地址
```

## 测试建议

### 1. API测试
- 使用Postman测试所有API接口
- 验证请求参数和响应格式
- 测试错误场景和边界条件

### 2. WebSocket测试
- 测试连接建立和断开
- 验证消息订阅和推送
- 测试重连机制

### 3. 集成测试
- 测试登录流程
- 测试行情数据展示
- 测试下单和撤单功能
- 测试实时数据更新

## 注意事项

1. **CORS配置**: 确保Java服务配置了正确的CORS策略
2. **Token验证**: 实现JWT令牌的生成和验证
3. **数据格式**: 严格按照约定的JSON格式返回数据
4. **错误码**: 使用统一的错误码和错误信息
5. **性能优化**: 对于高频数据推送，考虑数据压缩和批量发送

## 后续扩展

1. **SSL支持**: 生产环境使用HTTPS和WSS
2. **负载均衡**: 支持多个后端服务实例
3. **监控告警**: 添加API调用监控和告警
4. **缓存策略**: 实现合理的数据缓存机制
