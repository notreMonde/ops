package com.demo.ops.aircraft.model;

/**
 * 飞机四阶段 workflow 通用请求。
 */
public class AircraftWorkflowRequest {

    private String component;
    private Double leakAreaCm2;
    private Boolean continuousDrip;
    private String repairTarget;
    private String userIntent;
    private Boolean basicInfoConfirmed;
    private Boolean followUpAcknowledged;
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
