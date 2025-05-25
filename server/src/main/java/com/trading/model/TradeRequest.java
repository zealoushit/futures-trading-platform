package com.trading.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 交易请求模型
 */
public class TradeRequest {

    @NotBlank(message = "合约代码不能为空")
    @JsonProperty("instrumentId")
    private String instrumentId;

    @NotNull(message = "买卖方向不能为空")
    @JsonProperty("direction")
    private Character direction; // '0'-买, '1'-卖

    @NotNull(message = "开平标志不能为空")
    @JsonProperty("offsetFlag")
    private Character offsetFlag; // '0'-开仓, '1'-平仓, '3'-平今

    @NotNull(message = "价格不能为空")
    @JsonProperty("price")
    private Double price;

    @NotNull(message = "数量不能为空")
    @JsonProperty("volume")
    private Integer volume;

    @JsonProperty("orderPriceType")
    private Character orderPriceType = '2'; // 限价单

    @JsonProperty("timeCondition")
    private Character timeCondition = '3'; // 当日有效

    @JsonProperty("volumeCondition")
    private Character volumeCondition = '1'; // 任何数量

    @JsonProperty("contingentCondition")
    private Character contingentCondition = '1'; // 立即

    @JsonProperty("forceCloseReason")
    private Character forceCloseReason = '0'; // 非强平

    @JsonProperty("isAutoSuspend")
    private Integer isAutoSuspend = 0; // 不自动挂起

    @JsonProperty("userForceClose")
    private Integer userForceClose = 0; // 非用户强平

    // Constructors
    public TradeRequest() {}

    // Getters and Setters
    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Character getDirection() {
        return direction;
    }

    public void setDirection(Character direction) {
        this.direction = direction;
    }

    public Character getOffsetFlag() {
        return offsetFlag;
    }

    public void setOffsetFlag(Character offsetFlag) {
        this.offsetFlag = offsetFlag;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Character getOrderPriceType() {
        return orderPriceType;
    }

    public void setOrderPriceType(Character orderPriceType) {
        this.orderPriceType = orderPriceType;
    }

    public Character getTimeCondition() {
        return timeCondition;
    }

    public void setTimeCondition(Character timeCondition) {
        this.timeCondition = timeCondition;
    }

    public Character getVolumeCondition() {
        return volumeCondition;
    }

    public void setVolumeCondition(Character volumeCondition) {
        this.volumeCondition = volumeCondition;
    }

    public Character getContingentCondition() {
        return contingentCondition;
    }

    public void setContingentCondition(Character contingentCondition) {
        this.contingentCondition = contingentCondition;
    }

    public Character getForceCloseReason() {
        return forceCloseReason;
    }

    public void setForceCloseReason(Character forceCloseReason) {
        this.forceCloseReason = forceCloseReason;
    }

    public Integer getIsAutoSuspend() {
        return isAutoSuspend;
    }

    public void setIsAutoSuspend(Integer isAutoSuspend) {
        this.isAutoSuspend = isAutoSuspend;
    }

    public Integer getUserForceClose() {
        return userForceClose;
    }

    public void setUserForceClose(Integer userForceClose) {
        this.userForceClose = userForceClose;
    }

    @Override
    public String toString() {
        return "TradeRequest{" +
                "instrumentId='" + instrumentId + '\'' +
                ", direction=" + direction +
                ", offsetFlag=" + offsetFlag +
                ", price=" + price +
                ", volume=" + volume +
                '}';
    }
}
