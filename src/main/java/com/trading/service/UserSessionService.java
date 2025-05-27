package com.trading.service;

import com.trading.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户会话管理服务
 * 管理前端用户的登录状态、权限和订阅信息
 */
@Service
public class UserSessionService {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionService.class);

    // 会话存储 - sessionId -> UserSession
    private final ConcurrentHashMap<String, UserSession> sessions = new ConcurrentHashMap<>();
    
    // 用户名到会话ID的映射 - username -> Set<sessionId>
    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    // 客户端ID到会话ID的映射 - clientId -> sessionId
    private final ConcurrentHashMap<String, String> clientSessions = new ConcurrentHashMap<>();
    
    // 会话清理任务
    private ScheduledExecutorService cleanupExecutor;
    
    // 配置参数
    private static final int SESSION_TIMEOUT_MINUTES = 30;  // 会话超时时间
    private static final int CLEANUP_INTERVAL_MINUTES = 5;  // 清理任务间隔
    
    @PostConstruct
    public void initialize() {
        logger.info("初始化用户会话管理服务...");
        
        // 启动会话清理任务
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SessionCleanup");
            t.setDaemon(true);
            return t;
        });
        
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredSessions,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        logger.info("用户会话管理服务初始化完成");
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("清理用户会话管理服务...");
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
    }
    
    /**
     * 创建用户会话
     */
    public UserSession createSession(String username, String clientId) {
        String sessionId = generateSessionId();
        UserSession session = new UserSession(sessionId, username, clientId);
        
        // 添加基本权限
        session.addPermission("MARKET_DATA");
        session.addPermission("BASIC_QUERY");
        
        // 存储会话
        sessions.put(sessionId, session);
        
        // 更新用户会话映射
        userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        
        // 更新客户端会话映射
        String oldSessionId = clientSessions.put(clientId, sessionId);
        if (oldSessionId != null) {
            // 如果客户端已有会话，移除旧会话
            removeSession(oldSessionId);
        }
        
        logger.info("创建用户会话: sessionId={}, username={}, clientId={}", 
                   sessionId, username, clientId);
        
        return session;
    }
    
    /**
     * 获取会话
     */
    public UserSession getSession(String sessionId) {
        UserSession session = sessions.get(sessionId);
        if (session != null) {
            session.updateLastActiveTime();
        }
        return session;
    }
    
    /**
     * 根据客户端ID获取会话
     */
    public UserSession getSessionByClientId(String clientId) {
        String sessionId = clientSessions.get(clientId);
        return sessionId != null ? getSession(sessionId) : null;
    }
    
    /**
     * 获取用户的所有会话
     */
    public List<UserSession> getUserSessions(String username) {
        Set<String> sessionIds = userSessions.get(username);
        if (sessionIds == null) {
            return Collections.emptyList();
        }
        
        return sessionIds.stream()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 移除会话
     */
    public boolean removeSession(String sessionId) {
        UserSession session = sessions.remove(sessionId);
        if (session == null) {
            return false;
        }
        
        // 从用户会话映射中移除
        Set<String> userSessionIds = userSessions.get(session.getUsername());
        if (userSessionIds != null) {
            userSessionIds.remove(sessionId);
            if (userSessionIds.isEmpty()) {
                userSessions.remove(session.getUsername());
            }
        }
        
        // 从客户端会话映射中移除
        clientSessions.entrySet().removeIf(entry -> sessionId.equals(entry.getValue()));
        
        logger.info("移除用户会话: sessionId={}, username={}", 
                   sessionId, session.getUsername());
        
        return true;
    }
    
    /**
     * 用户登出
     */
    public boolean logout(String sessionId) {
        UserSession session = getSession(sessionId);
        if (session != null) {
            session.setActive(false);
            return removeSession(sessionId);
        }
        return false;
    }
    
    /**
     * 根据客户端ID登出
     */
    public boolean logoutByClientId(String clientId) {
        String sessionId = clientSessions.get(clientId);
        return sessionId != null && logout(sessionId);
    }
    
    /**
     * 验证会话是否有效
     */
    public boolean isValidSession(String sessionId) {
        UserSession session = sessions.get(sessionId);
        return session != null && session.isActive() && !session.isExpired(SESSION_TIMEOUT_MINUTES);
    }
    
    /**
     * 更新会话订阅信息
     */
    public boolean updateSubscription(String sessionId, Set<String> exchanges, Set<String> instruments) {
        UserSession session = getSession(sessionId);
        if (session == null) {
            return false;
        }
        
        // 清空现有订阅
        session.getSubscribedExchanges().clear();
        session.getSubscribedInstruments().clear();
        
        // 添加新订阅
        if (exchanges != null) {
            exchanges.forEach(session::addSubscribedExchange);
        }
        if (instruments != null) {
            instruments.forEach(session::addSubscribedInstrument);
        }
        
        logger.info("更新会话订阅: sessionId={}, exchanges={}, instruments={}", 
                   sessionId, exchanges, instruments);
        
        return true;
    }
    
    /**
     * 获取订阅了指定交易所的所有会话
     */
    public List<UserSession> getSessionsByExchange(String exchange) {
        return sessions.values().stream()
                .filter(session -> session.isActive() && session.isSubscribedToExchange(exchange))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取订阅了指定合约的所有会话
     */
    public List<UserSession> getSessionsByInstrument(String instrument) {
        return sessions.values().stream()
                .filter(session -> session.isActive() && session.isSubscribedToInstrument(instrument))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有活跃会话
     */
    public List<UserSession> getActiveSessions() {
        return sessions.values().stream()
                .filter(UserSession::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取会话统计信息
     */
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", sessions.size());
        stats.put("activeSessions", getActiveSessions().size());
        stats.put("uniqueUsers", userSessions.size());
        stats.put("connectedClients", clientSessions.size());
        
        // 按交易所统计订阅数
        Map<String, Long> exchangeStats = sessions.values().stream()
                .filter(UserSession::isActive)
                .flatMap(session -> session.getSubscribedExchanges().stream())
                .collect(Collectors.groupingBy(exchange -> exchange, Collectors.counting()));
        stats.put("exchangeSubscriptions", exchangeStats);
        
        return stats;
    }
    
    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        try {
            List<String> expiredSessions = sessions.entrySet().stream()
                    .filter(entry -> entry.getValue().isExpired(SESSION_TIMEOUT_MINUTES))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            if (!expiredSessions.isEmpty()) {
                logger.info("清理过期会话: {}", expiredSessions.size());
                expiredSessions.forEach(this::removeSession);
            }
        } catch (Exception e) {
            logger.error("清理过期会话失败", e);
        }
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "SID_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(new Random().nextInt());
    }
}
