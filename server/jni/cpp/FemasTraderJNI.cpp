#include "FemasTraderJNI.h"
#include "com_trading_jni_FemasTraderApi.h"
#include <iostream>
#include <cstring>
#include <thread>
#include <chrono>

// 全局实例管理
std::map<jlong, std::shared_ptr<FemasTraderJNI>> g_traderInstances;
int FemasTraderJNI::s_requestId = 1;

// FemasTraderJNI 实现
FemasTraderJNI::FemasTraderJNI()
    : m_pTraderApi(nullptr)
    , m_pTraderSpi(nullptr)
    , m_jvm(nullptr)
    , m_javaObject(nullptr)
    , m_javaClass(nullptr) {
}

FemasTraderJNI::~FemasTraderJNI() {
    cleanup();
}

bool FemasTraderJNI::initialize(JNIEnv* env, jobject javaObject) {
    // 获取JVM引用
    if (env->GetJavaVM(&m_jvm) != JNI_OK) {
        std::cerr << "Failed to get JavaVM" << std::endl;
        return false;
    }

    // 创建全局引用
    m_javaObject = env->NewGlobalRef(javaObject);
    if (!m_javaObject) {
        std::cerr << "Failed to create global reference" << std::endl;
        return false;
    }

    // 获取Java类
    jclass localClass = env->GetObjectClass(javaObject);
    m_javaClass = (jclass)env->NewGlobalRef(localClass);
    env->DeleteLocalRef(localClass);

    if (!m_javaClass) {
        std::cerr << "Failed to get Java class" << std::endl;
        return false;
    }

    // 缓存方法ID
    cacheMethodIDs(env);

    return true;
}

void FemasTraderJNI::cleanup() {
    if (m_pTraderApi) {
        m_pTraderApi->Release();
        m_pTraderApi = nullptr;
    }

    if (m_pTraderSpi) {
        delete m_pTraderSpi;
        m_pTraderSpi = nullptr;
    }

    if (m_jvm) {
        JNIEnv* env;
        if (m_jvm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_OK) {
            if (m_javaObject) {
                env->DeleteGlobalRef(m_javaObject);
                m_javaObject = nullptr;
            }
            if (m_javaClass) {
                env->DeleteGlobalRef(m_javaClass);
                m_javaClass = nullptr;
            }
        }
        m_jvm = nullptr;
    }
}

void FemasTraderJNI::cacheMethodIDs(JNIEnv* env) {
    m_onFrontConnectedMethod = env->GetMethodID(m_javaClass, "onFrontConnected", "()V");
    m_onFrontDisconnectedMethod = env->GetMethodID(m_javaClass, "onFrontDisconnected", "(I)V");
    m_onRspAuthenticateMethod = env->GetMethodID(m_javaClass, "onRspAuthenticate",
                                                "(Ljava/lang/String;ILjava/lang/String;)V");
    m_onRspUserLoginMethod = env->GetMethodID(m_javaClass, "onRspUserLogin",
                                             "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V");
    m_onRspOrderInsertMethod = env->GetMethodID(m_javaClass, "onRspOrderInsert",
                                               "(Ljava/lang/String;ILjava/lang/String;)V");
    m_onRtnOrderMethod = env->GetMethodID(m_javaClass, "onRtnOrder",
                                         "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;CCDIC)V");
    m_onRtnTradeMethod = env->GetMethodID(m_javaClass, "onRtnTrade",
                                         "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;CCDILjava/lang/String;)V");
}

bool FemasTraderJNI::createTraderApi(const std::string& flowPath) {
    try {
        // 创建交易API实例
        m_pTraderApi = CUstpFtdcTraderApi::CreateFtdcTraderApi(flowPath.c_str());
        if (!m_pTraderApi) {
            std::cerr << "Failed to create trader API" << std::endl;
            return false;
        }

        // 创建SPI实例
        m_pTraderSpi = new TraderSpiImpl(this);
        if (!m_pTraderSpi) {
            std::cerr << "Failed to create trader SPI" << std::endl;
            return false;
        }

        // 注册SPI
        m_pTraderApi->RegisterSpi(m_pTraderSpi);

        return true;
    } catch (const std::exception& e) {
        std::cerr << "Exception in createTraderApi: " << e.what() << std::endl;
        return false;
    }
}

