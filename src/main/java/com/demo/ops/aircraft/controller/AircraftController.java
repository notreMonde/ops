package com.demo.ops.aircraft.controller;

import com.demo.ops.aircraft.model.AircraftDiagnosisRequest;
import com.demo.ops.aircraft.model.AircraftDispositionRequest;
import com.demo.ops.aircraft.model.AircraftWorkflowRequest;
import com.demo.ops.aircraft.service.AircraftDecisionService;
import com.demo.ops.aircraft.service.AircraftInteractionService;
import com.demo.ops.common.model.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/aircraft")
public class AircraftController {

    private final AircraftDecisionService aircraftDecisionService;
    private final AircraftInteractionService aircraftInteractionService;

    public AircraftController(AircraftDecisionService aircraftDecisionService,
                              AircraftInteractionService aircraftInteractionService) {
        this.aircraftDecisionService = aircraftDecisionService;
        this.aircraftInteractionService = aircraftInteractionService;
    }

    @GetMapping("/{tailNo}/status-history")
    public ApiResponse<JsonNode> getStatusHistory(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getStatusHistory(tailNo));
    }

    @GetMapping("/{tailNo}/mel-release")
    public ApiResponse<JsonNode> getMelRelease(@PathVariable String tailNo,
                                               @RequestParam(required = false) String component,
                                               @RequestParam(required = false) Double leakAreaCm2,
                                               @RequestParam(required = false, defaultValue = "false") Boolean continuousDrip) {
        return ApiResponse.success(aircraftDecisionService.getMelRelease(tailNo, component, leakAreaCm2, continuousDrip));
    }

    @GetMapping("/{tailNo}/environment")
    public ApiResponse<JsonNode> getEnvironment(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getEnvironment(tailNo));
    }

    @GetMapping("/{tailNo}/troubleshooting-kb")
    public ApiResponse<JsonNode> getTroubleshootingKnowledge(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getTroubleshootingKnowledge(tailNo));
    }

    @GetMapping("/{tailNo}/knowledge-graph")
    public ApiResponse<JsonNode> getKnowledgeGraph(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getKnowledgeGraph(tailNo));
    }

    @GetMapping("/{tailNo}/personnel-match")
    public ApiResponse<JsonNode> getPersonnelMatch(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getPersonnelMatch(tailNo));
    }

    @GetMapping("/{tailNo}/inventory")
    public ApiResponse<JsonNode> getInventory(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getInventory(tailNo));
    }

    @PostMapping("/{tailNo}/diagnosis-conclusion")
    public ApiResponse<JsonNode> getDiagnosisConclusion(@PathVariable String tailNo,
                                                        @RequestBody(required = false) AircraftDiagnosisRequest request) {
        return ApiResponse.success(aircraftDecisionService.getDiagnosisConclusion(tailNo, request));
    }

    @PostMapping("/{tailNo}/disposition-decision")
    public ApiResponse<JsonNode> getDispositionDecision(@PathVariable String tailNo,
                                                        @RequestBody(required = false) AircraftDispositionRequest request) {
        return ApiResponse.success(aircraftDecisionService.getDispositionDecision(tailNo, request));
    }

    @PostMapping("/{tailNo}/workflow/clarify")
    public ApiResponse<JsonNode> getWorkflowClarify(@PathVariable String tailNo,
                                                    @RequestBody(required = false) AircraftWorkflowRequest request) {
        return ApiResponse.success(aircraftInteractionService.getClarify(tailNo, request));
    }

    @PostMapping("/{tailNo}/workflow/retrieval")
    public ApiResponse<JsonNode> getWorkflowRetrieval(@PathVariable String tailNo,
                                                      @RequestBody(required = false) AircraftWorkflowRequest request) {
        return ApiResponse.success(aircraftInteractionService.getRetrieval(tailNo, request));
    }

    @PostMapping("/{tailNo}/workflow/execution")
    public ApiResponse<JsonNode> getWorkflowExecution(@PathVariable String tailNo,
                                                      @RequestBody(required = false) AircraftWorkflowRequest request) {
        return ApiResponse.success(aircraftInteractionService.getExecution(tailNo, request));
    }

    @PostMapping("/{tailNo}/workflow/feedback")
    public ApiResponse<JsonNode> getWorkflowFeedback(@PathVariable String tailNo,
                                                     @RequestBody(required = false) AircraftWorkflowRequest request) {
        return ApiResponse.success(aircraftInteractionService.getFeedback(tailNo, request));
    }
}
