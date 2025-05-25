package com.trading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 交易配置类
 * 管理C++交易API的连接参数
 */
@Configuration
@ConfigurationProperties(prefix = "trading")
@EnableConfigurationProperties
public class TradingConfig {

    /**
     * 交易前置机地址
     */
    private String frontAddress;

    /**
     * 行情前置机地址
     */
    private String mdAddress;

    /**
     * 经纪商代码
     */
    private String brokerId;

    /**
     * 用户代码
     */
    private String userId;

    /**
     * 密码
     */
    private String password;

    /**
     * 投资者代码
     */
    private String investorId;

    /**
     * 应用标识
     */
    private String appId;

    /**
     * 认证码
     */
    private String authCode;

    /**
     * 产品信息
     */
    private String userProductInfo;

    /**
     * 流文件路径
     */
    private String flowPath;

    // Getters and Setters
    public String getFrontAddress() {
        return frontAddress;
    }

    public void setFrontAddress(String frontAddress) {
        this.frontAddress = frontAddress;
    }

    public String getMdAddress() {
        return mdAddress;
    }

    public void setMdAddress(String mdAddress) {
        this.mdAddress = mdAddress;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getInvestorId() {
        return investorId;
    }

    public void setInvestorId(String investorId) {
        this.investorId = investorId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getUserProductInfo() {
        return userProductInfo;
    }

    public void setUserProductInfo(String userProductInfo) {
        this.userProductInfo = userProductInfo;
    }

    public String getFlowPath() {
        return flowPath;
    }

    public void setFlowPath(String flowPath) {
        this.flowPath = flowPath;
    }
}
