package com.trading.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 用户会话信息
 * 管理前端用户的登录状态和订阅信息
 */
public class UserSession {
    
    private String sessionId;           // 会话ID
    private String username;            // 用户名
    private String clientId;            // 客户端ID
    private LocalDateTime loginTime;    // 登录时间
    private LocalDateTime lastActiveTime; // 最后活跃时间
    private boolean isActive;           // 是否活跃
    
    // 订阅的交易所
    private final Set<String> subscribedExchanges = new CopyOnWriteArraySet<>();
    
    // 订阅的合约
    private final Set<String> subscribedInstruments = new CopyOnWriteArraySet<>();
    
    // 用户权限
    private final Set<String> permissions = new CopyOnWriteArraySet<>();
    
    // 扩展属性
    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
    
    public UserSession() {
        this.loginTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
        this.isActive = true;
    }
    
    public UserSession(String sessionId, String username, String clientId) {
        this();
        this.sessionId = sessionId;
        this.username = username;
        this.clientId = clientId;
    }
    
    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }
    
    /**
     * 添加订阅交易所
     */
    public void addSubscribedExchange(String exchange) {
        subscribedExchanges.add(exchange);
        updateLastActiveTime();
    }
    
    /**
     * 移除订阅交易所
     */
    public void removeSubscribedExchange(String exchange) {
        subscribedExchanges.remove(exchange);
        updateLastActiveTime();
    }
    
    /**
     * 添加订阅合约
     */
    public void addSubscribedInstrument(String instrument) {
        subscribedInstruments.add(instrument);
        updateLastActiveTime();
    }
    
    /**
     * 移除订阅合约
     */
    public void removeSubscribedInstrument(String instrument) {
        subscribedInstruments.remove(instrument);
        updateLastActiveTime();
    }
    
    /**
     * 检查是否订阅了指定交易所
     */
    public boolean isSubscribedToExchange(String exchange) {
        return subscribedExchanges.contains(exchange);
    }
    
    /**
     * 检查是否订阅了指定合约
     */
    public boolean isSubscribedToInstrument(String instrument) {
        return subscribedInstruments.contains(instrument);
    }
    
    /**
     * 添加权限
     */
    public void addPermission(String permission) {
        permissions.add(permission);
    }
    
    /**
     * 检查是否有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    /**
     * 设置扩展属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
    
    /**
     * 检查会话是否过期
     */
    public boolean isExpired(int timeoutMinutes) {
        return lastActiveTime.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
    
    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }
    
    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public Set<String> getSubscribedExchanges() {
        return new CopyOnWriteArraySet<>(subscribedExchanges);
    }
    
    public Set<String> getSubscribedInstruments() {
        return new CopyOnWriteArraySet<>(subscribedInstruments);
    }
    
    public Set<String> getPermissions() {
        return new CopyOnWriteArraySet<>(permissions);
    }
    
    @Override
    public String toString() {
        return "UserSession{" +
                "sessionId='" + sessionId + '\'' +
                ", username='" + username + '\'' +
                ", clientId='" + clientId + '\'' +
                ", loginTime=" + loginTime +
                ", lastActiveTime=" + lastActiveTime +
                ", isActive=" + isActive +
                ", subscribedExchanges=" + subscribedExchanges +
                ", subscribedInstruments=" + subscribedInstruments +
                '}';
    }
}
