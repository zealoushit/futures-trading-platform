package com.trading.jni;

/**
 * 飞马行情API的JNI接口封装 - Java 8兼容
 * 用于调用C++行情API
 */
public class FemasMarketApi {

    // 加载本地库
    static {
        try {
            // 根据实际的DLL文件名调整
            System.loadLibrary("USTPmdapi");
            System.loadLibrary("FemasMarketJNI"); // 需要创建的JNI包装库
        } catch (UnsatisfiedLinkError e) {
            System.err.println("无法加载行情本地库: " + e.getMessage());
        }
    }

    // 行情API实例指针
    private long nativePtr;

    /**
     * 创建行情API实例
     * @param flowPath 流文件路径
     * @return 是否创建成功
     */
    public native boolean createMarketApi(String flowPath);

    /**
     * 注册行情前置机地址
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
     * 订阅行情
     * @param instrumentIds 合约代码数组
     * @return 请求ID
     */
    public native int subscribeMarketData(String[] instrumentIds);

    /**
     * 退订行情
     * @param instrumentIds 合约代码数组
     * @return 请求ID
     */
    public native int unsubscribeMarketData(String[] instrumentIds);

    /**
     * 查询合约
     * @param instrumentId 合约代码
     * @return 请求ID
     */
    public native int reqQryInstrument(String instrumentId);

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
     * 登出响应回调
     * @param brokerId 经纪商代码
     * @param userId 用户代码
     * @param errorId 错误代码
     * @param errorMsg 错误信息
     */
    public void onRspUserLogout(String brokerId, String userId, 
                               int errorId, String errorMsg) {
        // 由子类实现
    }

    /**
     * 订阅行情响应回调
     * @param instrumentId 合约代码
     * @param errorId 错误代码
     * @param errorMsg 错误信息
     */
    public void onRspSubMarketData(String instrumentId, int errorId, String errorMsg) {
        // 由子类实现
    }

    /**
     * 退订行情响应回调
     * @param instrumentId 合约代码
     * @param errorId 错误代码
     * @param errorMsg 错误信息
     */
    public void onRspUnSubMarketData(String instrumentId, int errorId, String errorMsg) {
        // 由子类实现
    }

    /**
     * 深度行情通知回调
     * @param instrumentId 合约代码
     * @param updateTime 更新时间
     * @param lastPrice 最新价
     * @param volume 成交量
     * @param turnover 成交金额
     * @param openInterest 持仓量
     * @param bidPrice1 买一价
     * @param bidVolume1 买一量
     * @param askPrice1 卖一价
     * @param askVolume1 卖一量
     * @param bidPrice2 买二价
     * @param bidVolume2 买二量
     * @param askPrice2 卖二价
     * @param askVolume2 卖二量
     * @param bidPrice3 买三价
     * @param bidVolume3 买三量
     * @param askPrice3 卖三价
     * @param askVolume3 卖三量
     * @param bidPrice4 买四价
     * @param bidVolume4 买四量
     * @param askPrice4 卖四价
     * @param askVolume4 卖四量
     * @param bidPrice5 买五价
     * @param bidVolume5 买五量
     * @param askPrice5 卖五价
     * @param askVolume5 卖五量
     * @param upperLimitPrice 涨停价
     * @param lowerLimitPrice 跌停价
     * @param preClosePrice 昨收价
     * @param openPrice 开盘价
     * @param highestPrice 最高价
     * @param lowestPrice 最低价
     */
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

    /**
     * 合约查询响应回调
     * @param instrumentId 合约代码
     * @param instrumentName 合约名称
     * @param exchangeId 交易所代码
     * @param productId 产品代码
     * @param priceTick 最小变动价位
     * @param volumeMultiple 合约数量乘数
     * @param minMarginRatio 最小保证金率
     * @param isLast 是否最后一条
     * @param errorId 错误代码
     * @param errorMsg 错误信息
     */
    public void onRspQryInstrument(String instrumentId, String instrumentName,
                                 String exchangeId, String productId,
                                 double priceTick, int volumeMultiple,
                                 double minMarginRatio, boolean isLast,
                                 int errorId, String errorMsg) {
        // 由子类实现
    }
}
