#ifndef FEMAS_TRADER_JNI_H
#define FEMAS_TRADER_JNI_H

#include <jni.h>
#include <memory>
#include <string>
#include <map>
#include "USTPFtdcTraderApi.h"

/**
 * 飞马交易API的JNI包装类
 * 实现Java与C++交易API的桥接
 */
class FemasTraderJNI {
private:
    CUstpFtdcTraderApi* m_pTraderApi;
    CUstpFtdcTraderSpi* m_pTraderSpi;
    JavaVM* m_jvm;
    jobject m_javaObject;
    jclass m_javaClass;
    
    // 回调方法ID缓存
    jmethodID m_onFrontConnectedMethod;
    jmethodID m_onFrontDisconnectedMethod;
    jmethodID m_onRspAuthenticateMethod;
    jmethodID m_onRspUserLoginMethod;
    jmethodID m_onRspOrderInsertMethod;
    jmethodID m_onRtnOrderMethod;
    jmethodID m_onRtnTradeMethod;
    
    // 请求ID生成器
    static int s_requestId;
    
public:
    FemasTraderJNI();
    ~FemasTraderJNI();
    
    // 初始化和清理
    bool initialize(JNIEnv* env, jobject javaObject);
    void cleanup();
    
    // API操作方法
    bool createTraderApi(const std::string& flowPath);
    void registerFront(const std::string& frontAddress);
    void init();
    void join();
    void release();
    
    // 交易请求方法
    int reqAuthenticate(const std::string& brokerId, const std::string& userId,
                       const std::string& userProductInfo, const std::string& authCode);
    int reqUserLogin(const std::string& brokerId, const std::string& userId, 
                    const std::string& password);
    int reqUserLogout(const std::string& brokerId, const std::string& userId);
    int reqOrderInsert(const std::string& instrumentId, char direction, char offsetFlag,
                      double price, int volume, char orderPriceType, 
                      char timeCondition, char volumeCondition);
    int reqOrderAction(const std::string& orderRef, int frontId, int sessionId, 
                      char actionFlag);
    int reqQryInvestorPosition(const std::string& brokerId, const std::string& investorId,
                              const std::string& instrumentId);
    int reqQryTradingAccount(const std::string& brokerId, const std::string& investorId);
    int reqQryInstrument(const std::string& instrumentId);
    int reqQryOrder(const std::string& brokerId, const std::string& investorId,
                   const std::string& instrumentId);
    int reqQryTrade(const std::string& brokerId, const std::string& investorId,
                   const std::string& instrumentId);
    
    // 回调方法
    void onFrontConnected();
    void onFrontDisconnected(int reason);
    void onRspAuthenticate(const std::string& authCode, int errorId, const std::string& errorMsg);
    void onRspUserLogin(const std::string& tradingDay, const std::string& loginTime,
                       const std::string& brokerId, const std::string& userId,
                       int errorId, const std::string& errorMsg);
    void onRspOrderInsert(const std::string& orderRef, int errorId, const std::string& errorMsg);
    void onRtnOrder(const std::string& orderSysId, const std::string& orderRef,
                   const std::string& instrumentId, char direction, char offsetFlag,
                   double price, int volume, char orderStatus);
    void onRtnTrade(const std::string& tradeId, const std::string& orderRef,
                   const std::string& instrumentId, char direction, char offsetFlag,
                   double price, int volume, const std::string& tradeTime);
    
private:
    // 辅助方法
    void cacheMethodIDs(JNIEnv* env);
    void callJavaMethod(const char* methodName, const char* signature, ...);
    std::string jstringToString(JNIEnv* env, jstring jstr);
    jstring stringToJstring(JNIEnv* env, const std::string& str);
    static int getNextRequestId();
};

/**
 * 交易SPI实现类
 */
class TraderSpiImpl : public CUstpFtdcTraderSpi {
private:
    FemasTraderJNI* m_pJNI;
    
public:
    TraderSpiImpl(FemasTraderJNI* jni) : m_pJNI(jni) {}
    
    // 重写回调方法
    virtual void OnFrontConnected() override;
    virtual void OnFrontDisconnected(int nReason) override;
    virtual void OnRspAuthenticate(CUstpFtdcRspAuthenticateField* pRspAuthenticate,
                                  CUstpFtdcRspInfoField* pRspInfo, int nRequestID, 
                                  bool bIsLast) override;
    virtual void OnRspUserLogin(CUstpFtdcRspUserLoginField* pRspUserLogin,
                               CUstpFtdcRspInfoField* pRspInfo, int nRequestID, 
                               bool bIsLast) override;
    virtual void OnRspOrderInsert(CUstpFtdcInputOrderField* pInputOrder,
                                 CUstpFtdcRspInfoField* pRspInfo, int nRequestID, 
                                 bool bIsLast) override;
    virtual void OnRtnOrder(CUstpFtdcOrderField* pOrder) override;
    virtual void OnRtnTrade(CUstpFtdcTradeField* pTrade) override;
    virtual void OnRspQryInvestorPosition(CUstpFtdcRspInvestorPositionField* pRspInvestorPosition,
                                         CUstpFtdcRspInfoField* pRspInfo, int nRequestID, 
                                         bool bIsLast) override;
    virtual void OnRspQryTradingAccount(CUstpFtdcRspTradingAccountField* pRspTradingAccount,
                                       CUstpFtdcRspInfoField* pRspInfo, int nRequestID, 
                                       bool bIsLast) override;
    virtual void OnRspQryInstrument(CUstpFtdcRspInstrumentField* pRspInstrument,
                                   CUstpFtdcRspInfoField* pRspInfo, int nRequestID, 
                                   bool bIsLast) override;
    virtual void OnRspQryOrder(CUstpFtdcOrderField* pOrder,
                              CUstpFtdcRspInfoField* pRspInfo, int nRequestID, 
                              bool bIsLast) override;
    virtual void OnRspQryTrade(CUstpFtdcTradeField* pTrade,
                              CUstpFtdcRspInfoField* pRspInfo, int nRequestID, 
                              bool bIsLast) override;
};

// 全局实例管理
extern std::map<jlong, std::shared_ptr<FemasTraderJNI>> g_traderInstances;

#endif // FEMAS_TRADER_JNI_H
