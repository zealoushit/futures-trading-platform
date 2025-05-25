# 🔧 WebSocket连接问题解决指南

## 🚨 错误信息
```
The URL's scheme must be either 'http:' or 'https:'. 'ws:' is not allowed.
SyntaxError: The URL's scheme must be either 'http:' or 'https:'. 'ws:' is not allowed.
```

## 🔍 问题分析

### 根本原因
这个错误通常出现在以下情况：
1. **SockJS URL格式错误**: SockJS需要使用HTTP/HTTPS协议，不是WS/WSS
2. **浏览器安全限制**: 某些浏览器环境对WebSocket有严格限制
3. **文件协议限制**: 使用file://协议打开HTML文件时的跨域限制
4. **服务端配置问题**: Spring Boot WebSocket配置不正确

## ✅ 解决方案

### 方案1: 使用正确的SockJS URL格式 (推荐)
```javascript
// ❌ 错误的写法
const socket = new SockJS('ws://localhost:8080/ws');

// ✅ 正确的写法
const socket = new SockJS('http://localhost:8080/ws');
```

### 方案2: 使用HTTP服务器而不是file://协议

#### 使用Python启动HTTP服务器:
```bash
cd C:\Users\Administrator\IdeaProjects\untitled
python -m http.server 3000
```
然后访问: `http://localhost:3000/api-test.html`

#### 使用Node.js启动HTTP服务器:
```bash
npm install -g http-server
cd C:\Users\Administrator\IdeaProjects\untitled
http-server -p 3000
```

#### 使用Live Server (VS Code插件):
1. 安装Live Server插件
2. 右键HTML文件选择"Open with Live Server"

### 方案3: 使用简化版测试工具
我们提供了三个不同的测试工具：

1. **`api-test-simple.html`** - 纯REST API测试，无WebSocket依赖 ⭐推荐
2. **`websocket-test.html`** - 专门的WebSocket连接测试工具
3. **`api-test.html`** - 完整版本，包含WebSocket功能

## 🛠️ 测试工具选择指南

### 📊 api-test-simple.html (推荐新手使用)
**优点:**
- ✅ 无WebSocket依赖，避免连接问题
- ✅ 可以直接双击打开，无需HTTP服务器
- ✅ 包含所有REST API测试功能
- ✅ 简单易用，专注于API测试

**使用场景:**
- 测试REST API接口
- 验证服务功能
- 开发调试
- 不需要实时推送功能

### 🔌 websocket-test.html (WebSocket专用)
**优点:**
- ✅ 专门测试WebSocket连接
- ✅ 多种连接方式测试
- ✅ 详细的错误诊断
- ✅ 连接状态实时监控

**使用场景:**
- 诊断WebSocket连接问题
- 测试实时推送功能
- 验证WebSocket配置

### 🚀 api-test.html (完整版)
**优点:**
- ✅ 功能最完整
- ✅ 包含实时行情监控
- ✅ 美观的界面设计
- ✅ 完整的WebSocket推送

**使用场景:**
- 生产环境测试
- 完整功能演示
- 需要实时数据推送

## 🔧 Spring Boot WebSocket配置检查

### 确保WebSocket配置正确
检查 `WebSocketConfig.java` 文件：

```java
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### 检查服务启动日志
启动服务时应该看到类似日志：
```
Mapped "{[/ws/**]}" onto public void org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler.handleRequest(...)
```

## 🧪 测试步骤

### 步骤1: 基础连接测试
1. 打开 `api-test-simple.html`
2. 点击"健康检查"
3. 验证REST API是否正常

### 步骤2: WebSocket连接测试
1. 打开 `websocket-test.html`
2. 点击"测试SockJS连接"
3. 查看连接日志

### 步骤3: 完整功能测试
1. 确保前两步都成功
2. 打开 `api-test.html`
3. 测试完整功能

## 🔍 常见问题排查

### 问题1: CORS跨域错误
**症状**: 控制台显示CORS错误
**解决**: 
- 使用HTTP服务器而不是file://协议
- 检查Spring Boot的CORS配置

### 问题2: 连接被拒绝
**症状**: Connection refused错误
**解决**:
- 确保Java服务已启动
- 检查端口8080是否被占用
- 验证防火墙设置

### 问题3: WebSocket握手失败
**症状**: WebSocket handshake failed
**解决**:
- 检查WebSocket端点配置
- 验证SockJS URL格式
- 查看服务端日志

### 问题4: 浏览器兼容性
**症状**: 某些浏览器无法连接
**解决**:
- 使用Chrome或Edge浏览器
- 更新浏览器到最新版本
- 检查浏览器安全设置

## 📋 快速解决清单

- [ ] 确保Java服务已启动 (`mvn spring-boot:run`)
- [ ] 使用正确的URL格式 (`http://` 而不是 `ws://`)
- [ ] 使用HTTP服务器而不是file://协议
- [ ] 检查浏览器控制台错误信息
- [ ] 尝试使用简化版测试工具
- [ ] 验证防火墙和网络设置
- [ ] 查看服务端启动日志

## 🎯 推荐解决流程

1. **立即可用**: 使用 `api-test-simple.html` 测试REST API
2. **诊断问题**: 使用 `websocket-test.html` 诊断WebSocket
3. **完整测试**: 问题解决后使用 `api-test.html` 完整测试

## 💡 最佳实践

1. **开发阶段**: 优先使用简化版工具，专注于API功能
2. **测试阶段**: 使用WebSocket测试工具验证实时功能
3. **演示阶段**: 使用完整版工具展示所有功能
4. **生产环境**: 确保使用HTTPS和WSS协议

现在您可以根据具体需求选择合适的测试工具，避免WebSocket连接问题！🚀