void FemasTraderJNI::registerFront(const std::string& frontAddress) {
    if (m_pTraderApi) {
        char* address = new char[frontAddress.length() + 1];
        strcpy(address, frontAddress.c_str());
        m_pTraderApi->RegisterFront(address);
        delete[] address;
    }
}

void FemasTraderJNI::init() {
    if (m_pTraderApi) {
        m_pTraderApi->Init();
    }
}

void FemasTraderJNI::join() {
    if (m_pTraderApi) {
        m_pTraderApi->Join();
    }
}

void FemasTraderJNI::release() {
    cleanup();
}

int FemasTraderJNI::reqAuthenticate(const std::string& brokerId, const std::string& userId,
                                   const std::string& userProductInfo, const std::string& authCode) {
    if (!m_pTraderApi) return -1;

    CUstpFtdcReqAuthenticateField req = {0};
    strncpy(req.BrokerID, brokerId.c_str(), sizeof(req.BrokerID) - 1);
    strncpy(req.UserID, userId.c_str(), sizeof(req.UserID) - 1);
    strncpy(req.UserProductInfo, userProductInfo.c_str(), sizeof(req.UserProductInfo) - 1);
    strncpy(req.AuthCode, authCode.c_str(), sizeof(req.AuthCode) - 1);

    int requestId = getNextRequestId();
    int result = m_pTraderApi->ReqAuthenticate(&req, requestId);
    return result == 0 ? requestId : -1;
}

int FemasTraderJNI::reqUserLogin(const std::string& brokerId, const std::string& userId,
                                const std::string& password) {
    if (!m_pTraderApi) return -1;

    CUstpFtdcReqUserLoginField req = {0};
    strncpy(req.BrokerID, brokerId.c_str(), sizeof(req.BrokerID) - 1);
    strncpy(req.UserID, userId.c_str(), sizeof(req.UserID) - 1);
    strncpy(req.Password, password.c_str(), sizeof(req.Password) - 1);

    int requestId = getNextRequestId();
    int result = m_pTraderApi->ReqUserLogin(&req, requestId);
    return result == 0 ? requestId : -1;
}

int FemasTraderJNI::reqUserLogout(const std::string& brokerId, const std::string& userId) {
    if (!m_pTraderApi) return -1;

    CUstpFtdcReqUserLogoutField req = {0};
    strncpy(req.BrokerID, brokerId.c_str(), sizeof(req.BrokerID) - 1);
    strncpy(req.UserID, userId.c_str(), sizeof(req.UserID) - 1);

    int requestId = getNextRequestId();
    int result = m_pTraderApi->ReqUserLogout(&req, requestId);
    return result == 0 ? requestId : -1;
}

int FemasTraderJNI::reqOrderInsert(const std::string& instrumentId, char direction, char offsetFlag,
                                  double price, int volume, char orderPriceType,
                                  char timeCondition, char volumeCondition) {
    if (!m_pTraderApi) return -1;

    CUstpFtdcInputOrderField req = {0};
    strncpy(req.InstrumentID, instrumentId.c_str(), sizeof(req.InstrumentID) - 1);
    req.Direction = direction;
    req.OffsetFlag = offsetFlag;
    req.LimitPrice = price;
    req.Volume = volume;
    req.OrderPriceType = orderPriceType;
    req.TimeCondition = timeCondition;
    req.VolumeCondition = volumeCondition;

    // 设置其他必要字段
    req.ContingentCondition = USTP_FTDC_CC_Immediately;
    req.ForceCloseReason = USTP_FTDC_FCR_NotForceClose;
    req.IsAutoSuspend = 0;
    req.UserForceClose = 0;

    int requestId = getNextRequestId();
    int result = m_pTraderApi->ReqOrderInsert(&req, requestId);
    return result == 0 ? requestId : -1;
}

