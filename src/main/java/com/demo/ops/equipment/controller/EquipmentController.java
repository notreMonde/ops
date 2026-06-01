package com.demo.ops.equipment.controller;

import com.demo.ops.common.model.ApiResponse;
import com.demo.ops.equipment.model.EquipmentDiagnosisRequest;
import com.demo.ops.equipment.model.EquipmentDispositionRequest;
import com.demo.ops.equipment.model.EquipmentWorkflowRequest;
import com.demo.ops.equipment.service.EquipmentDecisionService;
import com.demo.ops.equipment.service.EquipmentInteractionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/equipment")
public class EquipmentController {

    private final EquipmentDecisionService equipmentDecisionService;
    private final EquipmentInteractionService equipmentInteractionService;

    public EquipmentController(EquipmentDecisionService equipmentDecisionService,
                               EquipmentInteractionService equipmentInteractionService) {
        this.equipmentDecisionService = equipmentDecisionService;
        this.equipmentInteractionService = equipmentInteractionService;
    }

    @GetMapping("/{equipmentId}/digital-twin")
    public ApiResponse<JsonNode> getDigitalTwin(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getDigitalTwin(equipmentId));
    }

    @GetMapping("/{equipmentId}/telemetry")
    public ApiResponse<JsonNode> getTelemetry(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getTelemetry(equipmentId));
    }

    @GetMapping("/{equipmentId}/historical-cases")
    public ApiResponse<JsonNode> getHistoricalCases(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getHistoricalCases(equipmentId));
    }

    @GetMapping("/{equipmentId}/change-relations")
    public ApiResponse<JsonNode> getChangeRelations(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getChangeRelations(equipmentId));
    }

    @GetMapping("/{equipmentId}/personnel-match")
    public ApiResponse<JsonNode> getPersonnelMatch(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getPersonnelMatch(equipmentId));
    }

    @GetMapping("/{equipmentId}/inventory")
    public ApiResponse<JsonNode> getInventory(@PathVariable String equipmentId) {
        return ApiResponse.success(equipmentDecisionService.getInventory(equipmentId));
    }

    @PostMapping("/{equipmentId}/diagnosis-conclusion")
    public ApiResponse<JsonNode> getDiagnosisConclusion(@PathVariable String equipmentId,
                                                        @RequestBody(required = false) EquipmentDiagnosisRequest request) {
        return ApiResponse.success(equipmentDecisionService.getDiagnosisConclusion(equipmentId, request));
    }

    @PostMapping("/{equipmentId}/disposition-decision")
    public ApiResponse<JsonNode> getDispositionDecision(@PathVariable String equipmentId,
                                                        @RequestBody(required = false) EquipmentDispositionRequest request) {
        return ApiResponse.success(equipmentDecisionService.getDispositionDecision(equipmentId, request));
    }

    @PostMapping("/{equipmentId}/workflow/clarify")
    public ApiResponse<JsonNode> getWorkflowClarify(@PathVariable String equipmentId,
                                                    @RequestBody(required = false) EquipmentWorkflowRequest request) {
        return ApiResponse.success(equipmentInteractionService.getClarify(equipmentId, request));
    }

    @PostMapping("/{equipmentId}/workflow/retrieval")
    public ApiResponse<JsonNode> getWorkflowRetrieval(@PathVariable String equipmentId,
                                                      @RequestBody(required = false) EquipmentWorkflowRequest request) {
        return ApiResponse.success(equipmentInteractionService.getRetrieval(equipmentId, request));
    }

    @PostMapping("/{equipmentId}/workflow/execution")
    public ApiResponse<JsonNode> getWorkflowExecution(@PathVariable String equipmentId,
                                                      @RequestBody(required = false) EquipmentWorkflowRequest request) {
        return ApiResponse.success(equipmentInteractionService.getExecution(equipmentId, request));
    }

    @PostMapping("/{equipmentId}/workflow/feedback")
    public ApiResponse<JsonNode> getWorkflowFeedback(@PathVariable String equipmentId,
                                                     @RequestBody(required = false) EquipmentWorkflowRequest request) {
        return ApiResponse.success(equipmentInteractionService.getFeedback(equipmentId, request));
    }
}
