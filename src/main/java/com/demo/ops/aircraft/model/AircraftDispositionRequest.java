package com.demo.ops.aircraft.model;

/**
 * 飞机处置决策请求参数模型。
 * 用于接收前端传入的飞机故障处置决策请求参数，包括渗漏面积、持续滴落状态和维修目标。
 */
public class AircraftDispositionRequest {

    /** 渗漏面积（cm²） */
    private Double leakAreaCm2;
    /** 是否持续滴落 */
    private Boolean continuousDrip;
    /** 期望的维修目标 */
    private String repairTarget;

    public Double getLeakAreaCm2() {
        return leakAreaCm2;
    }

    public void setLeakAreaCm2(Double leakAreaCm2) {
        this.leakAreaCm2 = leakAreaCm2;
    }

    public Boolean getContinuousDrip() {
        return continuousDrip;
    }

    public void setContinuousDrip(Boolean continuousDrip) {
        this.continuousDrip = continuousDrip;
    }

    public String getRepairTarget() {
        return repairTarget;
    }

    public void setRepairTarget(String repairTarget) {
        this.repairTarget = repairTarget;
    }
}
