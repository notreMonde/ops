package com.demo.ops.equipment.model;

/**
 * 设备诊断请求参数模型。
 * 用于接收前端传入的设备故障诊断请求，包括当前温度、振动值和温度上升速率。
 */
public class EquipmentDiagnosisRequest {

    /** 当前温度（摄氏度） */
    private Double currentTemperatureC;
    /** 振动值（mm/s） */
    private Double vibrationMmPerS;
    /** 温度上升速率（°C/min） */
    private Double temperatureRiseRatePerMin;

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
}
