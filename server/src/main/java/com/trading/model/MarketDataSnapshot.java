package com.trading.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 行情数据快照
 * 用于缓存和推送行情数据
 */
public class MarketDataSnapshot {
    
    private String instrumentId;        // 合约代码
    private String exchangeId;          // 交易所代码
    private String instrumentName;      // 合约名称
    
    // 价格信息
    private volatile double lastPrice;          // 最新价
    private volatile double preClosePrice;      // 昨收价
    private volatile double openPrice;          // 开盘价
    private volatile double highestPrice;       // 最高价
    private volatile double lowestPrice;        // 最低价
    private volatile double upperLimitPrice;    // 涨停价
    private volatile double lowerLimitPrice;    // 跌停价
    
    // 成交信息
    private volatile long volume;               // 成交量
    private volatile double turnover;           // 成交金额
    private volatile double openInterest;       // 持仓量
    
    // 五档买盘
    private volatile double bidPrice1, bidPrice2, bidPrice3, bidPrice4, bidPrice5;
    private volatile int bidVolume1, bidVolume2, bidVolume3, bidVolume4, bidVolume5;
    
    // 五档卖盘
    private volatile double askPrice1, askPrice2, askPrice3, askPrice4, askPrice5;
    private volatile int askVolume1, askVolume2, askVolume3, askVolume4, askVolume5;
    
    // 时间信息
    private volatile String updateTime;         // 更新时间
    private volatile String tradingDay;         // 交易日
    private volatile LocalDateTime lastUpdateTime; // 最后更新时间戳
    
    // 统计信息
    private final AtomicLong updateCount = new AtomicLong(0);  // 更新次数
    
    public MarketDataSnapshot() {
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    public MarketDataSnapshot(String instrumentId, String exchangeId) {
        this();
        this.instrumentId = instrumentId;
        this.exchangeId = exchangeId;
    }
    
    /**
     * 更新行情数据
     */
    public synchronized void updateMarketData(
            String updateTime, double lastPrice, long volume, double turnover, double openInterest,
            double bidPrice1, int bidVolume1, double askPrice1, int askVolume1,
            double bidPrice2, int bidVolume2, double askPrice2, int askVolume2,
            double bidPrice3, int bidVolume3, double askPrice3, int askVolume3,
            double bidPrice4, int bidVolume4, double askPrice4, int askVolume4,
            double bidPrice5, int bidVolume5, double askPrice5, int askVolume5,
            double upperLimitPrice, double lowerLimitPrice,
            double preClosePrice, double openPrice, double highestPrice, double lowestPrice) {
        
        this.updateTime = updateTime;
        this.lastPrice = lastPrice;
        this.volume = volume;
        this.turnover = turnover;
        this.openInterest = openInterest;
        
        // 更新五档买盘
        this.bidPrice1 = bidPrice1;
        this.bidVolume1 = bidVolume1;
        this.bidPrice2 = bidPrice2;
        this.bidVolume2 = bidVolume2;
        this.bidPrice3 = bidPrice3;
        this.bidVolume3 = bidVolume3;
        this.bidPrice4 = bidPrice4;
        this.bidVolume4 = bidVolume4;
        this.bidPrice5 = bidPrice5;
        this.bidVolume5 = bidVolume5;
        
        // 更新五档卖盘
        this.askPrice1 = askPrice1;
        this.askVolume1 = askVolume1;
        this.askPrice2 = askPrice2;
        this.askVolume2 = askVolume2;
        this.askPrice3 = askPrice3;
        this.askVolume3 = askVolume3;
        this.askPrice4 = askPrice4;
        this.askVolume4 = askVolume4;
        this.askPrice5 = askPrice5;
        this.askVolume5 = askVolume5;
        
        // 更新价格信息
        this.upperLimitPrice = upperLimitPrice;
        this.lowerLimitPrice = lowerLimitPrice;
        this.preClosePrice = preClosePrice;
        this.openPrice = openPrice;
        this.highestPrice = highestPrice;
        this.lowestPrice = lowestPrice;
        
        // 更新时间戳和计数
        this.lastUpdateTime = LocalDateTime.now();
        this.updateCount.incrementAndGet();
    }
    
    /**
     * 计算涨跌幅
     */
    public double getPriceChangePercent() {
        if (preClosePrice <= 0) return 0.0;
        return ((lastPrice - preClosePrice) / preClosePrice) * 100;
    }
    
    /**
     * 计算涨跌额
     */
    public double getPriceChange() {
        return lastPrice - preClosePrice;
    }
    
    /**
     * 检查数据是否过期
     */
    public boolean isStale(int staleSeconds) {
        return lastUpdateTime.isBefore(LocalDateTime.now().minusSeconds(staleSeconds));
    }
    
    /**
     * 创建数据副本用于推送
     */
    public MarketDataSnapshot createSnapshot() {
        MarketDataSnapshot snapshot = new MarketDataSnapshot(this.instrumentId, this.exchangeId);
        
        // 复制所有字段
        snapshot.instrumentName = this.instrumentName;
        snapshot.lastPrice = this.lastPrice;
        snapshot.preClosePrice = this.preClosePrice;
        snapshot.openPrice = this.openPrice;
        snapshot.highestPrice = this.highestPrice;
        snapshot.lowestPrice = this.lowestPrice;
        snapshot.upperLimitPrice = this.upperLimitPrice;
        snapshot.lowerLimitPrice = this.lowerLimitPrice;
        snapshot.volume = this.volume;
        snapshot.turnover = this.turnover;
        snapshot.openInterest = this.openInterest;
        
        // 复制五档数据
        snapshot.bidPrice1 = this.bidPrice1;
        snapshot.bidVolume1 = this.bidVolume1;
        snapshot.bidPrice2 = this.bidPrice2;
        snapshot.bidVolume2 = this.bidVolume2;
        snapshot.bidPrice3 = this.bidPrice3;
        snapshot.bidVolume3 = this.bidVolume3;
        snapshot.bidPrice4 = this.bidPrice4;
        snapshot.bidVolume4 = this.bidVolume4;
        snapshot.bidPrice5 = this.bidPrice5;
        snapshot.bidVolume5 = this.bidVolume5;
        
        snapshot.askPrice1 = this.askPrice1;
        snapshot.askVolume1 = this.askVolume1;
        snapshot.askPrice2 = this.askPrice2;
        snapshot.askVolume2 = this.askVolume2;
        snapshot.askPrice3 = this.askPrice3;
        snapshot.askVolume3 = this.askVolume3;
        snapshot.askPrice4 = this.askPrice4;
        snapshot.askVolume4 = this.askVolume4;
        snapshot.askPrice5 = this.askPrice5;
        snapshot.askVolume5 = this.askVolume5;
        
        snapshot.updateTime = this.updateTime;
        snapshot.tradingDay = this.tradingDay;
        snapshot.lastUpdateTime = this.lastUpdateTime;
        
        return snapshot;
    }
    
    // Getters and Setters
    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }
    
