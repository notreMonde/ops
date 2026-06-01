package com.demo.ops.equipment.service;

import com.demo.ops.common.service.JsonResourceService;
import com.demo.ops.equipment.model.EquipmentDiagnosisRequest;
import com.demo.ops.equipment.model.EquipmentDispositionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

/**
 * 设备检修决策服务。
 * 提供设备数字孪生、遥测数据、历史案例查询以及诊断与处置决策等核心业务逻辑，
 * 从 JSON 数据集中读取伪数据并注入请求参数后返回。
 */
@Service
public class EquipmentDecisionService {

    private final JsonResourceService jsonResourceService;
    private final ObjectMapper objectMapper;

    public EquipmentDecisionService(JsonResourceService jsonResourceService, ObjectMapper objectMapper) {
        this.jsonResourceService = jsonResourceService;
        this.objectMapper = objectMapper;
    }

    /** 查询设备数字孪生模型数据 */
    public JsonNode getDigitalTwin(String equipmentId) {
        ObjectNode node = jsonResourceService.readObject("datasets/equipment/digital-twin.json");
        node.put("equipmentId", equipmentId);
        return node;
    }

    /** 查询设备实时遥测数据（温度、振动等传感器读数） */
    public JsonNode getTelemetry(String equipmentId) {
        ObjectNode node = jsonResourceService.readObject("datasets/equipment/telemetry.json");
        node.put("equipmentId", equipmentId);
        return node;
    }

    /** 查询设备历史故障案例库 */
    public JsonNode getHistoricalCases(String equipmentId) {
        ObjectNode node = jsonResourceService.readObject("datasets/equipment/historical-cases.json");
        node.put("equipmentId", equipmentId);
        return node;
    }

    /** 查询设备关联的变更关系 */
    public JsonNode getChangeRelations(String equipmentId) {
        ObjectNode node = jsonResourceService.readObject("datasets/equipment/change-relations.json");
        node.put("equipmentId", equipmentId);
        return node;
    }

    /** 查询可调配的维修人员信息 */
    public JsonNode getPersonnelMatch(String equipmentId) {
        ObjectNode node = jsonResourceService.readObject("datasets/equipment/personnel-match.json");
        node.put("equipmentId", equipmentId);
        return node;
    }

    /** 查询可用库存备件信息 */
    public JsonNode getInventory(String equipmentId) {
        ObjectNode node = jsonResourceService.readObject("datasets/equipment/inventory.json");
        node.put("equipmentId", equipmentId);
        return node;
    }

    /**
     * 设备故障诊断。
     * 基于温度（默认 92°C）、振动（默认 4.2 mm/s）和温升速率（默认 2.3°C/min）进行判断：
     * 温度 >= 90°C 或振动 >= 4.0mm/s 判定为严重（P0），否则为一般（P1）。
     */
    public JsonNode getDiagnosisConclusion(String equipmentId, EquipmentDiagnosisRequest request) {
        EquipmentDiagnosisRequest actualRequest = request == null ? new EquipmentDiagnosisRequest() : request;
        double currentTemperature = actualRequest.getCurrentTemperatureC() == null ? 92.0D : actualRequest.getCurrentTemperatureC();
        double vibration = actualRequest.getVibrationMmPerS() == null ? 4.2D : actualRequest.getVibrationMmPerS();
        double riseRate = actualRequest.getTemperatureRiseRatePerMin() == null ? 2.3D : actualRequest.getTemperatureRiseRatePerMin();

        ObjectNode node = jsonResourceService.readObject("datasets/equipment/diagnosis-conclusion.json");
        ObjectNode telemetrySnapshot = node.with("telemetrySnapshot");
        telemetrySnapshot.put("currentTemperatureC", currentTemperature);
        telemetrySnapshot.put("vibrationMmPerS", vibration);
        telemetrySnapshot.put("temperatureRiseRatePerMin", riseRate);
        node.put("equipmentId", equipmentId);
        node.put("severity", currentTemperature >= 90.0D || vibration >= 4.0D ? "P0" : "P1");
        node.put("confidence", currentTemperature >= 90.0D ? "高" : "中");
        return node;
    }

    /**
     * 设备处置决策生成。
     * 基于用户请求确定是否立即停机、预留轴承以及推送维修工单。
     * 默认三个选项均为 true。
     */
    public JsonNode getDispositionDecision(String equipmentId, EquipmentDispositionRequest request) {
        EquipmentDispositionRequest actualRequest = request == null ? new EquipmentDispositionRequest() : request;

        ObjectNode node = jsonResourceService.readObject("datasets/equipment/disposition-decision.json");
        node.put("equipmentId", equipmentId);
        node.put("immediateShutdown", actualRequest.getImmediateShutdown() == null || actualRequest.getImmediateShutdown());
        node.put("reserveBearing", actualRequest.getReserveBearing() == null || actualRequest.getReserveBearing());
        node.put("pushWorkOrder", actualRequest.getPushWorkOrder() == null || actualRequest.getPushWorkOrder());
        return node;
    }
}
