package com.demo.ops.equipment.model;

/**
 * 设备处置决策请求参数模型。
 * 用于接收前端传入的设备处置决策请求，包括是否立即停机、是否预留轴承和是否推送工单。
 */
public class EquipmentDispositionRequest {

    /** 是否立即停机 */
    private Boolean immediateShutdown;
    /** 是否预留备件轴承 */
    private Boolean reserveBearing;
    /** 是否推送维修工单 */
    private Boolean pushWorkOrder;

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
}