// 回调方法实现
void FemasTraderJNI::onFrontConnected() {
    if (!m_jvm || !m_javaObject) return;

    JNIEnv* env;
    if (m_jvm->AttachCurrentThread((void**)&env, nullptr) == JNI_OK) {
        env->CallVoidMethod(m_javaObject, m_onFrontConnectedMethod);
        m_jvm->DetachCurrentThread();
    }
}

void FemasTraderJNI::onFrontDisconnected(int reason) {
    if (!m_jvm || !m_javaObject) return;

    JNIEnv* env;
    if (m_jvm->AttachCurrentThread((void**)&env, nullptr) == JNI_OK) {
        env->CallVoidMethod(m_javaObject, m_onFrontDisconnectedMethod, reason);
        m_jvm->DetachCurrentThread();
    }
}

std::string FemasTraderJNI::jstringToString(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";

    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

jstring FemasTraderJNI::stringToJstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

int FemasTraderJNI::getNextRequestId() {
    return ++s_requestId;
}

// TraderSpiImpl 实现
void TraderSpiImpl::OnFrontConnected() {
    if (m_pJNI) {
        m_pJNI->onFrontConnected();
    }
}

void TraderSpiImpl::OnFrontDisconnected(int nReason) {
    if (m_pJNI) {
        m_pJNI->onFrontDisconnected(nReason);
    }
}

void TraderSpiImpl::OnRspAuthenticate(CUstpFtdcRspAuthenticateField* pRspAuthenticate,
                                     CUstpFtdcRspInfoField* pRspInfo, int nRequestID,
                                     bool bIsLast) {
    if (m_pJNI && bIsLast) {
        std::string authCode = pRspAuthenticate ? pRspAuthenticate->AuthCode : "";
        int errorId = pRspInfo ? pRspInfo->ErrorID : 0;
        std::string errorMsg = pRspInfo ? pRspInfo->ErrorMsg : "";
        m_pJNI->onRspAuthenticate(authCode, errorId, errorMsg);
    }
}

void TraderSpiImpl::OnRspUserLogin(CUstpFtdcRspUserLoginField* pRspUserLogin,
                                  CUstpFtdcRspInfoField* pRspInfo, int nRequestID,
                                  bool bIsLast) {
    if (m_pJNI && bIsLast) {
        std::string tradingDay = pRspUserLogin ? pRspUserLogin->TradingDay : "";
        std::string loginTime = pRspUserLogin ? pRspUserLogin->LoginTime : "";
        std::string brokerId = pRspUserLogin ? pRspUserLogin->BrokerID : "";
        std::string userId = pRspUserLogin ? pRspUserLogin->UserID : "";
        int errorId = pRspInfo ? pRspInfo->ErrorID : 0;
        std::string errorMsg = pRspInfo ? pRspInfo->ErrorMsg : "";
        m_pJNI->onRspUserLogin(tradingDay, loginTime, brokerId, userId, errorId, errorMsg);
    }
}

void TraderSpiImpl::OnRtnOrder(CUstpFtdcOrderField* pOrder) {
    if (m_pJNI && pOrder) {
        m_pJNI->onRtnOrder(
            pOrder->OrderSysID ? pOrder->OrderSysID : "",
            pOrder->OrderRef ? pOrder->OrderRef : "",
            pOrder->InstrumentID ? pOrder->InstrumentID : "",
            pOrder->Direction,
            pOrder->OffsetFlag,
            pOrder->LimitPrice,
            pOrder->Volume,
            pOrder->OrderStatus
        );
    }
}

void TraderSpiImpl::OnRtnTrade(CUstpFtdcTradeField* pTrade) {
    if (m_pJNI && pTrade) {
        m_pJNI->onRtnTrade(
            pTrade->TradeID ? pTrade->TradeID : "",
            pTrade->OrderRef ? pTrade->OrderRef : "",
            pTrade->InstrumentID ? pTrade->InstrumentID : "",
            pTrade->Direction,
            pTrade->OffsetFlag,
            pTrade->Price,
            pTrade->Volume,
            pTrade->TradeTime ? pTrade->TradeTime : ""
        );
    }
}

// JNI接口实现
extern "C" {

JNIEXPORT jboolean JNICALL Java_com_trading_jni_FemasTraderApi_createTraderApi
  (JNIEnv *env, jobject obj, jstring flowPath) {
    try {
        auto jni = std::make_shared<FemasTraderJNI>();
        if (!jni->initialize(env, obj)) {
            return JNI_FALSE;
        }

        std::string path = jni->jstringToString(env, flowPath);
        if (!jni->createTraderApi(path)) {
            return JNI_FALSE;
        }

        // 存储实例
        jlong ptr = reinterpret_cast<jlong>(jni.get());
        g_traderInstances[ptr] = jni;

        // 设置nativePtr字段
        jclass clazz = env->GetObjectClass(obj);
        jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
        env->SetLongField(obj, fieldId, ptr);

        return JNI_TRUE;
    } catch (...) {
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL Java_com_trading_jni_FemasTraderApi_registerFront
  (JNIEnv *env, jobject obj, jstring frontAddress) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldId);

    auto it = g_traderInstances.find(ptr);
    if (it != g_traderInstances.end()) {
        std::string address = it->second->jstringToString(env, frontAddress);
        it->second->registerFront(address);
    }
}

JNIEXPORT void JNICALL Java_com_trading_jni_FemasTraderApi_init
  (JNIEnv *env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldId);

    auto it = g_traderInstances.find(ptr);
    if (it != g_traderInstances.end()) {
        it->second->init();
    }
}

JNIEXPORT void JNICALL Java_com_trading_jni_FemasTraderApi_join
  (JNIEnv *env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldId);

    auto it = g_traderInstances.find(ptr);
    if (it != g_traderInstances.end()) {
        it->second->join();
    }
}

