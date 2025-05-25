package com.trading.jni;

/**
 * 飞马交易API的JNI接口封装 - Java 8兼容
 * 用于调用C++交易API
 */
public class FemasTraderApi {

    // 加载本地库
    static {
        try {
            // 根据实际的DLL文件名调整
            System.loadLibrary("USTPtraderapi");
            System.loadLibrary("FemasTraderJNI"); // 需要创建的JNI包装库
        } catch (UnsatisfiedLinkError e) {
            System.err.println("无法加载本地库: " + e.getMessage());
        }
    }

    // 交易API实例指针
    private long nativePtr;

    /**
     * 创建交易API实例
     * @param flowPath 流文件路径
     * @return 是否创建成功
     */
    public native boolean createTraderApi(String flowPath);

    /**
     * 注册前置机地址
     * @param frontAddress 前置机地址
     */
    public native void registerFront(String frontAddress);

    /**
     * 初始化API
     */
    public native void init();

    /**
     * 等待API线程结束
     */
    public native void join();

    /**
     * 释放API实例
     */
    public native void release();

    /**
     * 客户端认证请求
     * @param brokerId 经纪商代码
     * @param userId 用户代码
     * @param userProductInfo 用户产品信息
     * @param authCode 认证码
     * @return 请求ID
     */
    public native int reqAuthenticate(String brokerId, String userId, 
                                     String userProductInfo, String authCode);

    /**
     * 用户登录请求
     * @param brokerId 经纪商代码
     * @param userId 用户代码
     * @param password 密码
     * @return 请求ID
     */
    public native int reqUserLogin(String brokerId, String userId, String password);

    /**
     * 用户登出请求
     * @param brokerId 经纪商代码
     * @param userId 用户代码
     * @return 请求ID
     */
    public native int reqUserLogout(String brokerId, String userId);

    /**
     * 报单录入请求
     * @param instrumentId 合约代码
     * @param direction 买卖方向
     * @param offsetFlag 开平标志
     * @param price 价格
     * @param volume 数量
     * @param orderPriceType 报单价格条件
     * @param timeCondition 有效期类型
     * @param volumeCondition 成交量类型
     * @return 请求ID
     */
    public native int reqOrderInsert(String instrumentId, char direction, 
                                   char offsetFlag, double price, int volume,
                                   char orderPriceType, char timeCondition, 
                                   char volumeCondition);

    /**
     * 报单操作请求
     * @param orderRef 报单引用
     * @param frontId 前置编号
     * @param sessionId 会话编号
     * @param actionFlag 操作标志
     * @return 请求ID
     */
    public native int reqOrderAction(String orderRef, int frontId, 
                                   int sessionId, char actionFlag);

    /**
     * 查询投资者持仓
     * @param brokerId 经纪商代码
     * @param investorId 投资者代码
     * @param instrumentId 合约代码
     * @return 请求ID
     */
    public native int reqQryInvestorPosition(String brokerId, String investorId, 
                                           String instrumentId);

    /**
     * 查询资金账户
     * @param brokerId 经纪商代码
     * @param investorId 投资者代码
     * @return 请求ID
     */
    public native int reqQryTradingAccount(String brokerId, String investorId);

    /**
     * 查询合约
     * @param instrumentId 合约代码
     * @return 请求ID
     */
    public native int reqQryInstrument(String instrumentId);

    /**
     * 查询报单
     * @param brokerId 经纪商代码
     * @param investorId 投资者代码
     * @param instrumentId 合约代码
     * @return 请求ID
     */
    public native int reqQryOrder(String brokerId, String investorId, 
                                String instrumentId);

    /**
     * 查询成交
     * @param brokerId 经纪商代码
     * @param investorId 投资者代码
     * @param instrumentId 合约代码
     * @return 请求ID
     */
    public native int reqQryTrade(String brokerId, String investorId, 
                                String instrumentId);

    // 回调方法 - 由C++调用
    /**
     * 连接成功回调
     */
    public void onFrontConnected() {
        // 由子类实现
    }

    /**
     * 连接断开回调
     * @param reason 断开原因
     */
    public void onFrontDisconnected(int reason) {
        // 由子类实现
    }

    /**
     * 认证响应回调
     * @param authCode 认证码
     * @param errorId 错误代码
     * @param errorMsg 错误信息
     */
    public void onRspAuthenticate(String authCode, int errorId, String errorMsg) {
        // 由子类实现
    }

    /**
     * 登录响应回调
     * @param tradingDay 交易日
     * @param loginTime 登录时间
     * @param brokerId 经纪商代码
     * @param userId 用户代码
     * @param errorId 错误代码
     * @param errorMsg 错误信息
     */
    public void onRspUserLogin(String tradingDay, String loginTime, 
                              String brokerId, String userId, 
                              int errorId, String errorMsg) {
        // 由子类实现
    }

    /**
     * 报单录入响应回调
     * @param orderRef 报单引用
     * @param errorId 错误代码
     * @param errorMsg 错误信息
     */
    public void onRspOrderInsert(String orderRef, int errorId, String errorMsg) {
        // 由子类实现
    }

    /**
     * 报单通知回调
     * @param orderSysId 报单编号
     * @param orderRef 报单引用
     * @param instrumentId 合约代码
     * @param direction 买卖方向
     * @param offsetFlag 开平标志
     * @param price 价格
     * @param volume 数量
     * @param orderStatus 报单状态
     */
    public void onRtnOrder(String orderSysId, String orderRef, String instrumentId,
                          char direction, char offsetFlag, double price, 
                          int volume, char orderStatus) {
        // 由子类实现
    }

    /**
     * 成交通知回调
     * @param tradeId 成交编号
     * @param orderRef 报单引用
     * @param instrumentId 合约代码
     * @param direction 买卖方向
     * @param offsetFlag 开平标志
     * @param price 成交价格
     * @param volume 成交数量
     * @param tradeTime 成交时间
     */
    public void onRtnTrade(String tradeId, String orderRef, String instrumentId,
                          char direction, char offsetFlag, double price, 
                          int volume, String tradeTime) {
        // 由子类实现
    }
}
