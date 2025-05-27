package com.trading.service;

import com.trading.model.MarketDataSnapshot;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 行情数据缓存服务
 * 缓存完整的行情数据，提供快速查询和推送
 */
@Service
public class MarketDataCacheService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataCacheService.class);

    // 行情数据缓存 - instrumentId -> MarketDataSnapshot
    private final ConcurrentHashMap<String, MarketDataSnapshot> marketDataCache = new ConcurrentHashMap<>();
    
    // 交易所到合约的映射 - exchangeId -> Set<instrumentId>
    private final ConcurrentHashMap<String, Set<String>> exchangeInstruments = new ConcurrentHashMap<>();
    
    // 合约到交易所的映射 - instrumentId -> exchangeId
    private final ConcurrentHashMap<String, String> instrumentExchanges = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong totalUpdates = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    // 数据清理任务
    private ScheduledExecutorService cleanupExecutor;
    
    // 配置参数
    private static final int STALE_DATA_SECONDS = 300;  // 数据过期时间（5分钟）
    private static final int CLEANUP_INTERVAL_MINUTES = 10;  // 清理任务间隔
    
    @PostConstruct
    public void initialize() {
        logger.info("初始化行情数据缓存服务...");
        
        // 启动数据清理任务
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MarketDataCleanup");
            t.setDaemon(true);
            return t;
        });
        
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupStaleData,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        logger.info("行情数据缓存服务初始化完成");
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("清理行情数据缓存服务...");
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
        }
    }
    
    /**
     * 更新行情数据
     */
    public void updateMarketData(String instrumentId, String exchangeId,
                                String updateTime, double lastPrice, long volume, double turnover, double openInterest,
                                double bidPrice1, int bidVolume1, double askPrice1, int askVolume1,
                                double bidPrice2, int bidVolume2, double askPrice2, int askVolume2,
                                double bidPrice3, int bidVolume3, double askPrice3, int askVolume3,
                                double bidPrice4, int bidVolume4, double askPrice4, int askVolume4,
                                double bidPrice5, int bidVolume5, double askPrice5, int askVolume5,
                                double upperLimitPrice, double lowerLimitPrice,
                                double preClosePrice, double openPrice, double highestPrice, double lowestPrice) {
        
        // 获取或创建行情快照
        MarketDataSnapshot snapshot = marketDataCache.computeIfAbsent(instrumentId, 
            k -> new MarketDataSnapshot(instrumentId, exchangeId));
        
        // 更新数据
        snapshot.updateMarketData(updateTime, lastPrice, volume, turnover, openInterest,
                                bidPrice1, bidVolume1, askPrice1, askVolume1,
                                bidPrice2, bidVolume2, askPrice2, askVolume2,
                                bidPrice3, bidVolume3, askPrice3, askVolume3,
                                bidPrice4, bidVolume4, askPrice4, askVolume4,
                                bidPrice5, bidVolume5, askPrice5, askVolume5,
                                upperLimitPrice, lowerLimitPrice,
                                preClosePrice, openPrice, highestPrice, lowestPrice);
        
        // 更新交易所映射
        updateExchangeMapping(instrumentId, exchangeId);
        
        // 更新统计
        totalUpdates.incrementAndGet();
        
        if (logger.isDebugEnabled()) {
            logger.debug("更新行情数据: {} - {}", instrumentId, lastPrice);
        }
    }
    
    /**
     * 获取行情数据
     */
    public MarketDataSnapshot getMarketData(String instrumentId) {
        MarketDataSnapshot snapshot = marketDataCache.get(instrumentId);
        if (snapshot != null) {
            cacheHits.incrementAndGet();
            return snapshot.createSnapshot(); // 返回副本
        } else {
            cacheMisses.incrementAndGet();
            return null;
        }
    }
    
    /**
     * 获取指定交易所的所有行情数据
     */
    public List<MarketDataSnapshot> getMarketDataByExchange(String exchangeId) {
        Set<String> instruments = exchangeInstruments.get(exchangeId);
        if (instruments == null) {
            return Collections.emptyList();
        }
        
        return instruments.stream()
                .map(this::getMarketData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取多个合约的行情数据
     */
    public List<MarketDataSnapshot> getMarketDataByInstruments(Collection<String> instrumentIds) {
        return instrumentIds.stream()
                .map(this::getMarketData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定交易所列表的行情数据
     */
    public List<MarketDataSnapshot> getMarketDataByExchanges(Collection<String> exchangeIds) {
        return exchangeIds.stream()
                .flatMap(exchangeId -> getMarketDataByExchange(exchangeId).stream())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有行情数据
     */
    public List<MarketDataSnapshot> getAllMarketData() {
        return marketDataCache.values().stream()
                .map(MarketDataSnapshot::createSnapshot)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取活跃的行情数据（非过期）
     */
    public List<MarketDataSnapshot> getActiveMarketData() {
        return marketDataCache.values().stream()
                .filter(snapshot -> !snapshot.isStale(STALE_DATA_SECONDS))
                .map(MarketDataSnapshot::createSnapshot)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定交易所的活跃行情数据
     */
    public List<MarketDataSnapshot> getActiveMarketDataByExchange(String exchangeId) {
        Set<String> instruments = exchangeInstruments.get(exchangeId);
        if (instruments == null) {
            return Collections.emptyList();
        }
        
        return instruments.stream()
                .map(marketDataCache::get)
                .filter(Objects::nonNull)
                .filter(snapshot -> !snapshot.isStale(STALE_DATA_SECONDS))
                .map(MarketDataSnapshot::createSnapshot)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查合约是否存在
     */
    public boolean hasInstrument(String instrumentId) {
        return marketDataCache.containsKey(instrumentId);
    }
    
    /**
     * 获取交易所列表
     */
    public Set<String> getExchanges() {
        return new HashSet<>(exchangeInstruments.keySet());
    }
    
    /**
     * 获取指定交易所的合约列表
     */
    public Set<String> getInstrumentsByExchange(String exchangeId) {
        Set<String> instruments = exchangeInstruments.get(exchangeId);
        return instruments != null ? new HashSet<>(instruments) : Collections.emptySet();
    }
    
    /**
     * 获取合约所属的交易所
     */
    public String getExchangeByInstrument(String instrumentId) {
        return instrumentExchanges.get(instrumentId);
    }
    
    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInstruments", marketDataCache.size());
        stats.put("totalExchanges", exchangeInstruments.size());
        stats.put("totalUpdates", totalUpdates.get());
        stats.put("cacheHits", cacheHits.get());
        stats.put("cacheMisses", cacheMisses.get());
        
        // 计算缓存命中率
        long totalRequests = cacheHits.get() + cacheMisses.get();
        double hitRate = totalRequests > 0 ? (double) cacheHits.get() / totalRequests * 100 : 0;
        stats.put("hitRate", String.format("%.2f%%", hitRate));
        
        // 按交易所统计合约数量
        Map<String, Integer> exchangeStats = exchangeInstruments.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                ));
        stats.put("instrumentsByExchange", exchangeStats);
        
        // 活跃数据统计
        long activeCount = marketDataCache.values().stream()
                .mapToLong(snapshot -> snapshot.isStale(STALE_DATA_SECONDS) ? 0 : 1)
                .sum();
        stats.put("activeInstruments", activeCount);
        
        return stats;
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        logger.info("清空行情数据缓存");
        marketDataCache.clear();
        exchangeInstruments.clear();
        instrumentExchanges.clear();
        totalUpdates.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
    }
    
    /**
     * 更新交易所映射关系
     */
    private void updateExchangeMapping(String instrumentId, String exchangeId) {
        // 更新交易所到合约的映射
        exchangeInstruments.computeIfAbsent(exchangeId, k -> ConcurrentHashMap.newKeySet())
                           .add(instrumentId);
        
        // 更新合约到交易所的映射
        instrumentExchanges.put(instrumentId, exchangeId);
    }
    
    /**
     * 清理过期数据
     */
    private void cleanupStaleData() {
        try {
            List<String> staleInstruments = marketDataCache.entrySet().stream()
                    .filter(entry -> entry.getValue().isStale(STALE_DATA_SECONDS))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            if (!staleInstruments.isEmpty()) {
                logger.info("清理过期行情数据: {} 个合约", staleInstruments.size());
                
                for (String instrumentId : staleInstruments) {
                    MarketDataSnapshot removed = marketDataCache.remove(instrumentId);
                    if (removed != null) {
                        String exchangeId = removed.getExchangeId();
                        
                        // 从交易所映射中移除
                        Set<String> instruments = exchangeInstruments.get(exchangeId);
                        if (instruments != null) {
                            instruments.remove(instrumentId);
                            if (instruments.isEmpty()) {
                                exchangeInstruments.remove(exchangeId);
                            }
                        }
                        
                        // 从合约映射中移除
                        instrumentExchanges.remove(instrumentId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("清理过期行情数据失败", e);
        }
    }
}
