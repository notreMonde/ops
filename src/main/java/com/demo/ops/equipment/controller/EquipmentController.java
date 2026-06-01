package com.demo.ops.equipment.controller;

import com.demo.ops.common.model.ApiResponse;
import com.demo.ops.equipment.model.EquipmentDiagnosisRequest;
import com.demo.ops.equipment.model.EquipmentDispositionRequest;
import com.demo.ops.equipment.service.EquipmentDecisionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备检修 REST API 控制器。
 * 提供设备数字孪生、遥测数据、诊断与处置决策相关的 RESTful 接口，路径前缀为 /api/v1/equipment。
 */
@RestController
@RequestMapping("/api/v1/equipment")
public class EquipmentController {

    private final EquipmentDecisionService equipmentDecisionService;

    public EquipmentController(EquipmentDecisionService equipmentDecisionService) {
        this.equipmentDecisionService = equipmentDecisionService;
    }

    /** 查询设备数字孪生数据 */
    @GetMapping("/{equipmentId}/digital-twin")
    public ApiResponse<JsonNode> getDigitalTwin(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getDigitalTwin(equipmentId));
    }

    /** 查询设备遥测数据（温度、振动等） */
    @GetMapping("/{equipmentId}/telemetry")
    public ApiResponse<JsonNode> getTelemetry(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getTelemetry(equipmentId));
    }

    /** 查询设备历史故障案例 */
    @GetMapping("/{equipmentId}/historical-cases")
    public ApiResponse<JsonNode> getHistoricalCases(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getHistoricalCases(equipmentId));
    }

    /** 查询设备变更关系 */
    @GetMapping("/{equipmentId}/change-relations")
    public ApiResponse<JsonNode> getChangeRelations(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getChangeRelations(equipmentId));
    }

    /** 查询可调配的维修人员 */
    @GetMapping("/{equipmentId}/personnel-match")
    public ApiResponse<JsonNode> getPersonnelMatch(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getPersonnelMatch(equipmentId));
    }

    /** 查询可用库存备件 */
    @GetMapping("/{equipmentId}/inventory")
    public ApiResponse<JsonNode> getInventory(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getInventory(equipmentId));
    }

    /** 提交诊断请求并获取诊断结论 */
    @PostMapping("/{equipmentId}/diagnosis-conclusion")
    public ApiResponse<JsonNode> getDiagnosisConclusion(@PathVariable String equipmentId,
                                                        @RequestBody(required = false) EquipmentDiagnosisRequest request) {
        return ApiResponse.success(equipmentDecisionService.getDiagnosisConclusion(equipmentId, request));
    }

    /** 提交处置请求并获取处置决策 */
    @PostMapping("/{equipmentId}/disposition-decision")
    public ApiResponse<JsonNode> getDispositionDecision(@PathVariable String equipmentId,
                                                        @RequestBody(required = false) EquipmentDispositionRequest request) {
        return ApiResponse.success(equipmentDecisionService.getDispositionDecision(equipmentId, request));
    }
}
