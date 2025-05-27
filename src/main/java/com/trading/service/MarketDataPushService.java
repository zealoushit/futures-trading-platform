package com.trading.service;

import com.trading.model.ApiResponse;
import com.trading.model.MarketDataSnapshot;
import com.trading.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 行情数据推送服务
 * 按500ms频率向前端推送订阅的行情数据
 */
@Service
public class MarketDataPushService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataPushService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private MarketDataCacheService marketDataCacheService;

    // 推送任务执行器
    private ScheduledExecutorService pushExecutor;

    // 推送统计
    private final AtomicLong totalPushCount = new AtomicLong(0);
    private final AtomicLong successPushCount = new AtomicLong(0);
    private final AtomicLong failedPushCount = new AtomicLong(0);

    // 配置参数
    private static final int PUSH_INTERVAL_MS = 500;  // 推送间隔500ms
    private static final int PUSH_THREAD_POOL_SIZE = 2;  // 推送线程池大小

    @PostConstruct
    public void initialize() {
        logger.info("初始化行情数据推送服务...");

        // 创建推送任务执行器
        pushExecutor = Executors.newScheduledThreadPool(PUSH_THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, "MarketDataPush");
            t.setDaemon(true);
            return t;
        });

        // 启动定时推送任务
        pushExecutor.scheduleAtFixedRate(
            this::pushMarketDataToClients,
            1000,  // 延迟1秒启动
            PUSH_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );

        logger.info("行情数据推送服务初始化完成，推送间隔: {}ms", PUSH_INTERVAL_MS);
    }

    @PreDestroy
    public void cleanup() {
        logger.info("清理行情数据推送服务...");
        if (pushExecutor != null) {
            pushExecutor.shutdown();
            try {
                if (!pushExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    pushExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                pushExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 向所有客户端推送行情数据
     */
    private void pushMarketDataToClients() {
        try {
            List<UserSession> activeSessions = userSessionService.getActiveSessions();
            if (activeSessions.isEmpty()) {
                return;
            }

            // 按订阅的交易所分组会话
            Map<Set<String>, List<UserSession>> sessionsByExchanges = activeSessions.stream()
                    .filter(session -> !session.getSubscribedExchanges().isEmpty())
                    .collect(Collectors.groupingBy(UserSession::getSubscribedExchanges));

            // 为每组会话推送对应的行情数据
            for (Map.Entry<Set<String>, List<UserSession>> entry : sessionsByExchanges.entrySet()) {
                Set<String> exchanges = entry.getKey();
                List<UserSession> sessions = entry.getValue();

                // 获取这些交易所的行情数据
                List<MarketDataSnapshot> marketData = marketDataCacheService.getActiveMarketDataByExchanges(exchanges);
                if (!marketData.isEmpty()) {
                    pushToSessions(sessions, marketData);
                }
            }

            // 处理订阅了特定合约的会话
            pushToInstrumentSubscribers(activeSessions);

        } catch (Exception e) {
            logger.error("推送行情数据失败", e);
            failedPushCount.incrementAndGet();
        }
    }

    /**
     * 向订阅了特定合约的会话推送数据
     */
    private void pushToInstrumentSubscribers(List<UserSession> activeSessions) {
        // 按订阅的合约分组会话
        Map<Set<String>, List<UserSession>> sessionsByInstruments = activeSessions.stream()
                .filter(session -> !session.getSubscribedInstruments().isEmpty())
                .collect(Collectors.groupingBy(UserSession::getSubscribedInstruments));

        for (Map.Entry<Set<String>, List<UserSession>> entry : sessionsByInstruments.entrySet()) {
            Set<String> instruments = entry.getKey();
            List<UserSession> sessions = entry.getValue();

            // 获取这些合约的行情数据
            List<MarketDataSnapshot> marketData = marketDataCacheService.getMarketDataByInstruments(instruments);
            if (!marketData.isEmpty()) {
                pushToSessions(sessions, marketData);
            }
        }
    }

    /**
     * 向指定会话推送行情数据
     */
    private void pushToSessions(List<UserSession> sessions, List<MarketDataSnapshot> marketData) {
        if (sessions.isEmpty() || marketData.isEmpty()) {
            return;
        }

        // 构造推送消息
        Map<String, Object> pushData = new HashMap<>();
        pushData.put("timestamp", System.currentTimeMillis());
        pushData.put("count", marketData.size());
        pushData.put("data", convertToClientFormat(marketData));

        ApiResponse<Object> response = ApiResponse.success("行情数据推送", pushData);

        // 向每个会话推送
        for (UserSession session : sessions) {
            try {
                // 推送到用户专用主题
                String userTopic = "/topic/market/user/" + session.getSessionId();
                messagingTemplate.convertAndSend(userTopic, response);

                totalPushCount.incrementAndGet();
                successPushCount.incrementAndGet();

                if (logger.isDebugEnabled()) {
                    logger.debug("推送行情数据到会话: sessionId={}, count={}", 
                               session.getSessionId(), marketData.size());
                }

            } catch (Exception e) {
                logger.warn("推送行情数据到会话失败: sessionId={}, error={}", 
                           session.getSessionId(), e.getMessage());
                failedPushCount.incrementAndGet();
            }
        }
    }

    /**
     * 立即推送指定交易所的行情数据
     */
    public void pushExchangeData(String exchangeId) {
        try {
            List<UserSession> sessions = userSessionService.getSessionsByExchange(exchangeId);
            if (sessions.isEmpty()) {
                return;
            }

            List<MarketDataSnapshot> marketData = marketDataCacheService.getActiveMarketDataByExchange(exchangeId);
            if (!marketData.isEmpty()) {
                pushToSessions(sessions, marketData);
                logger.info("立即推送交易所行情数据: exchangeId={}, sessions={}, instruments={}", 
                           exchangeId, sessions.size(), marketData.size());
            }
        } catch (Exception e) {
            logger.error("立即推送交易所行情数据失败: exchangeId={}", exchangeId, e);
        }
    }

    /**
     * 立即推送指定合约的行情数据
     */
    public void pushInstrumentData(String instrumentId) {
        try {
            List<UserSession> sessions = userSessionService.getSessionsByInstrument(instrumentId);
            if (sessions.isEmpty()) {
                return;
            }

            MarketDataSnapshot marketData = marketDataCacheService.getMarketData(instrumentId);
            if (marketData != null) {
                pushToSessions(sessions, Collections.singletonList(marketData));
                logger.info("立即推送合约行情数据: instrumentId={}, sessions={}", 
                           instrumentId, sessions.size());
            }
        } catch (Exception e) {
            logger.error("立即推送合约行情数据失败: instrumentId={}", instrumentId, e);
        }
    }

    /**
     * 向指定会话推送全量行情数据
     */
    public void pushFullDataToSession(String sessionId) {
        try {
            UserSession session = userSessionService.getSession(sessionId);
            if (session == null || !session.isActive()) {
                return;
            }

            // 获取用户订阅的行情数据
            List<MarketDataSnapshot> marketData = new ArrayList<>();

            // 添加订阅的交易所数据
            if (!session.getSubscribedExchanges().isEmpty()) {
                marketData.addAll(marketDataCacheService.getActiveMarketDataByExchanges(
                    session.getSubscribedExchanges()));
            }

            // 添加订阅的合约数据
            if (!session.getSubscribedInstruments().isEmpty()) {
                marketData.addAll(marketDataCacheService.getMarketDataByInstruments(
                    session.getSubscribedInstruments()));
            }

            // 去重
            Map<String, MarketDataSnapshot> uniqueData = marketData.stream()
                    .collect(Collectors.toMap(
                        MarketDataSnapshot::getInstrumentId,
                        snapshot -> snapshot,
                        (existing, replacement) -> replacement
                    ));

            if (!uniqueData.isEmpty()) {
                pushToSessions(Collections.singletonList(session), 
                             new ArrayList<>(uniqueData.values()));
                logger.info("推送全量行情数据到会话: sessionId={}, count={}", 
                           sessionId, uniqueData.size());
            }

        } catch (Exception e) {
            logger.error("推送全量行情数据到会话失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 转换为客户端格式
     */
    private List<Map<String, Object>> convertToClientFormat(List<MarketDataSnapshot> marketData) {
        return marketData.stream().map(this::convertSnapshotToMap).collect(Collectors.toList());
    }

    /**
     * 转换单个行情快照为Map格式
     */
    private Map<String, Object> convertSnapshotToMap(MarketDataSnapshot snapshot) {
        Map<String, Object> data = new HashMap<>();
        data.put("instrumentId", snapshot.getInstrumentId());
        data.put("exchangeId", snapshot.getExchangeId());
        data.put("instrumentName", snapshot.getInstrumentName());
        data.put("updateTime", snapshot.getUpdateTime());
        data.put("lastPrice", snapshot.getLastPrice());
        data.put("preClosePrice", snapshot.getPreClosePrice());
        data.put("openPrice", snapshot.getOpenPrice());
        data.put("highestPrice", snapshot.getHighestPrice());
        data.put("lowestPrice", snapshot.getLowestPrice());
        data.put("upperLimitPrice", snapshot.getUpperLimitPrice());
        data.put("lowerLimitPrice", snapshot.getLowerLimitPrice());
        data.put("volume", snapshot.getVolume());
        data.put("turnover", snapshot.getTurnover());
        data.put("openInterest", snapshot.getOpenInterest());
        data.put("priceChange", snapshot.getPriceChange());
        data.put("priceChangePercent", snapshot.getPriceChangePercent());

        // 五档买卖盘
        Map<String, Object> bid = new HashMap<>();
        bid.put("price1", snapshot.getBidPrice1());
        bid.put("volume1", snapshot.getBidVolume1());
        bid.put("price2", snapshot.getBidPrice2());
        bid.put("volume2", snapshot.getBidVolume2());
        bid.put("price3", snapshot.getBidPrice3());
        bid.put("volume3", snapshot.getBidVolume3());
        bid.put("price4", snapshot.getBidPrice4());
        bid.put("volume4", snapshot.getBidVolume4());
        bid.put("price5", snapshot.getBidPrice5());
        bid.put("volume5", snapshot.getBidVolume5());

        Map<String, Object> ask = new HashMap<>();
        ask.put("price1", snapshot.getAskPrice1());
        ask.put("volume1", snapshot.getAskVolume1());
        ask.put("price2", snapshot.getAskPrice2());
        ask.put("volume2", snapshot.getAskVolume2());
        ask.put("price3", snapshot.getAskPrice3());
        ask.put("volume3", snapshot.getAskVolume3());
        ask.put("price4", snapshot.getAskPrice4());
        ask.put("volume4", snapshot.getAskVolume4());
        ask.put("price5", snapshot.getAskPrice5());
        ask.put("volume5", snapshot.getAskVolume5());

        data.put("bid", bid);
        data.put("ask", ask);

        return data;
    }

    /**
     * 获取推送统计信息
     */
    public Map<String, Object> getPushStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPushCount", totalPushCount.get());
        stats.put("successPushCount", successPushCount.get());
        stats.put("failedPushCount", failedPushCount.get());
        stats.put("pushInterval", PUSH_INTERVAL_MS + "ms");

        // 计算成功率
        long total = totalPushCount.get();
        double successRate = total > 0 ? (double) successPushCount.get() / total * 100 : 0;
        stats.put("successRate", String.format("%.2f%%", successRate));

        return stats;
    }
}
