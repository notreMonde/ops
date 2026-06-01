package com.demo.ops.equipment.model;

/**
 * 设备四阶段 workflow 通用请求。
 */
public class EquipmentWorkflowRequest {

    private Double currentTemperatureC;
    private Double vibrationMmPerS;
    private Double temperatureRiseRatePerMin;
    private Boolean immediateShutdown;
    private Boolean reserveBearing;
    private Boolean pushWorkOrder;
    private String userIntent;
    private Boolean basicInfoConfirmed;
    private Boolean followUpAcknowledged;
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

    public Boolean getBasicInfoConfirmed() {
        return basicInfoConfirmed;
    }

    public void setBasicInfoConfirmed(Boolean basicInfoConfirmed) {
        this.basicInfoConfirmed = basicInfoConfirmed;
    }

    public Boolean getFollowUpAcknowledged() {
        return followUpAcknowledged;
    }

    public void setFollowUpAcknowledged(Boolean followUpAcknowledged) {
        this.followUpAcknowledged = followUpAcknowledged;
    }

    public String getSelectedOptionCode() {
        return selectedOptionCode;
    }

    public void setSelectedOptionCode(String selectedOptionCode) {
        this.selectedOptionCode = selectedOptionCode;
    }
}
