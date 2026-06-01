package com.demo.ops.aircraft.service;

import com.demo.ops.aircraft.model.AircraftDiagnosisRequest;
import com.demo.ops.aircraft.model.AircraftDispositionRequest;
import com.demo.ops.common.service.JsonResourceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

/**
 * 飞机维修决策服务。
 * 提供飞机故障诊断、MEL 保留放行评估、环境查询、知识图谱等核心业务逻辑，
 * 从 JSON 数据集中读取伪数据并注入请求参数后返回。
 */
@Service
public class AircraftDecisionService {

    /** 默认故障部件：左主起落架减震支柱 */
    private static final String DEFAULT_COMPONENT = "左主起落架减震支柱";

    private final JsonResourceService jsonResourceService;
    private final ObjectMapper objectMapper;

    public AircraftDecisionService(JsonResourceService jsonResourceService, ObjectMapper objectMapper) {
        this.jsonResourceService = jsonResourceService;
        this.objectMapper = objectMapper;
    }

    /** 查询飞机历史状态数据 */
    public JsonNode getStatusHistory(String tailNo) {
        ObjectNode node = jsonResourceService.readObject("datasets/aircraft/status-history.json");
        node.put("tailNo", tailNo);
        return node;
    }

    /**
     * 评估 MEL 保留放行条件。
     * 根据渗漏面积（默认 15cm²）和是否持续滴落，判断是否可保留放行（限值 10cm² 且无持续滴落）。
     */
    public JsonNode getMelRelease(String tailNo, String component, Double leakAreaCm2, Boolean continuousDrip) {
        double actualLeakArea = leakAreaCm2 == null ? 15.0D : leakAreaCm2;
        boolean actualContinuousDrip = continuousDrip != null && continuousDrip;
        boolean releasable = actualLeakArea <= 10.0D && !actualContinuousDrip;

        ObjectNode node = objectMapper.createObjectNode();
        node.put("tailNo", tailNo);
        node.put("component", component == null || component.trim().isEmpty() ? DEFAULT_COMPONENT : component);
        node.put("melItem", "32-10-02-01");
        node.put("melTitle", "主起落架减震支柱内筒渗漏");
        node.put("thresholdCm2", 10.0D);
        node.put("observedLeakAreaCm2", actualLeakArea);
        node.put("continuousDrip", actualContinuousDrip);
        node.put("releasable", releasable);
        node.put("releaseProhibited", !releasable);
        node.put("repairDueDays", releasable ? 10 : 0);
        node.put("decision", releasable ? "可保留放行" : "不可保留，禁止放行");
        node.put("reason", buildMelReason(actualLeakArea, actualContinuousDrip));
        node.put("followUpAction", releasable ? "按 MEL 保留放行并安排 10 天内维修" : "必须排故后才能恢复适航");
        return node;
    }

    /** 查询当前环境数据（温度、湿度、天气等） */
    public JsonNode getEnvironment(String tailNo) {
        ObjectNode node = jsonResourceService.readObject("datasets/aircraft/environment.json");
        node.put("tailNo", tailNo);
        return node;
    }

    /** 查询故障排查知识库 */
    public JsonNode getTroubleshootingKnowledge(String tailNo) {
        ObjectNode node = jsonResourceService.readObject("datasets/aircraft/troubleshooting-kb.json");
        node.put("tailNo", tailNo);
        return node;
    }

    /** 查询知识图谱（故障与部件的关联关系） */
    public JsonNode getKnowledgeGraph(String tailNo) {
        ObjectNode node = jsonResourceService.readObject("datasets/aircraft/knowledge-graph.json");
        node.put("tailNo", tailNo);
        return node;
    }

    /** 查询可调配的维修人员信息 */
    public JsonNode getPersonnelMatch(String tailNo) {
        ObjectNode node = jsonResourceService.readObject("datasets/aircraft/personnel-match.json");
        node.put("tailNo", tailNo);
        return node;
    }

    /** 查询可用库存备件 */
    public JsonNode getInventory(String tailNo) {
        ObjectNode node = jsonResourceService.readObject("datasets/aircraft/inventory.json");
        node.put("tailNo", tailNo);
        return node;
    }

    /**
     * 综合诊断并给出诊断结论。
     * 基于 MEL 评估结果和用户请求的维修目标，生成诊断摘要和严重等级。
     */
    public JsonNode getDiagnosisConclusion(String tailNo, AircraftDiagnosisRequest request) {
        AircraftDiagnosisRequest actualRequest = request == null ? new AircraftDiagnosisRequest() : request;
        double actualLeakArea = actualRequest.getLeakAreaCm2() == null ? 15.0D : actualRequest.getLeakAreaCm2();
        boolean actualContinuousDrip = actualRequest.getContinuousDrip() != null && actualRequest.getContinuousDrip();
        boolean releasable = actualLeakArea <= 10.0D && !actualContinuousDrip;

        ObjectNode node = jsonResourceService.readObject("datasets/aircraft/diagnosis-conclusion.json");
        node.put("tailNo", tailNo);
        node.put("repairTargetRequested",
                actualRequest.getRepairTarget() == null ? "完全修复" : actualRequest.getRepairTarget());
        node.set("melAssessment", getMelRelease(tailNo, DEFAULT_COMPONENT, actualLeakArea, actualContinuousDrip));
        node.put("summary", releasable
                ? "当前渗漏满足 MEL 保留条件，可在 10 天维修窗口内保留放行。"
                : "左主起落架减震支柱渗漏超出 MEL 保留标准，当前不可保留放行。");
        node.put("severity", releasable ? "P2" : "P0");
        return node;
    }

    /**
     * 处置决策生成。
     * 基于用户请求的维修目标和渗漏参数，结合 MEL 评估结果生成处置方案。
     */
    public JsonNode getDispositionDecision(String tailNo, AircraftDispositionRequest request) {
        AircraftDispositionRequest actualRequest = request == null ? new AircraftDispositionRequest() : request;
        double actualLeakArea = actualRequest.getLeakAreaCm2() == null ? 15.0D : actualRequest.getLeakAreaCm2();
        boolean actualContinuousDrip = actualRequest.getContinuousDrip() != null && actualRequest.getContinuousDrip();

        ObjectNode node = jsonResourceService.readObject("datasets/aircraft/disposition-decision.json");
        node.put("tailNo", tailNo);
        node.put("repairTarget", actualRequest.getRepairTarget() == null ? "完全修复" : actualRequest.getRepairTarget());
        node.set("melAssessment", getMelRelease(tailNo, DEFAULT_COMPONENT, actualLeakArea, actualContinuousDrip));
        return node;
    }

    /** 构建 MEL 评估的原因说明文本 */
    private String buildMelReason(double leakAreaCm2, boolean continuousDrip) {
        if (continuousDrip) {
            return "存在持续滴落，不满足 MEL 保留条件";
        }
        if (leakAreaCm2 > 10.0D) {
            return "渗漏面积超过 10cm² 限值";
        }
        return "渗漏面积和滴落条件均满足 MEL 保留放行要求";
    }
}
