package com.demo.ops.aircraft.controller;

import com.demo.ops.aircraft.model.AircraftDiagnosisRequest;
import com.demo.ops.aircraft.model.AircraftDispositionRequest;
import com.demo.ops.aircraft.service.AircraftDecisionService;
import com.demo.ops.common.model.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 飞机维修 REST API 控制器。
 * 提供飞机故障诊断与处置决策相关的 RESTful 接口，路径前缀为 /api/v1/aircraft。
 */
@RestController
@RequestMapping("/api/v1/aircraft")
public class AircraftController {

    private final AircraftDecisionService aircraftDecisionService;

    public AircraftController(AircraftDecisionService aircraftDecisionService) {
        this.aircraftDecisionService = aircraftDecisionService;
    }

    /** 查询飞机历史状态 */
    @GetMapping("/{tailNo}/status-history")
    public ApiResponse<JsonNode> getStatusHistory(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getStatusHistory(tailNo));
    }

    /** 查询 MEL 保留放行评估结果（含渗漏面积等参数） */
    @GetMapping("/{tailNo}/mel-release")
    public ApiResponse<JsonNode> getMelRelease(@PathVariable String tailNo,
                                               @RequestParam(required = false) String component,
                                               @RequestParam(required = false) Double leakAreaCm2,
                                               @RequestParam(required = false, defaultValue = "false") Boolean continuousDrip) {
        return ApiResponse.success(aircraftDecisionService.getMelRelease(tailNo, component, leakAreaCm2, continuousDrip));
    }

    /** 查询当前环境数据 */
    @GetMapping("/{tailNo}/environment")
    public ApiResponse<JsonNode> getEnvironment(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getEnvironment(tailNo));
    }

    /** 查询故障排查知识库 */
    @GetMapping("/{tailNo}/troubleshooting-kb")
    public ApiResponse<JsonNode> getTroubleshootingKnowledge(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getTroubleshootingKnowledge(tailNo));
    }

    /** 查询知识图谱 */
    @GetMapping("/{tailNo}/knowledge-graph")
    public ApiResponse<JsonNode> getKnowledgeGraph(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getKnowledgeGraph(tailNo));
    }

    /** 查询人员匹配信息 */
    @GetMapping("/{tailNo}/personnel-match")
    public ApiResponse<JsonNode> getPersonnelMatch(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getPersonnelMatch(tailNo));
    }

    /** 查询库存备件信息 */
    @GetMapping("/{tailNo}/inventory")
    public ApiResponse<JsonNode> getInventory(@PathVariable String tailNo) {
        return ApiResponse.success(aircraftDecisionService.getInventory(tailNo));
    }

    /** 提交诊断请求并获取诊断结论 */
    @PostMapping("/{tailNo}/diagnosis-conclusion")
    public ApiResponse<JsonNode> getDiagnosisConclusion(@PathVariable String tailNo,
                                                        @RequestBody(required = false) AircraftDiagnosisRequest request) {
        return ApiResponse.success(aircraftDecisionService.getDiagnosisConclusion(tailNo, request));
    }

    /** 提交处置请求并获取处置决策 */
    @PostMapping("/{tailNo}/disposition-decision")
    public ApiResponse<JsonNode> getDispositionDecision(@PathVariable String tailNo,
                                                        @RequestBody(required = false) AircraftDispositionRequest request) {
        return ApiResponse.success(aircraftDecisionService.getDispositionDecision(tailNo, request));
    }
}
