package com.demo.ops.equipment.model;

/**
 * 设备维修三阶段互动流程请求模型。
 * 用于承载检索、执行、反馈三个阶段共用的上下文字段。
 */
public class EquipmentWorkflowRequest {

    /** 当前温度 */
    private Double currentTemperatureC;
    /** 振动值 */
    private Double vibrationMmPerS;
    /** 温升速率 */
    private Double temperatureRiseRatePerMin;
    /** 是否立即停机 */
    private Boolean immediateShutdown;
    /** 是否预留轴承 */
    private Boolean reserveBearing;
    /** 是否推送工单 */
    private Boolean pushWorkOrder;
    /** 用户意图 */
    private String userIntent;
    /** 用户在执行阶段选择的方案编码 */
    private String selectedOptionCode;

    public Double getCurrentTemperatureC() {
        return currentTemperatureC;
    }

    public void setCurrentTemperatureC(Double currentTemperatureC) {
        this.currentTemperatureC = currentTemperatureC;
    }

    public Double getVibrationMmPerS() {
        return vibrationMmPerS;
    }

    public void setVibrationMmPerS(Double vibrationMmPerS) {
        this.vibrationMmPerS = vibrationMmPerS;
    }

    public Double getTemperatureRiseRatePerMin() {
        return temperatureRiseRatePerMin;
    }

    public void setTemperatureRiseRatePerMin(Double temperatureRiseRatePerMin) {
        this.temperatureRiseRatePerMin = temperatureRiseRatePerMin;
    }

    public Boolean getImmediateShutdown() {
        return immediateShutdown;
    }

    public void setImmediateShutdown(Boolean immediateShutdown) {
        this.immediateShutdown = immediateShutdown;
    }

    public Boolean getReserveBearing() {
        return reserveBearing;
    }

    public void setReserveBearing(Boolean reserveBearing) {
        this.reserveBearing = reserveBearing;
    }

    public Boolean getPushWorkOrder() {
        return pushWorkOrder;
    }

    public void setPushWorkOrder(Boolean pushWorkOrder) {
        this.pushWorkOrder = pushWorkOrder;
    }

    public String getUserIntent() {
        return userIntent;
    }

    public void setUserIntent(String userIntent) {
        this.userIntent = userIntent;
    }

    public String getSelectedOptionCode() {
        return selectedOptionCode;
    }

    public void setSelectedOptionCode(String selectedOptionCode) {
        this.selectedOptionCode = selectedOptionCode;
    }
}