    public String getExchangeId() { return exchangeId; }
    public void setExchangeId(String exchangeId) { this.exchangeId = exchangeId; }
    
    public String getInstrumentName() { return instrumentName; }
    public void setInstrumentName(String instrumentName) { this.instrumentName = instrumentName; }
    
    public double getLastPrice() { return lastPrice; }
    public double getPreClosePrice() { return preClosePrice; }
    public double getOpenPrice() { return openPrice; }
    public double getHighestPrice() { return highestPrice; }
    public double getLowestPrice() { return lowestPrice; }
    public double getUpperLimitPrice() { return upperLimitPrice; }
    public double getLowerLimitPrice() { return lowerLimitPrice; }
    
    public long getVolume() { return volume; }
    public double getTurnover() { return turnover; }
    public double getOpenInterest() { return openInterest; }
    
    public double getBidPrice1() { return bidPrice1; }
    public int getBidVolume1() { return bidVolume1; }
    public double getBidPrice2() { return bidPrice2; }
    public int getBidVolume2() { return bidVolume2; }
    public double getBidPrice3() { return bidPrice3; }
    public int getBidVolume3() { return bidVolume3; }
    public double getBidPrice4() { return bidPrice4; }
    public int getBidVolume4() { return bidVolume4; }
    public double getBidPrice5() { return bidPrice5; }
    public int getBidVolume5() { return bidVolume5; }
    
    public double getAskPrice1() { return askPrice1; }
    public int getAskVolume1() { return askVolume1; }
    public double getAskPrice2() { return askPrice2; }
    public int getAskVolume2() { return askVolume2; }
    public double getAskPrice3() { return askPrice3; }
    public int getAskVolume3() { return askVolume3; }
    public double getAskPrice4() { return askPrice4; }
    public int getAskVolume4() { return askVolume4; }
    public double getAskPrice5() { return askPrice5; }
    public int getAskVolume5() { return askVolume5; }
    
    public String getUpdateTime() { return updateTime; }
    public String getTradingDay() { return tradingDay; }
    public void setTradingDay(String tradingDay) { this.tradingDay = tradingDay; }
    
    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public long getUpdateCount() { return updateCount.get(); }
}
