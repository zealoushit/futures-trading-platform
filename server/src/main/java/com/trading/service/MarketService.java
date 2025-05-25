package com.trading.service;

import com.trading.config.TradingConfig;
import com.trading.jni.FemasMarketApiMock;
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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 行情服务类 - Java 8兼容
 * 封装飞马行情API，提供行情推送服务
 */
@Service
public class MarketService extends FemasMarketApiMock {

    private static final Logger logger = LoggerFactory.getLogger(MarketService.class);

    @Autowired
    private TradingConfig tradingConfig;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 请求ID生成器
    private final AtomicInteger requestIdGenerator = new AtomicInteger(1);

    // 存储异步请求的Future
    private final ConcurrentHashMap<Integer, CompletableFuture<ApiResponse<Object>>> pendingRequests =
            new ConcurrentHashMap<>();

    // 已订阅的合约
    private final CopyOnWriteArraySet<String> subscribedInstruments = new CopyOnWriteArraySet<>();

    // 连接状态
    private volatile boolean isConnected = false;
    private volatile boolean isLoggedIn = false;

    @PostConstruct
    public void initialize() {
        logger.info("初始化行情服务...");
        try {
            // 创建API实例
            if (createMarketApi(tradingConfig.getFlowPath() + "md/")) {
                // 注册前置机
                registerFront(tradingConfig.getMdAddress());
                // 初始化
                init();
                logger.info("行情API初始化成功");
            } else {
                logger.error("行情API创建失败");
            }
        } catch (Exception e) {
            logger.error("行情服务初始化失败", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("清理行情服务资源...");
        try {
            if (isLoggedIn) {
                logout();
            }
            release();
        } catch (Exception e) {
            logger.error("清理行情服务资源失败", e);
        }
    }

    /**
     * 用户登录
     */
    public CompletableFuture<ApiResponse<Object>> login() {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isConnected) {
            future.complete(ApiResponse.error("未连接到行情前置机"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            reqUserLogin(
                tradingConfig.getBrokerId(),
                tradingConfig.getUserId(),
                tradingConfig.getPassword()
            );

        } catch (Exception e) {
            logger.error("行情登录请求失败", e);
            future.complete(ApiResponse.error("行情登录请求失败: " + e.getMessage()));
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
            logger.error("行情登出请求失败", e);
            future.complete(ApiResponse.error("行情登出请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 订阅行情
     */
    public CompletableFuture<ApiResponse<Object>> subscribeMarket(String[] instrumentIds) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("行情用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            // 调用父类的方法
            super.subscribeMarketData(instrumentIds);

            // 添加到已订阅列表
            for (String instrumentId : instrumentIds) {
                subscribedInstruments.add(instrumentId);
            }

        } catch (Exception e) {
            logger.error("订阅行情请求失败", e);
            future.complete(ApiResponse.error("订阅行情请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 退订行情
     */
    public CompletableFuture<ApiResponse<Object>> unsubscribeMarket(String[] instrumentIds) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("行情用户未登录"));
            return future;
        }

        try {
            int requestId = requestIdGenerator.getAndIncrement();
            pendingRequests.put(requestId, future);

            // 调用父类的方法
            super.unsubscribeMarketData(instrumentIds);

            // 从已订阅列表移除
            for (String instrumentId : instrumentIds) {
                subscribedInstruments.remove(instrumentId);
            }

        } catch (Exception e) {
            logger.error("退订行情请求失败", e);
            future.complete(ApiResponse.error("退订行情请求失败: " + e.getMessage()));
        }

        return future;
    }

    /**
     * 查询合约信息
     */
    public CompletableFuture<ApiResponse<Object>> queryInstrument(String instrumentId) {
        CompletableFuture<ApiResponse<Object>> future = new CompletableFuture<>();

        if (!isLoggedIn) {
            future.complete(ApiResponse.error("行情用户未登录"));
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

    // 回调方法实现
    @Override
    public void onFrontConnected() {
        logger.info("行情前置机连接成功");
        isConnected = true;

        // 通知前端连接状态
        messagingTemplate.convertAndSend("/topic/market/connection",
            ApiResponse.success("行情前置机连接成功", null));
    }

    @Override
    public void onFrontDisconnected(int reason) {
        logger.warn("行情前置机连接断开, 原因: {}", reason);
        isConnected = false;
        isLoggedIn = false;

        // 通知前端连接状态
        messagingTemplate.convertAndSend("/topic/market/connection",
            ApiResponse.error("行情前置机连接断开"));
    }

    @Override
    public void onRspUserLogin(String tradingDay, String loginTime,
                              String brokerId, String userId,
                              int errorId, String errorMsg) {
        if (errorId == 0) {
            logger.info("行情登录成功 - 交易日: {}, 登录时间: {}", tradingDay, loginTime);
            isLoggedIn = true;

            // 完成登录请求
            completeAllPendingRequests(ApiResponse.success("行情登录成功", null));

            // 通知前端登录状态
            messagingTemplate.convertAndSend("/topic/market/login",
                ApiResponse.success("行情登录成功", null));
        } else {
            logger.error("行情登录失败: {} - {}", errorId, errorMsg);
            completeAllPendingRequests(ApiResponse.error(errorId, "行情登录失败: " + errorMsg));
        }
    }

    @Override
    public void onRtnDepthMarketData(String instrumentId, String updateTime,
                                   double lastPrice, long volume, double turnover,
                                   double openInterest,
                                   double bidPrice1, int bidVolume1,
                                   double askPrice1, int askVolume1,
                                   double bidPrice2, int bidVolume2,
                                   double askPrice2, int askVolume2,
                                   double bidPrice3, int bidVolume3,
                                   double askPrice3, int askVolume3,
                                   double bidPrice4, int bidVolume4,
                                   double askPrice4, int askVolume4,
                                   double bidPrice5, int bidVolume5,
                                   double askPrice5, int askVolume5,
                                   double upperLimitPrice, double lowerLimitPrice,
                                   double preClosePrice, double openPrice,
                                   double highestPrice, double lowestPrice) {

        // 构造行情数据并推送给前端
        ConcurrentHashMap<String, Object> marketData = new ConcurrentHashMap<>();
        marketData.put("instrumentId", instrumentId);
        marketData.put("updateTime", updateTime);
        marketData.put("lastPrice", lastPrice);
        marketData.put("volume", volume);
        marketData.put("turnover", turnover);
        marketData.put("openInterest", openInterest);

        // 五档买卖盘
        ConcurrentHashMap<String, Object> bidData = new ConcurrentHashMap<>();
        bidData.put("price1", bidPrice1);
        bidData.put("volume1", bidVolume1);
        bidData.put("price2", bidPrice2);
        bidData.put("volume2", bidVolume2);
        bidData.put("price3", bidPrice3);
        bidData.put("volume3", bidVolume3);
        bidData.put("price4", bidPrice4);
        bidData.put("volume4", bidVolume4);
        bidData.put("price5", bidPrice5);
        bidData.put("volume5", bidVolume5);

        ConcurrentHashMap<String, Object> askData = new ConcurrentHashMap<>();
        askData.put("price1", askPrice1);
        askData.put("volume1", askVolume1);
        askData.put("price2", askPrice2);
        askData.put("volume2", askVolume2);
        askData.put("price3", askPrice3);
        askData.put("volume3", askVolume3);
        askData.put("price4", askPrice4);
        askData.put("volume4", askVolume4);
        askData.put("price5", askPrice5);
        askData.put("volume5", askVolume5);

        marketData.put("bid", bidData);
        marketData.put("ask", askData);

        // 价格信息
        marketData.put("upperLimitPrice", upperLimitPrice);
        marketData.put("lowerLimitPrice", lowerLimitPrice);
        marketData.put("preClosePrice", preClosePrice);
        marketData.put("openPrice", openPrice);
        marketData.put("highestPrice", highestPrice);
        marketData.put("lowestPrice", lowestPrice);

        // 推送到特定合约的主题
        messagingTemplate.convertAndSend("/topic/market/data/" + instrumentId,
            ApiResponse.success("行情数据", marketData));

        // 推送到通用行情主题
        messagingTemplate.convertAndSend("/topic/market/data",
            ApiResponse.success("行情数据", marketData));
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

    public CopyOnWriteArraySet<String> getSubscribedInstruments() {
        return new CopyOnWriteArraySet<>(subscribedInstruments);
    }
}
