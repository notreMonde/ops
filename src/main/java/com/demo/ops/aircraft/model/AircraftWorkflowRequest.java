package com.demo.ops.aircraft.model;

/**
 * 飞机维修三阶段互动流程请求模型。
 * 用于承载检索、执行、反馈三个阶段共用的上下文字段。
 */
public class AircraftWorkflowRequest {

    /** 故障部件 */
    private String component;
    /** 渗漏面积（cm²） */
    private Double leakAreaCm2;
    /** 是否持续滴落 */
    private Boolean continuousDrip;
    /** 维修目标 */
    private String repairTarget;
    /** 用户意图 */
    private String userIntent;
    /** 用户在执行阶段选择的方案编码 */
    private String selectedOptionCode;

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

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