JNIEXPORT void JNICALL Java_com_trading_jni_FemasTraderApi_release
  (JNIEnv *env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldId);

    auto it = g_traderInstances.find(ptr);
    if (it != g_traderInstances.end()) {
        it->second->release();
        g_traderInstances.erase(it);
    }

    env->SetLongField(obj, fieldId, 0);
}

JNIEXPORT jint JNICALL Java_com_trading_jni_FemasTraderApi_reqAuthenticate
  (JNIEnv *env, jobject obj, jstring brokerId, jstring userId, jstring userProductInfo, jstring authCode) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldId);

    auto it = g_traderInstances.find(ptr);
    if (it != g_traderInstances.end()) {
        auto jni = it->second;
        return jni->reqAuthenticate(
            jni->jstringToString(env, brokerId),
            jni->jstringToString(env, userId),
            jni->jstringToString(env, userProductInfo),
            jni->jstringToString(env, authCode)
        );
    }
    return -1;
}

JNIEXPORT jint JNICALL Java_com_trading_jni_FemasTraderApi_reqUserLogin
  (JNIEnv *env, jobject obj, jstring brokerId, jstring userId, jstring password) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldId);

    auto it = g_traderInstances.find(ptr);
    if (it != g_traderInstances.end()) {
        auto jni = it->second;
        return jni->reqUserLogin(
            jni->jstringToString(env, brokerId),
            jni->jstringToString(env, userId),
            jni->jstringToString(env, password)
        );
    }
    return -1;
}

JNIEXPORT jint JNICALL Java_com_trading_jni_FemasTraderApi_reqOrderInsert
  (JNIEnv *env, jobject obj, jstring instrumentId, jchar direction, jchar offsetFlag,
   jdouble price, jint volume, jchar orderPriceType, jchar timeCondition, jchar volumeCondition) {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldId = env->GetFieldID(clazz, "nativePtr", "J");
    jlong ptr = env->GetLongField(obj, fieldId);

    auto it = g_traderInstances.find(ptr);
    if (it != g_traderInstances.end()) {
        auto jni = it->second;
        return jni->reqOrderInsert(
            jni->jstringToString(env, instrumentId),
            (char)direction,
            (char)offsetFlag,
            price,
            volume,
            (char)orderPriceType,
            (char)timeCondition,
            (char)volumeCondition
        );
    }
    return -1;
}

} // extern "C"
