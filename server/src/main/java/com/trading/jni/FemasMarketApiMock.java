package com.trading.jni;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 飞马行情API的模拟实现 - 用于开发和测试
 * 不依赖JNI库，可以正常启动服务并模拟行情推送
 */
public class FemasMarketApiMock {

    private static final Logger logger = LoggerFactory.getLogger(FemasMarketApiMock.class);
    
    // 模拟连接状态
    private boolean isApiCreated = false;
    private boolean isInitialized = false;
    
    // 行情推送线程池
    private ScheduledExecutorService marketDataExecutor;
    private Random random = new Random();

    /**
     * 创建行情API实例
     * @param flowPath 流文件路径
     * @return 是否创建成功
     */
    public boolean createMarketApi(String flowPath) {
        logger.info("模拟创建行情API实例，流文件路径: {}", flowPath);
        isApiCreated = true;
        marketDataExecutor = Executors.newScheduledThreadPool(2);
        return true;
    }

    /**
     * 注册行情前置机地址
     * @param frontAddress 前置机地址
     */
    public void registerFront(String frontAddress) {
        logger.info("模拟注册行情前置机地址: {}", frontAddress);
    }

    /**
     * 初始化API
     */
    public void init() {
        logger.info("模拟初始化行情API");
        isInitialized = true;
        
        // 模拟连接成功
        new Thread(() -> {
            try {
                Thread.sleep(800); // 模拟连接延迟
                onFrontConnected();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 等待API线程结束
     */
    public void join() {
        logger.info("模拟等待行情API线程结束");
    }

    /**
     * 释放API实例
     */
    public void release() {
        logger.info("模拟释放行情API实例");
        isApiCreated = false;
        isInitialized = false;
        if (marketDataExecutor != null) {
            marketDataExecutor.shutdown();
        }
    }

    /**
     * 用户登录请求
     */
    public int reqUserLogin(String brokerId, String userId, String password) {
        logger.info("模拟行情登录请求 - 经纪商: {}, 用户: {}", brokerId, userId);
        
        // 模拟登录成功
        new Thread(() -> {
            try {
                Thread.sleep(300);
                onRspUserLogin("20240101", "09:00:00", brokerId, userId, 0, "行情登录成功");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        return 1;
    }

    /**
     * 用户登出请求
     */
    public int reqUserLogout(String brokerId, String userId) {
        logger.info("模拟行情登出请求 - 经纪商: {}, 用户: {}", brokerId, userId);
        
        // 模拟登出成功
        new Thread(() -> {
            try {
                Thread.sleep(200);
                onRspUserLogout(brokerId, userId, 0, "行情登出成功");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        return 2;
    }

    /**
     * 订阅行情
     */
    public int subscribeMarketData(String[] instrumentIds) {
        logger.info("模拟订阅行情 - 合约: {}", String.join(",", instrumentIds));
        
        // 模拟订阅成功响应
        for (String instrumentId : instrumentIds) {
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    onRspSubMarketData(instrumentId, 0, "订阅成功");
                    
                    // 开始推送模拟行情数据
                    startMarketDataPush(instrumentId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        return 3;
    }

    /**
     * 退订行情
     */
    public int unsubscribeMarketData(String[] instrumentIds) {
        logger.info("模拟退订行情 - 合约: {}", String.join(",", instrumentIds));
        
        for (String instrumentId : instrumentIds) {
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    onRspUnSubMarketData(instrumentId, 0, "退订成功");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        return 4;
    }

    /**
     * 查询合约
     */
    public int reqQryInstrument(String instrumentId) {
        logger.info("模拟查询合约 - 合约: {}", instrumentId);
        
        // 模拟查询响应
        new Thread(() -> {
            try {
                Thread.sleep(200);
                onRspQryInstrument(instrumentId, instrumentId + "合约", "SHFE", "rb", 
                                 1.0, 10, 0.08, true, 0, "查询成功");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        return 5;
    }

    /**
     * 开始推送模拟行情数据
     */
    private void startMarketDataPush(String instrumentId) {
        if (marketDataExecutor == null || marketDataExecutor.isShutdown()) {
            return;
        }
        
        // 每秒推送一次行情数据
        marketDataExecutor.scheduleAtFixedRate(() -> {
            try {
                // 生成模拟行情数据
                double basePrice = getBasePrice(instrumentId);
                double lastPrice = basePrice + (random.nextGaussian() * 10);
                
                onRtnDepthMarketData(
                    instrumentId,
                    "09:30:00",
                    lastPrice,
                    random.nextInt(10000) + 1000,
                    lastPrice * (random.nextInt(1000) + 100),
                    random.nextInt(50000) + 10000,
                    // 买盘
                    lastPrice - 1, random.nextInt(100) + 10,
                    lastPrice - 2, random.nextInt(100) + 10,
                    lastPrice - 3, random.nextInt(100) + 10,
                    lastPrice - 4, random.nextInt(100) + 10,
                    lastPrice - 5, random.nextInt(100) + 10,
                    // 卖盘
                    lastPrice + 1, random.nextInt(100) + 10,
                    lastPrice + 2, random.nextInt(100) + 10,
                    lastPrice + 3, random.nextInt(100) + 10,
                    lastPrice + 4, random.nextInt(100) + 10,
                    lastPrice + 5, random.nextInt(100) + 10,
                    // 其他价格
                    lastPrice + 350, lastPrice - 350,
                    lastPrice - 50, lastPrice + 20,
                    lastPrice + 30, lastPrice - 20
                );
            } catch (Exception e) {
                logger.error("推送模拟行情数据失败", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * 获取合约基础价格
     */
    private double getBasePrice(String instrumentId) {
        if (instrumentId.startsWith("rb")) {
            return 3500.0;
        } else if (instrumentId.startsWith("cu")) {
            return 70000.0;
        } else if (instrumentId.startsWith("au")) {
            return 450.0;
        } else {
            return 1000.0;
        }
    }

    // 回调方法 - 由子类实现
    public void onFrontConnected() {
        logger.info("模拟行情前置机连接成功回调");
    }

    public void onFrontDisconnected(int reason) {
        logger.info("模拟行情前置机连接断开回调，原因: {}", reason);
    }

    public void onRspUserLogin(String tradingDay, String loginTime, 
                              String brokerId, String userId, 
                              int errorId, String errorMsg) {
        logger.info("模拟行情登录响应回调 - 交易日: {}, 错误码: {}", tradingDay, errorId);
    }

    public void onRspUserLogout(String brokerId, String userId, 
                               int errorId, String errorMsg) {
        logger.info("模拟行情登出响应回调 - 用户: {}, 错误码: {}", userId, errorId);
    }

    public void onRspSubMarketData(String instrumentId, int errorId, String errorMsg) {
        logger.info("模拟订阅行情响应回调 - 合约: {}, 错误码: {}", instrumentId, errorId);
    }

    public void onRspUnSubMarketData(String instrumentId, int errorId, String errorMsg) {
        logger.info("模拟退订行情响应回调 - 合约: {}, 错误码: {}", instrumentId, errorId);
    }

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
        // 由子类实现
    }

    public void onRspQryInstrument(String instrumentId, String instrumentName,
                                 String exchangeId, String productId,
                                 double priceTick, int volumeMultiple,
                                 double minMarginRatio, boolean isLast,
                                 int errorId, String errorMsg) {
        logger.info("模拟合约查询响应回调 - 合约: {}, 名称: {}", instrumentId, instrumentName);
    }
}
