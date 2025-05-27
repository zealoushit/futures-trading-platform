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

    @Autowired
    private MarketDataCacheService marketDataCacheService;

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
                logger.info("行情API初始化成功，将自动连接和登录");
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

        // 自动登录
        logger.info("开始自动登录行情服务...");
        reqUserLogin(
            tradingConfig.getBrokerId(),
            tradingConfig.getUserId(),
            tradingConfig.getPassword()
        );
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

            // 自动订阅主要合约以缓存行情数据
            subscribeMainInstruments();
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

        logger.debug("收到行情数据: {} - {}", instrumentId, lastPrice);

        // 获取交易所ID（从合约代码推断）
        String exchangeId = getExchangeIdFromInstrument(instrumentId);

        // 更新缓存
        marketDataCacheService.updateMarketData(
            instrumentId, exchangeId, updateTime, lastPrice, volume, turnover, openInterest,
            bidPrice1, bidVolume1, askPrice1, askVolume1,
            bidPrice2, bidVolume2, askPrice2, askVolume2,
            bidPrice3, bidVolume3, askPrice3, askVolume3,
            bidPrice4, bidVolume4, askPrice4, askVolume4,
            bidPrice5, bidVolume5, askPrice5, askVolume5,
            upperLimitPrice, lowerLimitPrice,
            preClosePrice, openPrice, highestPrice, lowestPrice
        );

        // 注意：不再直接推送到前端，由MarketDataPushService统一推送
        logger.debug("行情数据已更新到缓存: {}", instrumentId);
    }

    /**
     * 自动订阅主要合约
     */
    private void subscribeMainInstruments() {
        logger.info("开始自动订阅主要合约...");

        // 定义主要合约列表
        String[] mainInstruments = {
            "rb2405", "rb2409", "rb2501",  // 螺纹钢
            "cu2405", "cu2409", "cu2501",  // 铜
            "au2406", "au2408", "au2412",  // 黄金
            "ag2406", "ag2408", "ag2412",  // 白银
            "ni2405", "ni2409", "ni2501",  // 镍
            "zn2405", "zn2409", "zn2501",  // 锌
            "al2405", "al2409", "al2501",  // 铝
            "IF2405", "IF2406", "IF2409",  // 沪深300
            "IC2405", "IC2406", "IC2409",  // 中证500
            "IH2405", "IH2406", "IH2409"   // 上证50
        };

        // 订阅主要合约
        super.subscribeMarketData(mainInstruments);

        // 添加到已订阅列表
        for (String instrument : mainInstruments) {
            subscribedInstruments.add(instrument);
        }

        logger.info("已自动订阅 {} 个主要合约", mainInstruments.length);
    }

    /**
     * 从合约代码推断交易所ID
     */
    private String getExchangeIdFromInstrument(String instrumentId) {
        if (instrumentId == null || instrumentId.isEmpty()) {
            return "UNKNOWN";
        }

        String upperInstrument = instrumentId.toUpperCase();

        // 上海期货交易所 (SHFE)
        if (upperInstrument.startsWith("CU") || upperInstrument.startsWith("AL") ||
            upperInstrument.startsWith("ZN") || upperInstrument.startsWith("PB") ||
            upperInstrument.startsWith("NI") || upperInstrument.startsWith("SN") ||
            upperInstrument.startsWith("AU") || upperInstrument.startsWith("AG") ||
            upperInstrument.startsWith("RB") || upperInstrument.startsWith("WR") ||
            upperInstrument.startsWith("HC") || upperInstrument.startsWith("FU") ||
            upperInstrument.startsWith("BU") || upperInstrument.startsWith("RU")) {
            return "SHFE";
        }

        // 中国金融期货交易所 (CFFEX)
        if (upperInstrument.startsWith("IF") || upperInstrument.startsWith("IC") ||
            upperInstrument.startsWith("IH") || upperInstrument.startsWith("T") ||
            upperInstrument.startsWith("TF") || upperInstrument.startsWith("TS")) {
            return "CFFEX";
        }

        // 大连商品交易所 (DCE)
        if (upperInstrument.startsWith("A") || upperInstrument.startsWith("B") ||
            upperInstrument.startsWith("C") || upperInstrument.startsWith("CS") ||
            upperInstrument.startsWith("I") || upperInstrument.startsWith("J") ||
            upperInstrument.startsWith("JM") || upperInstrument.startsWith("L") ||
            upperInstrument.startsWith("M") || upperInstrument.startsWith("P") ||
            upperInstrument.startsWith("PP") || upperInstrument.startsWith("V") ||
            upperInstrument.startsWith("Y") || upperInstrument.startsWith("JD") ||
            upperInstrument.startsWith("LH") || upperInstrument.startsWith("EB") ||
            upperInstrument.startsWith("EG") || upperInstrument.startsWith("RR") ||
            upperInstrument.startsWith("PG")) {
            return "DCE";
        }

        // 郑州商品交易所 (CZCE)
        if (upperInstrument.startsWith("CF") || upperInstrument.startsWith("CY") ||
            upperInstrument.startsWith("FG") || upperInstrument.startsWith("JR") ||
            upperInstrument.startsWith("LR") || upperInstrument.startsWith("MA") ||
            upperInstrument.startsWith("OI") || upperInstrument.startsWith("PM") ||
            upperInstrument.startsWith("RI") || upperInstrument.startsWith("RM") ||
            upperInstrument.startsWith("RS") || upperInstrument.startsWith("SF") ||
            upperInstrument.startsWith("SM") || upperInstrument.startsWith("SR") ||
            upperInstrument.startsWith("TA") || upperInstrument.startsWith("WH") ||
            upperInstrument.startsWith("ZC") || upperInstrument.startsWith("AP") ||
            upperInstrument.startsWith("CJ") || upperInstrument.startsWith("UR") ||
            upperInstrument.startsWith("SA") || upperInstrument.startsWith("PF")) {
            return "CZCE";
        }

        // 上海国际能源交易中心 (INE)
        if (upperInstrument.startsWith("SC") || upperInstrument.startsWith("NR") ||
            upperInstrument.startsWith("LU") || upperInstrument.startsWith("BC")) {
            return "INE";
        }

        return "UNKNOWN";
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
