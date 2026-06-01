package com.demo.ops.aircraft.service;

import com.demo.ops.aircraft.model.AircraftDiagnosisRequest;
import com.demo.ops.aircraft.model.AircraftDispositionRequest;
import com.demo.ops.aircraft.model.AircraftWorkflowRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AircraftInteractionService {

    private static final String FOLLOW_UP_ACTION = "接下来会深挖历史/变更/资源事实后再生成方案";
    private static final String DEFAULT_COMPONENT = "左主起落架减震支柱";
    private static final String DEFAULT_REPAIR_TARGET = "完全修复";

    private final AircraftDecisionService aircraftDecisionService;
    private final ObjectMapper objectMapper;

    public AircraftInteractionService(AircraftDecisionService aircraftDecisionService, ObjectMapper objectMapper) {
        this.aircraftDecisionService = aircraftDecisionService;
        this.objectMapper = objectMapper;
    }

    public JsonNode getClarify(String tailNo, AircraftWorkflowRequest request) {
        AircraftWorkflowRequest actualRequest = ensureRequest(request);
        ObjectNode statusHistory = (ObjectNode) aircraftDecisionService.getStatusHistory(tailNo);

        ObjectNode result = baseStage("clarify");
        ObjectNode basicInfo = result.putObject("basicInfo");
        basicInfo.put("tailNo", tailNo);
        basicInfo.put("aircraftModel", statusHistory.path("aircraftModel").asText(""));
        basicInfo.put("flightStatus", statusHistory.path("flightStatus").asText(""));

        boolean basicInfoConfirmed = isTrue(actualRequest.getBasicInfoConfirmed());
        boolean followUpAcknowledged = isTrue(actualRequest.getFollowUpAcknowledged());

        result.put("followUpAction", FOLLOW_UP_ACTION);
        result.put("clarificationRequired", true);
        result.put("basicInfoConfirmed", basicInfoConfirmed);
        result.put("followUpAcknowledged", followUpAcknowledged);
        result.put("canProceedToRetrieval", basicInfoConfirmed && followUpAcknowledged);
        result.put("nextStep", basicInfoConfirmed && followUpAcknowledged
                ? "澄清已完成，可直接进入检索阶段。"
                : "请先确认基础信息是否正确，并确认已知晓后续会先深挖事实再生成方案。");
        return result;
    }

    public JsonNode getRetrieval(String tailNo, AircraftWorkflowRequest request) {
        AircraftWorkflowRequest actualRequest = ensureRequest(request);
        if (!isClarified(actualRequest)) {
            return blockedRetrieval("澄清阶段尚未完成确认");
        }
        return buildRetrievalResult(tailNo, actualRequest);
    }

    public JsonNode getExecution(String tailNo, AircraftWorkflowRequest request) {
        AircraftWorkflowRequest actualRequest = ensureRequest(request);
        if (!isClarified(actualRequest)) {
            return blockedExecution("澄清阶段尚未完成确认");
        }

        ObjectNode retrieval = buildRetrievalResult(tailNo, actualRequest);
        ObjectNode statusHistory = (ObjectNode) aircraftDecisionService.getStatusHistory(tailNo);
        ObjectNode diagnosis = buildDiagnosis(tailNo, actualRequest, statusHistory);
        ObjectNode melSummary = (ObjectNode) retrieval.path("retrievedInfo").path("melSummary");

        boolean melKnown = actualRequest.getContinuousDrip() != null;
        boolean melReleasable = melSummary.path("releasable").asBoolean(false);
        String recommendedOptionCode = determineRecommendedOption(actualRequest, melKnown, melReleasable);

        ObjectNode result = baseStage("execution");
        result.set("retrievedInfo", retrieval.path("retrievedInfo"));
        result.set("knownConstraints", retrieval.path("knownConstraints"));
        result.set("retrievalSummary", retrieval.path("retrievalSummary"));

        ArrayNode options = result.putArray("options");
        options.add(buildOption(
                "A",
                "转入机库做完整修复",
                "适用于 MEL 不满足、风险已收敛到必须根治，或用户目标就是一次性消除渗漏的场景。",
                "A".equals(recommendedOptionCode),
                true,
                arrayOf("一次性消除主要渗漏风险", "便于同步安排检验、航材和工装"),
                arrayOf("停场时间更长", "需要占用机库资源并协调拖机"),
                arrayOf("机库位可用", "封严组合件、液压油和专用工具到位")));
        options.add(buildOption(
                "B",
                "按 MEL 保留放行并锁定窗口期维修",
                "适用于已经满足 MEL 条件，且当前更看重短期恢复运行能力的场景。",
                "B".equals(recommendedOptionCode),
                melKnown && melReleasable,
                arrayOf("对当前航班影响最小", "可把完整维修转移到计划窗口执行"),
                arrayOf("需要持续复核渗漏状态", "一旦状态恶化必须立即退出保留放行路径"),
                arrayOf("已确认无持续滴落", "渗漏面积保持在 MEL 阈值内")));
        options.add(buildOption(
                "C",
                "先做现场复核再收敛方案",
                "适用于还需要用现场事实进一步缩小范围，尤其是持续滴落状态尚未确认的场景。",
                "C".equals(recommendedOptionCode),
                true,
                arrayOf("避免在关键事实缺失时直接拍板", "可以先复核滴落趋势、现场状态和环境限制"),
                arrayOf("总处理时长可能拉长", "复核结束后仍可能回到完整修复方案"),
                arrayOf("安排授权主修现场复核", "复核后重新评估 MEL 条件")));

        ObjectNode analysis = result.putObject("analysis");
        analysis.put("recommendedOptionCode", recommendedOptionCode);
        analysis.put("severity", diagnosis.path("severity").asText(""));
        analysis.put("diagnosisSummary", diagnosis.path("summary").asText(""));
        analysis.put("melDecision", melSummary.path("decision").asText(""));
        analysis.put("melReleasable", melKnown && melReleasable);

        result.put("selectionPrompt", "请从 A / B / C 中选择一个方案，执行阶段会保留检索阶段的压缩关键信息供你比对。");
        result.put("nextStep", "选择方案后进入反馈阶段，我会返回所选方案的最终执行回执。");
        return result;
    }

    public JsonNode getFeedback(String tailNo, AircraftWorkflowRequest request) {
        AircraftWorkflowRequest actualRequest = ensureRequest(request);
        if (!isClarified(actualRequest)) {
            return blockedFeedback("澄清阶段尚未完成确认");
        }

        ObjectNode execution = (ObjectNode) getExecution(tailNo, actualRequest);
        if ("blocked".equals(execution.path("executionStatus").asText(""))) {
            return blockedFeedback(execution.path("blockingReason").asText("执行阶段未满足前置条件"));
        }

        String selectedOptionCode = normalizeOptionCode(actualRequest.getSelectedOptionCode());
        if (selectedOptionCode == null) {
            ObjectNode result = baseStage("feedback");
            result.put("executionStatus", "pending-selection");
            result.set("retrievedInfo", execution.path("retrievedInfo"));
            result.set("options", execution.path("options"));
            result.put("blockingReason", "尚未选择执行方案");
            result.put("nextStep", "请先在执行阶段确认方案编码，再进入反馈阶段。");
            return result;
        }

        ObjectNode retrieval = buildRetrievalResult(tailNo, actualRequest);
        ObjectNode melSummary = (ObjectNode) retrieval.path("retrievedInfo").path("melSummary");
        boolean melKnown = actualRequest.getContinuousDrip() != null;
        boolean melReleasable = melSummary.path("releasable").asBoolean(false);

        if ("B".equals(selectedOptionCode) && (!melKnown || !melReleasable)) {
            ObjectNode result = baseStage("feedback");
            result.put("executionStatus", "blocked");
            result.put("selectedOptionCode", "B");
            result.put("blockingReason", melKnown
                    ? "当前事实不满足 MEL 保留放行条件"
                    : "持续滴落状态尚未确认，不能直接确认方案 B");
            result.put("nextStep", "请改选方案 A/C，或先补充持续滴落事实后重新进入执行阶段。");
            return result;
        }

        ObjectNode statusHistory = (ObjectNode) aircraftDecisionService.getStatusHistory(tailNo);
        ObjectNode diagnosis = buildDiagnosis(tailNo, actualRequest, statusHistory);
        ObjectNode result = baseStage("feedback");
        result.put("executionStatus", "confirmed");
        result.put("selectedOptionCode", selectedOptionCode);

        ObjectNode finalPlan = result.putObject("finalPlan");
        if ("A".equals(selectedOptionCode)) {
            JsonNode disposition = aircraftDecisionService.getDispositionDecision(
                    tailNo,
                    toDispositionRequest(resolveLeakArea(actualRequest, statusHistory), actualRequest.getContinuousDrip(), resolveRepairTarget(actualRequest.getRepairTarget())));
            finalPlan.put("summary", "按完整修复路径处理左主起落架减震支柱渗漏，并在机库内完成复检。");
            finalPlan.set("executionSteps", disposition.path("planSteps"));
            finalPlan.set("expectedResults", arrayOf("渗漏风险收敛到可接受范围", "完成复检后可恢复标准适航状态"));
            finalPlan.set("cautions", arrayOf("拖机前确认机库资源和检验资源已锁定", "拆装过程同步记录关键扭矩和试验结果"));
            finalPlan.set("rollbackPlan", arrayOf("若拆检后发现镜面或内筒损伤超预期，则升级更大范围部件更换", "若试验后仍存在异常渗漏，则保持停场并重新做根因排查"));
            finalPlan.set("resources", buildAircraftResources(
                    aircraftDecisionService.getPersonnelMatch(tailNo),
                    aircraftDecisionService.getInventory(tailNo)));
            finalPlan.set("evidence", diagnosis.path("evidence"));
            result.put("nextAction", "请安排机库位、授权主修和检验资源，按完整修复路线推进。");
            return result;
        }

        if ("B".equals(selectedOptionCode)) {
            finalPlan.put("summary", "按 MEL 条件短期保留放行，并在维修窗口内完成完整修复。");
            finalPlan.set("executionSteps", arrayOf(
                    "复核渗漏面积、滴落状态和 MEL 条款匹配关系并形成放行记录",
                    "在每次执行前后记录渗漏变化，发现恶化立即退出保留放行",
                    "提前锁定窗口期的机库位、航材、检验和主修资源"));
            finalPlan.set("expectedResults", arrayOf("在不突破 MEL 条件的前提下维持短期运行", "把完整修复转移到可控的计划停场窗口"));
            finalPlan.set("cautions", arrayOf("任何持续滴落或面积扩大都要立即重评", "放行依据需随每次状态变化同步复核"));
            finalPlan.set("rollbackPlan", arrayOf("一旦转为持续滴落，立即切换到方案 A", "若窗口期资源失效，重新评估后续航班安排"));
            finalPlan.set("resources", buildAircraftResources(
                    aircraftDecisionService.getPersonnelMatch(tailNo),
                    aircraftDecisionService.getInventory(tailNo)));
            result.put("nextAction", "请确认是否采用 MEL 保留放行，并锁定窗口期维修时间。");
            return result;
        }

        finalPlan.put("summary", "先做现场复核和状态再确认，再决定是转入 MEL 保留放行还是直接完整修复。");
        finalPlan.set("executionSteps", arrayOf(
                "安排授权主修做现场清洁、复测和滴落趋势观察",
                "复核历史记录、AMM 关键步骤和现场环境限制",
                "根据复核结果重新评估 MEL 条件，再回到执行阶段收敛方案"));
        finalPlan.set("expectedResults", arrayOf("补齐持续滴落和现场趋势等关键事实", "避免在假设条件下直接确认最终方案"));
        finalPlan.set("cautions", arrayOf("复核期间仍需控制机位作业风险", "若观察到状态恶化，不应继续停留在复核阶段"));
        finalPlan.set("rollbackPlan", arrayOf("若复核结论仍不清晰，则升级为完整修复路线", "若确认持续滴落，立即放弃保留放行路径"));
        finalPlan.set("resources", buildAircraftResources(
                aircraftDecisionService.getPersonnelMatch(tailNo),
                aircraftDecisionService.getInventory(tailNo)));
        result.put("nextAction", "请先安排现场复核，复核完成后重新进入执行阶段选定最终方案。");
        return result;
    }

    private ObjectNode buildRetrievalResult(String tailNo, AircraftWorkflowRequest request) {
        ObjectNode statusHistory = (ObjectNode) aircraftDecisionService.getStatusHistory(tailNo);
        ObjectNode environment = (ObjectNode) aircraftDecisionService.getEnvironment(tailNo);
        ObjectNode troubleshootingKnowledge = (ObjectNode) aircraftDecisionService.getTroubleshootingKnowledge(tailNo);
        ObjectNode knowledgeGraph = (ObjectNode) aircraftDecisionService.getKnowledgeGraph(tailNo);
        ObjectNode personnel = (ObjectNode) aircraftDecisionService.getPersonnelMatch(tailNo);
        ObjectNode inventory = (ObjectNode) aircraftDecisionService.getInventory(tailNo);
        ObjectNode diagnosis = buildDiagnosis(tailNo, request, statusHistory);
        ObjectNode melAssessment = buildMelAssessmentDetail(tailNo, request, statusHistory);

        ObjectNode result = baseStage("retrieval");
        result.put("retrievalStatus", "completed");

        ObjectNode retrievedInfo = result.putObject("retrievedInfo");
        retrievedInfo.set("statusHistorySummary", buildStatusHistorySummary(statusHistory));
        retrievedInfo.set("melSummary", buildMelSummary(melAssessment));
        retrievedInfo.set("environmentSummary", buildEnvironmentSummary(environment));
        retrievedInfo.set("resourceSummary", buildAircraftResourceSummary(personnel, inventory));

        ObjectNode retrievedDetails = result.putObject("retrievedDetails");
        retrievedDetails.set("statusHistory", statusHistory);
        retrievedDetails.set("melAssessment", melAssessment);
        retrievedDetails.set("environment", environment);
        retrievedDetails.set("troubleshootingKnowledge", troubleshootingKnowledge);
        retrievedDetails.set("knowledgeGraph", knowledgeGraph);
        retrievedDetails.set("personnelMatch", personnel);
        retrievedDetails.set("inventory", inventory);
        retrievedDetails.set("diagnosisConclusion", diagnosis);

        ArrayNode knownConstraints = result.putArray("knownConstraints");
        appendAircraftConstraints(knownConstraints, environment, request);

        ObjectNode retrievalSummary = result.putObject("retrievalSummary");
        retrievalSummary.put("severity", diagnosis.path("severity").asText(""));
        retrievalSummary.put("coreFinding", diagnosis.path("summary").asText(""));
        retrievalSummary.put("executionReadiness", "已展示完整检索结果，可继续进入执行阶段。");
        retrievalSummary.put("detailAvailable", true);

        result.put("canProceedToExecution", true);
        result.put("nextStep", "检索已完成，当前已同时展示摘要和完整详情，可进入执行阶段。");
        return result;
    }

    private ObjectNode buildStatusHistorySummary(ObjectNode statusHistory) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("base", statusHistory.path("base").asText(""));
        summary.put("measuredLeakAreaCm2", statusHistory.path("currentFaultSnapshot").path("measuredLeakAreaCm2").asDouble(0.0D));
        summary.put("manualThresholdCm2", statusHistory.path("currentFaultSnapshot").path("manualThresholdCm2").asDouble(0.0D));
        summary.put("lastSealReplacementDate", statusHistory.path("maintenanceHistory").path("lastSealReplacementDate").asText(""));
        JsonNode latestFinding = firstArrayItem(statusHistory.path("historicalFindings"));
        summary.put("latestFinding", latestFinding == null ? "" : latestFinding.path("finding").asText(""));
        summary.put("latestAction", latestFinding == null ? "" : latestFinding.path("action").asText(""));
        return summary;
    }

    private ObjectNode buildMelSummary(ObjectNode melAssessment) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("component", melAssessment.path("component").asText(""));
        summary.put("observedLeakAreaCm2", melAssessment.path("observedLeakAreaCm2").asDouble(0.0D));
        summary.put("thresholdCm2", melAssessment.path("thresholdCm2").asDouble(0.0D));
        summary.put("continuousDripKnown", melAssessment.path("continuousDripKnown").asBoolean(false));
        if (melAssessment.path("continuousDrip").isNull()) {
            summary.putNull("continuousDrip");
        } else {
            summary.put("continuousDrip", melAssessment.path("continuousDrip").asBoolean(false));
        }
        summary.put("releasable", melAssessment.path("releasable").asBoolean(false));
        summary.put("decision", melAssessment.path("decision").asText(""));
        summary.put("reason", melAssessment.path("reason").asText(""));
        return summary;
    }

    private ObjectNode buildMelAssessmentDetail(String tailNo, AircraftWorkflowRequest request, ObjectNode statusHistory) {
        double leakArea = resolveLeakArea(request, statusHistory);
        if (request.getContinuousDrip() == null) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("tailNo", tailNo);
            node.put("component", resolveComponent(request.getComponent()));
            node.put("melItem", "32-10-02-01");
            node.put("melTitle", "主起落架减震支柱内筒渗漏");
            node.put("thresholdCm2", 10.0D);
            node.put("observedLeakAreaCm2", leakArea);
            node.put("continuousDripKnown", false);
            node.putNull("continuousDrip");
            node.put("releasable", false);
            node.put("releaseProhibited", true);
            node.put("repairDueDays", 0);
            node.put("decision", "待补充持续滴落状态后确认");
            node.put("reason", "当前已拿到渗漏面积，但持续滴落状态仍未确认。");
            node.put("followUpAction", "先补充持续滴落状态，再确认是否可按 MEL 保留放行。");
            return node;
        }
        ObjectNode melRelease = (ObjectNode) aircraftDecisionService.getMelRelease(
                tailNo,
                resolveComponent(request.getComponent()),
                leakArea,
                request.getContinuousDrip());
        melRelease.put("continuousDripKnown", true);
        return melRelease;
    }

    private ObjectNode buildEnvironmentSummary(ObjectNode environment) {
        ObjectNode summary = objectMapper.createObjectNode();
        JsonNode currentStand = environment.path("currentStand");
        JsonNode nearestHangar = findFirstAvailable(environment.path("hangars"));

        summary.put("airport", environment.path("airport").asText(""));
        summary.put("currentBay", currentStand.path("bay").asText(""));
        summary.put("standType", currentStand.path("standType").asText(""));
        summary.put("hasCanopy", currentStand.path("hasCanopy").asBoolean(false));
        summary.put("nightTemperatureC", currentStand.path("nightTemperatureC").asDouble(0.0D));
        summary.put("nearestAvailableHangar", nearestHangar == null ? "" : nearestHangar.path("name").asText(""));
        summary.put("nearestAvailableHangarDistanceMeters", nearestHangar == null ? 0 : nearestHangar.path("distanceMeters").asInt(0));
        return summary;
    }

    private ObjectNode buildAircraftResourceSummary(ObjectNode personnel, ObjectNode inventory) {
        ObjectNode summary = objectMapper.createObjectNode();
        JsonNode firstMaterial = firstArrayItem(inventory.path("materials"));
        JsonNode firstTool = firstArrayItem(inventory.path("tools"));
        summary.put("recommendedLead", personnel.path("recommendedLead").asText(""));
        summary.put("recommendedReleaseInspector", personnel.path("recommendedReleaseInspector").asText(""));
        summary.put("firstMaterial", firstMaterial == null ? "" : firstMaterial.path("name").asText(""));
        summary.put("firstMaterialStock", firstMaterial == null ? 0 : firstMaterial.path("quantity").asInt(0));
        summary.put("firstTool", firstTool == null ? "" : firstTool.path("name").asText(""));
        summary.put("firstToolStock", firstTool == null ? 0 : firstTool.path("quantity").asInt(0));
        return summary;
    }

    private void appendAircraftConstraints(ArrayNode knownConstraints, ObjectNode environment, AircraftWorkflowRequest request) {
        JsonNode currentStand = environment.path("currentStand");
        if (!currentStand.path("hasCanopy").asBoolean(false)) {
            knownConstraints.add("当前机位无雨棚，夜间和低温环境会抬高现场排故与复核成本。");
        }
        JsonNode nearestHangar = findFirstAvailable(environment.path("hangars"));
        if (nearestHangar != null) {
            knownConstraints.add("最近可用机库为 " + nearestHangar.path("name").asText("")
                    + "，距当前机位 " + nearestHangar.path("distanceMeters").asText("") + " 米。");
        }
        if (request.getContinuousDrip() == null) {
            knownConstraints.add("持续滴落状态尚未确认，方案 B 当前不能直接落地。");
        }
    }

    private ObjectNode buildDiagnosis(String tailNo, AircraftWorkflowRequest request, ObjectNode statusHistory) {
        return (ObjectNode) aircraftDecisionService.getDiagnosisConclusion(
                tailNo,
                toDiagnosisRequest(request, resolveLeakArea(request, statusHistory), resolveRepairTarget(request.getRepairTarget())));
    }

    private ObjectNode buildAircraftResources(JsonNode personnel, JsonNode inventory) {
        ObjectNode resources = objectMapper.createObjectNode();
        resources.set("recommendedPeople", selectRecommendedCandidates(personnel.path("candidates")));
        resources.set("materials", inventory.path("materials"));
        resources.set("tools", inventory.path("tools"));
        return resources;
    }

    private String determineRecommendedOption(AircraftWorkflowRequest request, boolean melKnown, boolean melReleasable) {
        if (!melKnown) {
            return "C";
        }
        if (!melReleasable) {
            return "A";
        }
        if (prefersRelease(request.getUserIntent(), request.getRepairTarget())) {
            return "B";
        }
        if (prefersCompleteRepair(request.getUserIntent(), request.getRepairTarget())) {
            return "A";
        }
        return "C";
    }

    private boolean prefersRelease(String userIntent, String repairTarget) {
        String intent = normalize(userIntent);
        String target = normalize(repairTarget);
        return intent.contains("放行") || intent.contains("周转") || intent.contains("尽快恢复") || target.contains("保留放行");
    }

    private boolean prefersCompleteRepair(String userIntent, String repairTarget) {
        String intent = normalize(userIntent);
        String target = normalize(repairTarget);
        return intent.contains("彻底") || intent.contains("根治") || target.contains("完全修复");
    }

    private ObjectNode blockedRetrieval(String reason) {
        ObjectNode result = baseStage("retrieval");
        result.put("retrievalStatus", "blocked");
        result.put("blockingReason", reason);
        result.put("canProceedToExecution", false);
        result.put("nextStep", "请先完成澄清阶段确认，再进入检索阶段。");
        return result;
    }

    private ObjectNode blockedExecution(String reason) {
        ObjectNode result = baseStage("execution");
        result.put("executionStatus", "blocked");
        result.put("blockingReason", reason);
        result.put("canSelectOption", false);
        result.put("nextStep", "请先完成澄清阶段确认，并先查看检索阶段的压缩事实信息。");
        return result;
    }

    private ObjectNode blockedFeedback(String reason) {
        ObjectNode result = baseStage("feedback");
        result.put("executionStatus", "blocked");
        result.put("blockingReason", reason);
        result.put("nextStep", "请先按 澄清 -> 检索 -> 执行 的顺序完成前置阶段。");
        return result;
    }

    private ObjectNode baseStage(String stage) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("stage", stage);
        node.put("interactionMode", "four-phase");
        return node;
    }

    private ObjectNode buildOption(String code,
                                   String title,
                                   String applicableScenario,
                                   boolean recommended,
                                   boolean feasible,
                                   ArrayNode advantages,
                                   ArrayNode risks,
                                   ArrayNode prerequisites) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("optionCode", code);
        node.put("title", title);
        node.put("applicableScenario", applicableScenario);
        node.put("recommended", recommended);
        node.put("feasible", feasible);
        node.set("advantages", advantages);
        node.set("risks", risks);
        node.set("prerequisites", prerequisites);
        return node;
    }

    private ArrayNode selectRecommendedCandidates(JsonNode candidates) {
        ArrayNode result = objectMapper.createArrayNode();
        if (candidates == null || !candidates.isArray()) {
            return result;
        }
        for (JsonNode candidate : candidates) {
            if (candidate.path("recommended").asBoolean(false) && candidate.path("onDuty").asBoolean(true)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private JsonNode findFirstAvailable(JsonNode hangars) {
        if (hangars == null || !hangars.isArray()) {
            return null;
        }
        for (JsonNode hangar : hangars) {
            if (hangar.path("available").asBoolean(false)) {
                return hangar;
            }
        }
        return null;
    }

    private JsonNode firstArrayItem(JsonNode node) {
        if (node != null && node.isArray() && node.size() > 0) {
            return node.get(0);
        }
        return null;
    }

    private double resolveLeakArea(AircraftWorkflowRequest request, ObjectNode statusHistory) {
        if (request.getLeakAreaCm2() != null) {
            return request.getLeakAreaCm2();
        }
        return statusHistory.path("currentFaultSnapshot").path("measuredLeakAreaCm2").asDouble(15.0D);
    }

    private String resolveRepairTarget(String repairTarget) {
        return hasText(repairTarget) ? repairTarget.trim() : DEFAULT_REPAIR_TARGET;
    }

    private String resolveComponent(String component) {
        return hasText(component) ? component.trim() : DEFAULT_COMPONENT;
    }

    private AircraftDiagnosisRequest toDiagnosisRequest(AircraftWorkflowRequest request, double leakArea, String repairTarget) {
        AircraftDiagnosisRequest diagnosisRequest = new AircraftDiagnosisRequest();
        diagnosisRequest.setLeakAreaCm2(leakArea);
        diagnosisRequest.setContinuousDrip(request.getContinuousDrip());
        diagnosisRequest.setRepairTarget(repairTarget);
        return diagnosisRequest;
    }

    private AircraftDispositionRequest toDispositionRequest(double leakArea, Boolean continuousDrip, String repairTarget) {
        AircraftDispositionRequest dispositionRequest = new AircraftDispositionRequest();
        dispositionRequest.setLeakAreaCm2(leakArea);
        dispositionRequest.setContinuousDrip(continuousDrip);
        dispositionRequest.setRepairTarget(repairTarget);
        return dispositionRequest;
    }

    private AircraftWorkflowRequest ensureRequest(AircraftWorkflowRequest request) {
        return request == null ? new AircraftWorkflowRequest() : request;
    }

    private boolean isClarified(AircraftWorkflowRequest request) {
        return isTrue(request.getBasicInfoConfirmed()) && isTrue(request.getFollowUpAcknowledged());
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private String normalizeOptionCode(String optionCode) {
        if (!hasText(optionCode)) {
            return null;
        }
        return optionCode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private ArrayNode arrayOf(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
