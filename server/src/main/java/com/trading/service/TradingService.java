package com.trading.service;

import com.trading.config.TradingConfig;
import com.trading.jni.FemasTraderApiMock;
import com.trading.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 交易服务类 - Java 8兼容
 * 封装飞马交易API，提供业务接口
 */
@Service
public class TradingService extends FemasTraderApiMock {

    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);

    @Autowired
    private TradingConfig tradingConfig;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 请求ID生成器
    private final AtomicInteger requestIdGenerator = new AtomicInteger(1);

    // 存储异步请求的Future
    private final ConcurrentHashMap<Integer, CompletableFuture<ApiResponse<Object>>> pendingRequests =
            new ConcurrentHashMap<>();

    // 连接状态
    private volatile boolean isConnected = false;
    private volatile boolean isLoggedIn = false;

    @PostConstruct
    public void initialize() {
        logger.info("初始化交易服务...");
        try {
            // 创建API实例
            if (createTraderApi(tradingConfig.getFlowPath())) {
                // 注册前置机
                registerFront(tradingConfig.getFrontAddress());
                // 初始化
                init();
                logger.info("交易API初始化成功");
            } else {
                logger.error("交易API创建失败");
            }
        } catch (Exception e) {
            logger.error("交易服务初始化失败", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("清理交易服务资源...");
        try {
            if (isLoggedIn) {
                logout();
            }
            release();
        } catch (Exception e) {
            logger.error("清理交易服务资源失败", e);
        }
    }

    /**
     * 用户登录
     */
    public CompletableFuture<ApiResponse<Object>> login() {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isConnected) {
            future.complete(ApiResponse.error("未连接到交易前置机"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            // 先进行认证
            reqAuthenticate(
                tradingConfig.getBrokerId(),
                tradingConfig.getUserId(),
                tradingConfig.getUserProductInfo(),
                tradingConfig.getAuthCode()
            );

        } catch (Exception e) {
            logger.error("登录请求失败", e);
            future.complete(ApiResponse.error("登录请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 用户登出
     */
    public CompletableFuture<ApiResponse<Object>> logout() {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqUserLogout(tradingConfig.getBrokerId(), tradingConfig.getUserId());

        } catch (Exception e) {
            logger.error("登出请求失败", e);
            future.complete(ApiResponse.error("登出请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 下单
     */
    public CompletableFuture<ApiResponse<Object>> placeOrder(String instrumentId,
                                                           char direction,
                                                           char offsetFlag,
                                                           double price,
                                                           int volume) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqOrderInsert(instrumentId, direction, offsetFlag, price, volume,
                          '2', '3', '1'); // 限价单，当日有效，任何数量

        } catch (Exception e) {
            logger.error("下单请求失败", e);
            future.complete(ApiResponse.error("下单请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 撤单
     */
    public CompletableFuture<ApiResponse<Object>> cancelOrder(String orderRef,
                                                            int frontId,
                                                            int sessionId) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqOrderAction(orderRef, frontId, sessionId, '0'); // 撤单

        } catch (Exception e) {
            logger.error("撤单请求失败", e);
            future.complete(ApiResponse.error("撤单请求失败: " + e.getMessage()));
        }

        return future;
    }

    // 回调方法实现
    @Override
    public void onFrontConnected() {
        logger.info("交易前置机连接成功");
        isConnected = true;

        // 通知前端连接状态
        messagingTemplate.convertAndSend("/topic/connection",
            ApiResponse.success("交易前置机连接成功", null));
    }

    @Override
    public void onFrontDisconnected(int reason) {
        logger.warn("交易前置机连接断开, 原因: {}", reason);
        isConnected = false;
        isLoggedIn = false;

        // 通知前端连接状态
        messagingTemplate.convertAndSend("/topic/connection",
            ApiResponse.error("交易前置机连接断开"));
    }

    @Override
    public void onRspAuthenticate(String authCode, int errorId, String errorMsg) {
        if (errorId == 0) {
            logger.info("认证成功，开始登录");
            // 认证成功后进行登录
            reqUserLogin(
                tradingConfig.getBrokerId(),
                tradingConfig.getUserId(),
                tradingConfig.getPassword()
            );
        } else {
            logger.error("认证失败: {} - {}", errorId, errorMsg);
            // 完成所有等待认证的请求
            completeAllPendingRequests(ApiResponse.error(errorId, "认证失败: " + errorMsg));
        }
    }

    @Override
    public void onRspUserLogin(String tradingDay, String loginTime,
                              String brokerId, String userId,
                              int errorId, String errorMsg) {
        if (errorId == 0) {
            logger.info("登录成功 - 交易日: {}, 登录时间: {}", tradingDay, loginTime);
            isLoggedIn = true;

            // 完成登录请求
            completeAllPendingRequests(ApiResponse.success("登录成功", null));

            // 通知前端登录状态
            messagingTemplate.convertAndSend("/topic/login",
                ApiResponse.success("登录成功", null));
        } else {
            logger.error("登录失败: {} - {}", errorId, errorMsg);
            completeAllPendingRequests(ApiResponse.error(errorId, "登录失败: " + errorMsg));
        }
    }

    @Override
    public void onRtnOrder(String orderSysId, String orderRef, String instrumentId,
                          char direction, char offsetFlag, double price,
                          int volume, char orderStatus) {
        logger.info("报单回报: {} - {} - 状态: {}", orderSysId, instrumentId, orderStatus);

        // 构造报单数据并推送给前端
        ConcurrentHashMap<String, Object> orderData = new ConcurrentHashMap<>();
        orderData.put("orderSysId", orderSysId);
        orderData.put("orderRef", orderRef);
        orderData.put("instrumentId", instrumentId);
        orderData.put("direction", direction);
        orderData.put("offsetFlag", offsetFlag);
        orderData.put("price", price);
        orderData.put("volume", volume);
        orderData.put("orderStatus", orderStatus);

        messagingTemplate.convertAndSend("/topic/orders",
            ApiResponse.success("报单回报", orderData));
    }

    @Override
    public void onRtnTrade(String tradeId, String orderRef, String instrumentId,
                          char direction, char offsetFlag, double price,
                          int volume, String tradeTime) {
        logger.info("成交回报: {} - {} - 价格: {} - 数量: {}", tradeId, instrumentId, price, volume);

        // 构造成交数据并推送给前端
        ConcurrentHashMap<String, Object> tradeData = new ConcurrentHashMap<>();
        tradeData.put("tradeId", tradeId);
        tradeData.put("orderRef", orderRef);
        tradeData.put("instrumentId", instrumentId);
        tradeData.put("direction", direction);
        tradeData.put("offsetFlag", offsetFlag);
        tradeData.put("price", price);
        tradeData.put("volume", volume);
        tradeData.put("tradeTime", tradeTime);

        messagingTemplate.convertAndSend("/topic/trades",
            ApiResponse.success("成交回报", tradeData));
    }

    /**
     * 查询持仓
     */
    public CompletableFuture<ApiResponse<Object>> queryPosition(String instrumentId) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqQryInvestorPosition(
                tradingConfig.getBrokerId(),
                tradingConfig.getInvestorId(),
                instrumentId != null ? instrumentId : ""
            );

        } catch (Exception e) {
            logger.error("查询持仓请求失败", e);
            future.complete(ApiResponse.error("查询持仓请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 查询资金账户
     */
    public CompletableFuture<ApiResponse<Object>> queryAccount() {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqQryTradingAccount(
                tradingConfig.getBrokerId(),
                tradingConfig.getInvestorId()
            );

        } catch (Exception e) {
            logger.error("查询资金账户请求失败", e);
            future.complete(ApiResponse.error("查询资金账户请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 查询报单
     */
    public CompletableFuture<ApiResponse<Object>> queryOrders(String instrumentId) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqQryOrder(
                tradingConfig.getBrokerId(),
                tradingConfig.getInvestorId(),
                instrumentId != null ? instrumentId : ""
            );

        } catch (Exception e) {
            logger.error("查询报单请求失败", e);
            future.complete(ApiResponse.error("查询报单请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 查询成交
     */
    public CompletableFuture<ApiResponse<Object>> queryTrades(String instrumentId) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqQryTrade(
                tradingConfig.getBrokerId(),
                tradingConfig.getInvestorId(),
                instrumentId != null ? instrumentId : ""
            );

        } catch (Exception e) {
            logger.error("查询成交请求失败", e);
            future.complete(ApiResponse.error("查询成交请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 查询合约
     */
    public CompletableFuture<ApiResponse<Object>> queryInstrument(String instrumentId) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqQryInstrument(instrumentId);

        } catch (Exception e) {
            logger.error("查询合约请求失败", e);
            future.complete(ApiResponse.error("查询合约请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 完成所有等待的请求
     */
    private void completeAllPendingRequests(ApiResponse<Object> response) {
        pendingRequests.values().forEach(future -> {
            if (!future.isDone()) {
                future.complete(response);
            }
        });
        pendingRequests.clear();
    }

    // Getters
    public boolean isConnected() {
        return isConnected;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}
