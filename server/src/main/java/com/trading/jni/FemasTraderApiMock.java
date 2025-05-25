package com.trading.jni;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 飞马交易API的模拟实现 - 用于开发和测试
 * 不依赖JNI库，可以正常启动服务
 */
public class FemasTraderApiMock {

    private static final Logger logger = LoggerFactory.getLogger(FemasTraderApiMock.class);

    // 模拟连接状态
    private boolean isApiCreated = false;
    private boolean isInitialized = false;

    /**
     * 创建交易API实例
     * @param flowPath 流文件路径
     * @return 是否创建成功
     */
    public boolean createTraderApi(String flowPath) {
        logger.info("模拟创建交易API实例，流文件路径: {}", flowPath);
        isApiCreated = true;
        return true;
    }

    /**
     * 注册前置机地址
     * @param frontAddress 前置机地址
     */
    public void registerFront(String frontAddress) {
        logger.info("模拟注册前置机地址: {}", frontAddress);
    }

    /**
     * 初始化API
     */
    public void init() {
        logger.info("模拟初始化交易API");
        isInitialized = true;
        
        // 模拟连接成功
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 模拟连接延迟
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
        logger.info("模拟等待API线程结束");
    }

    /**
     * 释放API实例
     */
    public void release() {
        logger.info("模拟释放API实例");
        isApiCreated = false;
        isInitialized = false;
    }

    /**
     * 客户端认证请求
     */
    public int reqAuthenticate(String brokerId, String userId, 
                              String userProductInfo, String authCode) {
        logger.info("模拟认证请求 - 经纪商: {}, 用户: {}", brokerId, userId);
        
        // 模拟认证成功
        new Thread(() -> {
            try {
                Thread.sleep(500);
                onRspAuthenticate(authCode, 0, "认证成功");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        return 1;
    }

    /**
     * 用户登录请求
     */
    public int reqUserLogin(String brokerId, String userId, String password) {
        logger.info("模拟登录请求 - 经纪商: {}, 用户: {}", brokerId, userId);
        
        // 模拟登录成功
        new Thread(() -> {
            try {
                Thread.sleep(500);
                onRspUserLogin("20240101", "09:00:00", brokerId, userId, 0, "登录成功");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        return 2;
    }

    /**
     * 用户登出请求
     */
    public int reqUserLogout(String brokerId, String userId) {
        logger.info("模拟登出请求 - 经纪商: {}, 用户: {}", brokerId, userId);
        return 3;
    }

    /**
     * 报单录入请求
     */
    public int reqOrderInsert(String instrumentId, char direction, 
                             char offsetFlag, double price, int volume,
                             char orderPriceType, char timeCondition, 
                             char volumeCondition) {
        logger.info("模拟下单请求 - 合约: {}, 方向: {}, 价格: {}, 数量: {}", 
                   instrumentId, direction, price, volume);
        
        // 模拟报单回报
        new Thread(() -> {
            try {
                Thread.sleep(200);
                String orderSysId = "SIM" + System.currentTimeMillis();
                String orderRef = String.valueOf(System.currentTimeMillis() % 1000000);
                onRtnOrder(orderSysId, orderRef, instrumentId, direction, offsetFlag, 
                          price, volume, '0'); // 0-全部成交
                
                // 模拟成交回报
                Thread.sleep(100);
                String tradeId = "T" + System.currentTimeMillis();
                onRtnTrade(tradeId, orderRef, instrumentId, direction, offsetFlag, 
                          price, volume, "09:30:00");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        return 4;
    }

    /**
     * 报单操作请求
     */
    public int reqOrderAction(String orderRef, int frontId, 
                             int sessionId, char actionFlag) {
        logger.info("模拟撤单请求 - 报单引用: {}", orderRef);
        return 5;
    }

    /**
     * 查询投资者持仓
     */
    public int reqQryInvestorPosition(String brokerId, String investorId, 
                                     String instrumentId) {
        logger.info("模拟查询持仓 - 投资者: {}, 合约: {}", investorId, instrumentId);
        return 6;
    }

    /**
     * 查询资金账户
     */
    public int reqQryTradingAccount(String brokerId, String investorId) {
        logger.info("模拟查询资金账户 - 投资者: {}", investorId);
        return 7;
    }

    /**
     * 查询合约
     */
    public int reqQryInstrument(String instrumentId) {
        logger.info("模拟查询合约 - 合约: {}", instrumentId);
        return 8;
    }

    /**
     * 查询报单
     */
    public int reqQryOrder(String brokerId, String investorId, 
                          String instrumentId) {
        logger.info("模拟查询报单 - 投资者: {}, 合约: {}", investorId, instrumentId);
        return 9;
    }

    /**
     * 查询成交
     */
    public int reqQryTrade(String brokerId, String investorId, 
                          String instrumentId) {
        logger.info("模拟查询成交 - 投资者: {}, 合约: {}", investorId, instrumentId);
        return 10;
    }

    // 回调方法 - 由子类实现
    public void onFrontConnected() {
        logger.info("模拟前置机连接成功回调");
    }

    public void onFrontDisconnected(int reason) {
        logger.info("模拟前置机连接断开回调，原因: {}", reason);
    }

    public void onRspAuthenticate(String authCode, int errorId, String errorMsg) {
        logger.info("模拟认证响应回调 - 错误码: {}, 消息: {}", errorId, errorMsg);
    }

    public void onRspUserLogin(String tradingDay, String loginTime, 
                              String brokerId, String userId, 
                              int errorId, String errorMsg) {
        logger.info("模拟登录响应回调 - 交易日: {}, 错误码: {}", tradingDay, errorId);
    }

    public void onRspOrderInsert(String orderRef, int errorId, String errorMsg) {
        logger.info("模拟报单录入响应回调 - 报单引用: {}, 错误码: {}", orderRef, errorId);
    }

    public void onRtnOrder(String orderSysId, String orderRef, String instrumentId,
                          char direction, char offsetFlag, double price, 
                          int volume, char orderStatus) {
        logger.info("模拟报单通知回调 - 报单号: {}, 合约: {}, 状态: {}", orderSysId, instrumentId, orderStatus);
    }

    public void onRtnTrade(String tradeId, String orderRef, String instrumentId,
                          char direction, char offsetFlag, double price, 
                          int volume, String tradeTime) {
        logger.info("模拟成交通知回调 - 成交号: {}, 合约: {}, 价格: {}, 数量: {}", 
                   tradeId, instrumentId, price, volume);
    }
}
