package com.trading.controller;

import com.trading.model.ApiResponse;
import com.trading.model.UserSession;
import com.trading.service.MarketDataPushService;
import com.trading.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 前端认证控制器
 * 处理前端用户的登录、登出和会话管理
 * 与交易后台认证分离
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private MarketDataPushService marketDataPushService;

    /**
     * 前端用户登录
     * 与交易后台认证分离，仅用于前端会话管理
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String clientId = request.get("clientId");

            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                return ApiResponse.error("用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return ApiResponse.error("密码不能为空");
            }
            if (clientId == null || clientId.trim().isEmpty()) {
                clientId = "WEB_" + System.currentTimeMillis();
            }

            logger.info("前端用户登录请求: username={}, clientId={}", username, clientId);

            // 简单的用户验证（实际应用中应该连接用户数据库）
            if (!validateUser(username, password)) {
                return ApiResponse.error("用户名或密码错误");
            }

            // 创建用户会话
            UserSession session = userSessionService.createSession(username, clientId);

            // 构造响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("sessionId", session.getSessionId());
            responseData.put("username", session.getUsername());
            responseData.put("clientId", session.getClientId());
            responseData.put("loginTime", session.getLoginTime());
            responseData.put("permissions", session.getPermissions());

            logger.info("前端用户登录成功: sessionId={}, username={}", 
                       session.getSessionId(), username);

            return ApiResponse.success("登录成功", responseData);

        } catch (Exception e) {
            logger.error("前端用户登录失败", e);
            return ApiResponse.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 前端用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            String clientId = request.get("clientId");

            if (sessionId != null) {
                boolean success = userSessionService.logout(sessionId);
                if (success) {
                    logger.info("用户登出成功: sessionId={}", sessionId);
                    return ApiResponse.success("登出成功", "会话已结束");
                } else {
                    return ApiResponse.error("会话不存在或已过期");
                }
            } else if (clientId != null) {
                boolean success = userSessionService.logoutByClientId(clientId);
                if (success) {
                    logger.info("用户登出成功: clientId={}", clientId);
                    return ApiResponse.success("登出成功", "会话已结束");
                } else {
                    return ApiResponse.error("客户端会话不存在");
                }
            } else {
                return ApiResponse.error("缺少sessionId或clientId参数");
            }

        } catch (Exception e) {
            logger.error("前端用户登出失败", e);
            return ApiResponse.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 验证会话有效性
     */
    @PostMapping("/validate")
    public ApiResponse<Map<String, Object>> validateSession(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return ApiResponse.error("sessionId不能为空");
            }

            UserSession session = userSessionService.getSession(sessionId);
            if (session == null) {
                return ApiResponse.error("会话不存在");
            }

            if (!session.isActive()) {
                return ApiResponse.error("会话已失效");
            }

            // 构造响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("sessionId", session.getSessionId());
            responseData.put("username", session.getUsername());
            responseData.put("clientId", session.getClientId());
            responseData.put("loginTime", session.getLoginTime());
            responseData.put("lastActiveTime", session.getLastActiveTime());
            responseData.put("subscribedExchanges", session.getSubscribedExchanges());
            responseData.put("subscribedInstruments", session.getSubscribedInstruments());
            responseData.put("permissions", session.getPermissions());

            return ApiResponse.success("会话有效", responseData);

        } catch (Exception e) {
            logger.error("验证会话失败", e);
            return ApiResponse.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 更新订阅信息
     */
    @PostMapping("/subscribe")
    public ApiResponse<String> updateSubscription(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return ApiResponse.error("sessionId不能为空");
            }

            // 验证会话
            if (!userSessionService.isValidSession(sessionId)) {
                return ApiResponse.error("会话无效或已过期");
            }

            // 解析订阅参数
            @SuppressWarnings("unchecked")
            List<String> exchanges = (List<String>) request.get("exchanges");
            @SuppressWarnings("unchecked")
            List<String> instruments = (List<String>) request.get("instruments");

            Set<String> exchangeSet = exchanges != null ? new HashSet<>(exchanges) : new HashSet<>();
            Set<String> instrumentSet = instruments != null ? new HashSet<>(instruments) : new HashSet<>();

            // 更新订阅
            boolean success = userSessionService.updateSubscription(sessionId, exchangeSet, instrumentSet);
            if (!success) {
                return ApiResponse.error("更新订阅失败");
            }

            // 立即推送全量数据给该会话
            marketDataPushService.pushFullDataToSession(sessionId);

            logger.info("更新用户订阅: sessionId={}, exchanges={}, instruments={}", 
                       sessionId, exchangeSet, instrumentSet);

            return ApiResponse.success("订阅更新成功", "已开始推送行情数据");

        } catch (Exception e) {
            logger.error("更新订阅失败", e);
            return ApiResponse.error("更新订阅失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话信息
     */
    @GetMapping("/session/{sessionId}")
    public ApiResponse<Map<String, Object>> getSessionInfo(@PathVariable String sessionId) {
        try {
            UserSession session = userSessionService.getSession(sessionId);
            if (session == null) {
                return ApiResponse.error("会话不存在");
            }

            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", session.getSessionId());
            sessionInfo.put("username", session.getUsername());
            sessionInfo.put("clientId", session.getClientId());
            sessionInfo.put("loginTime", session.getLoginTime());
            sessionInfo.put("lastActiveTime", session.getLastActiveTime());
            sessionInfo.put("isActive", session.isActive());
            sessionInfo.put("subscribedExchanges", session.getSubscribedExchanges());
            sessionInfo.put("subscribedInstruments", session.getSubscribedInstruments());
            sessionInfo.put("permissions", session.getPermissions());

            return ApiResponse.success("获取会话信息成功", sessionInfo);

        } catch (Exception e) {
            logger.error("获取会话信息失败", e);
            return ApiResponse.error("获取会话信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 会话统计
            stats.put("sessionStats", userSessionService.getSessionStats());
            
            // 推送统计
            stats.put("pushStats", marketDataPushService.getPushStats());
            
            return ApiResponse.success("获取系统统计成功", stats);

        } catch (Exception e) {
            logger.error("获取系统统计失败", e);
            return ApiResponse.error("获取系统统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online-users")
    public ApiResponse<List<Map<String, Object>>> getOnlineUsers() {
        try {
            List<UserSession> activeSessions = userSessionService.getActiveSessions();
            
            List<Map<String, Object>> users = activeSessions.stream()
                    .map(session -> {
                        Map<String, Object> user = new HashMap<>();
                        user.put("sessionId", session.getSessionId());
                        user.put("username", session.getUsername());
                        user.put("clientId", session.getClientId());
                        user.put("loginTime", session.getLoginTime());
                        user.put("lastActiveTime", session.getLastActiveTime());
                        user.put("subscribedExchanges", session.getSubscribedExchanges());
                        user.put("subscribedInstruments", session.getSubscribedInstruments());
                        return user;
                    })
                    .collect(java.util.stream.Collectors.toList());

            return ApiResponse.success("获取在线用户成功", users);

        } catch (Exception e) {
            logger.error("获取在线用户失败", e);
            return ApiResponse.error("获取在线用户失败: " + e.getMessage());
        }
    }

    /**
     * 简单的用户验证
     * 实际应用中应该连接用户数据库或LDAP等认证系统
     */
    private boolean validateUser(String username, String password) {
        // 这里实现简单的用户验证逻辑
        // 实际应用中应该：
        // 1. 查询用户数据库
        // 2. 验证密码哈希
        // 3. 检查用户状态（是否被禁用等）
        // 4. 记录登录日志
        
        // 演示用的简单验证
        Map<String, String> users = new HashMap<>();
        users.put("admin", "admin123");
        users.put("trader1", "trader123");
        users.put("trader2", "trader123");
        users.put("guest", "guest123");
        
        return password.equals(users.get(username));
    }
}
