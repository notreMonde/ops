package com.demo.ops.equipment.service;

import com.demo.ops.equipment.model.EquipmentDiagnosisRequest;
import com.demo.ops.equipment.model.EquipmentDispositionRequest;
import com.demo.ops.equipment.model.EquipmentWorkflowRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class EquipmentInteractionService {

    private static final String FOLLOW_UP_ACTION = "接下来会深挖历史/变更/资源事实后再生成方案";

    private final EquipmentDecisionService equipmentDecisionService;
    private final ObjectMapper objectMapper;

    public EquipmentInteractionService(EquipmentDecisionService equipmentDecisionService, ObjectMapper objectMapper) {
        this.equipmentDecisionService = equipmentDecisionService;
        this.objectMapper = objectMapper;
    }

    public JsonNode getClarify(String equipmentId, EquipmentWorkflowRequest request) {
        EquipmentWorkflowRequest actualRequest = ensureRequest(request);
        ObjectNode digitalTwin = (ObjectNode) equipmentDecisionService.getDigitalTwin(equipmentId);

        ObjectNode result = baseStage("clarify");
        ObjectNode basicInfo = result.putObject("basicInfo");
        basicInfo.put("equipmentId", equipmentId);
        basicInfo.put("equipmentName", digitalTwin.path("equipmentName").asText(""));
        basicInfo.put("model", digitalTwin.path("model").asText(""));

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

    public JsonNode getRetrieval(String equipmentId, EquipmentWorkflowRequest request) {
        EquipmentWorkflowRequest actualRequest = ensureRequest(request);
        if (!isClarified(actualRequest)) {
            return blockedRetrieval("澄清阶段尚未完成确认");
        }
        return buildRetrievalResult(equipmentId, actualRequest);
    }

    public JsonNode getExecution(String equipmentId, EquipmentWorkflowRequest request) {
        EquipmentWorkflowRequest actualRequest = ensureRequest(request);
        if (!isClarified(actualRequest)) {
            return blockedExecution("澄清阶段尚未完成确认");
        }

        ObjectNode retrieval = buildRetrievalResult(equipmentId, actualRequest);
        ObjectNode telemetry = buildTelemetrySnapshot(equipmentId, actualRequest);
        ObjectNode diagnosis = (ObjectNode) equipmentDecisionService.getDiagnosisConclusion(
                equipmentId, toDiagnosisRequest(telemetry));
        boolean severe = isSevere(diagnosis, telemetry);
        String recommendedOptionCode = determineRecommendedOption(severe, actualRequest.getUserIntent());

        ObjectNode result = baseStage("execution");
        result.set("retrievedInfo", retrieval.path("retrievedInfo"));
        result.set("knownConstraints", retrieval.path("knownConstraints"));
        result.set("retrievalSummary", retrieval.path("retrievalSummary"));

        ArrayNode options = result.putArray("options");
        options.add(buildOption(
                "A",
                "立即停机并做拆检",
                "适用于高温、高振动或已经判断为高严重度的场景。",
                "A".equals(recommendedOptionCode),
                true,
                arrayOf("最快切断故障继续扩大的风险", "便于一次性检查轴承、喷嘴和润滑链路"),
                arrayOf("会立即影响当前产能", "需要立刻协调停机和维修资源"),
                arrayOf("停机窗口立即可用", "备件、加热器和拉马到位")));
        options.add(buildOption(
                "B",
                "降载运行并加密监测",
                "适用于趋势可控、严重度不高且当前更看重连续生产的场景。",
                "B".equals(recommendedOptionCode),
                !severe,
                arrayOf("对产能冲击最小", "可为计划停机争取准备时间"),
                arrayOf("趋势判断失误会放大故障风险", "需要严格执行阈值监测和升级机制"),
                arrayOf("仅适用于非 P0 场景", "每班次落实温振记录和责任人")));
        options.add(buildOption(
                "C",
                "锁定最近停机窗口做计划性检修",
                "适用于希望平衡风险和产能，并提前准备人、料、工具的场景。",
                "C".equals(recommendedOptionCode),
                true,
                arrayOf("方便提前协调备件和审批", "比临停更容易组织资源"),
                arrayOf("窗口到来前仍需守住预警阈值", "若状态恶化仍需切换到方案 A"),
                arrayOf("明确最近可用停机窗口", "提前预留轴承和润滑喷嘴")));

        ObjectNode analysis = result.putObject("analysis");
        analysis.put("recommendedOptionCode", recommendedOptionCode);
        analysis.put("severity", diagnosis.path("severity").asText(""));
        analysis.put("diagnosisSummary", diagnosis.path("summary").asText(""));

        result.put("selectionPrompt", "请从 A / B / C 中选择一个方案，执行阶段会继续展示检索阶段压缩后的关键信息。");
        result.put("nextStep", "选择方案后进入反馈阶段，我会返回所选方案的回执和执行安排。");
        return result;
    }

    public JsonNode getFeedback(String equipmentId, EquipmentWorkflowRequest request) {
        EquipmentWorkflowRequest actualRequest = ensureRequest(request);
        if (!isClarified(actualRequest)) {
            return blockedFeedback("澄清阶段尚未完成确认");
        }

        ObjectNode execution = (ObjectNode) getExecution(equipmentId, actualRequest);
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

        ObjectNode telemetry = buildTelemetrySnapshot(equipmentId, actualRequest);
        ObjectNode diagnosis = (ObjectNode) equipmentDecisionService.getDiagnosisConclusion(
                equipmentId, toDiagnosisRequest(telemetry));
        boolean severe = isSevere(diagnosis, telemetry);

        if ("B".equals(selectedOptionCode) && severe) {
            ObjectNode result = baseStage("feedback");
            result.put("executionStatus", "blocked");
            result.put("selectedOptionCode", "B");
            result.put("blockingReason", "当前温振趋势已处于高风险水平，不建议继续运行。");
            result.put("nextStep", "请改选方案 A，或选择方案 C 并尽快锁定停机窗口。");
            return result;
        }

        ObjectNode result = baseStage("feedback");
        result.put("executionStatus", "confirmed");
        result.put("selectedOptionCode", selectedOptionCode);

        ObjectNode finalPlan = result.putObject("finalPlan");
        if ("A".equals(selectedOptionCode)) {
            JsonNode disposition = equipmentDecisionService.getDispositionDecision(equipmentId, toDispositionRequest(actualRequest));
            finalPlan.put("summary", "立即停机并拆检主轴润滑与轴承链路，优先排除喷嘴堵塞和轴承损伤。");
            finalPlan.set("executionSteps", disposition.path("planSteps"));
            finalPlan.set("expectedResults", arrayOf("快速隔离故障扩散风险", "确认喷嘴、润滑链路和轴承的真实状态"));
            finalPlan.set("cautions", arrayOf("停机后先控温，再执行手动盘车和拆检", "更换轴承前同步复核润滑系统状态"));
            finalPlan.set("rollbackPlan", arrayOf("若喷嘴清理后问题仍在，则升级为轴承更换", "若更换轴承后振动仍异常，则继续排查主轴对中与润滑回路"));
            finalPlan.set("resources", buildEquipmentResources(
                    equipmentDecisionService.getPersonnelMatch(equipmentId),
                    equipmentDecisionService.getInventory(equipmentId)));
            result.put("nextAction", "请立即协调停机窗口和关键备件，按拆检路线执行。");
            return result;
        }

        if ("B".equals(selectedOptionCode)) {
            finalPlan.put("summary", "在受控降载前提下继续运行，并执行班次级加密监测。");
            finalPlan.set("executionSteps", arrayOf(
                    "将主轴负载降到保守区间，并限制高频重载工况",
                    "每班次记录温度、振动和温升速率，任一指标越阈立即停机",
                    "提前预留轴承和润滑喷嘴，准备随时切换停机检修"));
            finalPlan.set("expectedResults", arrayOf("在风险可控前提下争取短时间连续生产", "为计划检修争取备件和人员准备时间"));
            finalPlan.set("cautions", arrayOf("任何高温高振抬升都要触发停机", "该方案不适用于新增异响或卡滞迹象"));
            finalPlan.set("rollbackPlan", arrayOf("任一监测指标恶化立即切换到方案 A", "若计划窗口提前释放，可升级为方案 C 或直接停机"));
            finalPlan.set("resources", buildEquipmentResources(
                    equipmentDecisionService.getPersonnelMatch(equipmentId),
                    equipmentDecisionService.getInventory(equipmentId)));
            result.put("nextAction", "请确认是否接受降载运行，并指定监测责任人与升级阈值。");
            return result;
        }

        finalPlan.put("summary", "锁定最近停机窗口，按计划性检修路线准备人、料、工具。");
        finalPlan.set("executionSteps", arrayOf(
                "确认最近可用停机窗口并提前推送检修准备任务",
                "预留轴承、润滑喷嘴和专用工具，组织机械与电气配合",
                "窗口开始后执行喷嘴检查、润滑链路复核，并按结果决定是否更换轴承"));
        finalPlan.set("expectedResults", arrayOf("减少临时停机对产能的冲击", "在计划窗口内完成高概率根因处置"));
        finalPlan.set("cautions", arrayOf("窗口到来前仍需持续监测趋势", "若状态升级为 P0，不应继续等待计划窗口"));
        finalPlan.set("rollbackPlan", arrayOf("窗口前状态恶化则立即切换方案 A", "窗口内若排查结果不理想，则直接执行轴承更换"));
        finalPlan.set("resources", buildEquipmentResources(
                equipmentDecisionService.getPersonnelMatch(equipmentId),
                equipmentDecisionService.getInventory(equipmentId)));
        result.put("nextAction", "请确认最近可用停机窗口，我会基于该窗口继续细化执行安排。");
        return result;
    }

    private ObjectNode buildRetrievalResult(String equipmentId, EquipmentWorkflowRequest request) {
        ObjectNode digitalTwin = (ObjectNode) equipmentDecisionService.getDigitalTwin(equipmentId);
        ObjectNode telemetry = buildTelemetrySnapshot(equipmentId, request);
        ObjectNode historicalCases = (ObjectNode) equipmentDecisionService.getHistoricalCases(equipmentId);
        ObjectNode changeRelations = (ObjectNode) equipmentDecisionService.getChangeRelations(equipmentId);
        ObjectNode personnel = (ObjectNode) equipmentDecisionService.getPersonnelMatch(equipmentId);
        ObjectNode inventory = (ObjectNode) equipmentDecisionService.getInventory(equipmentId);
        ObjectNode diagnosis = (ObjectNode) equipmentDecisionService.getDiagnosisConclusion(equipmentId, toDiagnosisRequest(telemetry));

        ObjectNode result = baseStage("retrieval");
        result.put("retrievalStatus", "completed");

        ObjectNode retrievedInfo = result.putObject("retrievedInfo");
        retrievedInfo.set("telemetrySummary", buildTelemetrySummary(telemetry));
        retrievedInfo.set("historicalCaseSummary", buildHistoricalCaseSummary(historicalCases));
        retrievedInfo.set("changeRelationSummary", buildChangeRelationSummary(changeRelations));
        retrievedInfo.set("resourceSummary", buildEquipmentResourceSummary(personnel, inventory));
        retrievedInfo.set("diagnosisSummary", buildDiagnosisSummary(diagnosis, digitalTwin));

        ObjectNode retrievedDetails = result.putObject("retrievedDetails");
        retrievedDetails.set("digitalTwin", digitalTwin);
        retrievedDetails.set("telemetry", telemetry);
        retrievedDetails.set("historicalCases", historicalCases);
        retrievedDetails.set("changeRelations", changeRelations);
        retrievedDetails.set("personnelMatch", personnel);
        retrievedDetails.set("inventory", inventory);
        retrievedDetails.set("diagnosisConclusion", diagnosis);

        ArrayNode knownConstraints = result.putArray("knownConstraints");
        appendEquipmentConstraints(knownConstraints, digitalTwin, diagnosis);

        ObjectNode retrievalSummary = result.putObject("retrievalSummary");
        retrievalSummary.put("severity", diagnosis.path("severity").asText(""));
        retrievalSummary.put("coreFinding", diagnosis.path("summary").asText(""));
        retrievalSummary.put("executionReadiness", "已展示完整检索结果，可继续进入执行阶段。");
        retrievalSummary.put("detailAvailable", true);

        result.put("canProceedToExecution", true);
        result.put("nextStep", "检索已完成，当前已同时展示摘要和完整详情，可进入执行阶段。");
        return result;
    }

    private ObjectNode buildTelemetrySummary(ObjectNode telemetry) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("currentTemperatureC", telemetry.path("currentTemperatureC").asDouble(0.0D));
        summary.put("vibrationMmPerS", telemetry.path("vibrationMmPerS").asDouble(0.0D));
        summary.put("temperatureRiseRatePerMin", telemetry.path("temperatureRiseRatePerMin").asDouble(0.0D));
        summary.put("motorCurrentStatus", telemetry.path("motorCurrentStatus").asText(""));
        return summary;
    }

    private ObjectNode buildHistoricalCaseSummary(ObjectNode historicalCases) {
        ObjectNode summary = objectMapper.createObjectNode();
        JsonNode latestMaintenance = historicalCases.path("latestMaintenance");
        JsonNode topCase = firstArrayItem(historicalCases.path("similarCases"));
        summary.put("latestMaintenanceDate", latestMaintenance.path("date").asText(""));
        summary.put("latestMaintenanceAction", latestMaintenance.path("action").asText(""));
        summary.put("topCaseSimilarity", topCase == null ? 0.0D : topCase.path("similarity").asDouble(0.0D));
        summary.put("topCaseSummary", topCase == null ? "" : topCase.path("summary").asText(""));
        summary.put("topCaseResolution", topCase == null ? "" : topCase.path("resolvedBy").asText(""));
        return summary;
    }

    private ObjectNode buildChangeRelationSummary(ObjectNode changeRelations) {
        ObjectNode summary = objectMapper.createObjectNode();
        JsonNode recentChange = firstArrayItem(changeRelations.path("recentChanges"));
        summary.put("recentChangeTime", recentChange == null ? "" : recentChange.path("time").asText(""));
        summary.put("recentChangeTitle", recentChange == null ? "" : recentChange.path("change").asText(""));
        summary.put("inference", changeRelations.path("inference").asText(""));
        return summary;
    }

    private ObjectNode buildEquipmentResourceSummary(ObjectNode personnel, ObjectNode inventory) {
        ObjectNode summary = objectMapper.createObjectNode();
        JsonNode firstSpare = firstArrayItem(inventory.path("spareParts"));
        JsonNode firstTool = firstArrayItem(inventory.path("tools"));
        summary.put("recommendedLead", personnel.path("recommendedLead").asText(""));
        summary.put("firstSparePart", firstSpare == null ? "" : firstSpare.path("name").asText(""));
        summary.put("firstSparePartStock", firstSpare == null ? 0 : firstSpare.path("quantity").asInt(0));
        summary.put("firstTool", firstTool == null ? "" : firstTool.path("name").asText(""));
        summary.put("firstToolStock", firstTool == null ? 0 : firstTool.path("quantity").asInt(0));
        return summary;
    }

    private ObjectNode buildDiagnosisSummary(ObjectNode diagnosis, ObjectNode digitalTwin) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("severity", diagnosis.path("severity").asText(""));
        summary.put("summary", diagnosis.path("summary").asText(""));
        summary.put("rootCause", diagnosis.path("rootCause").asText(""));
        summary.put("confidence", diagnosis.path("confidence").asText(""));
        summary.put("bearingModel", digitalTwin.path("bearingModel").asText(""));
        return summary;
    }

    private void appendEquipmentConstraints(ArrayNode knownConstraints, ObjectNode digitalTwin, ObjectNode diagnosis) {
        knownConstraints.add("当前连续运行时长约 " + digitalTwin.path("continuousRunHours").asText("") + " 小时。");
        knownConstraints.add("润滑方式为 " + digitalTwin.path("lubricationMode").asText("") + "，对喷嘴状态较敏感。");
        knownConstraints.add("当前诊断严重度为 " + diagnosis.path("severity").asText("") + "。");
    }

    private ObjectNode buildEquipmentResources(JsonNode personnel, JsonNode inventory) {
        ObjectNode resources = objectMapper.createObjectNode();
        resources.set("recommendedPeople", selectRecommendedCandidates(personnel.path("candidates")));
        resources.set("spareParts", inventory.path("spareParts"));
        resources.set("tools", inventory.path("tools"));
        return resources;
    }

    private ObjectNode buildTelemetrySnapshot(String equipmentId, EquipmentWorkflowRequest request) {
        ObjectNode telemetry = (ObjectNode) equipmentDecisionService.getTelemetry(equipmentId);
        if (request.getCurrentTemperatureC() != null) {
            telemetry.put("currentTemperatureC", request.getCurrentTemperatureC());
        }
        if (request.getVibrationMmPerS() != null) {
            telemetry.put("vibrationMmPerS", request.getVibrationMmPerS());
        }
        if (request.getTemperatureRiseRatePerMin() != null) {
            telemetry.put("temperatureRiseRatePerMin", request.getTemperatureRiseRatePerMin());
        }
        return telemetry;
    }

    private EquipmentDiagnosisRequest toDiagnosisRequest(ObjectNode telemetry) {
        EquipmentDiagnosisRequest diagnosisRequest = new EquipmentDiagnosisRequest();
        diagnosisRequest.setCurrentTemperatureC(telemetry.path("currentTemperatureC").asDouble());
        diagnosisRequest.setVibrationMmPerS(telemetry.path("vibrationMmPerS").asDouble());
        diagnosisRequest.setTemperatureRiseRatePerMin(telemetry.path("temperatureRiseRatePerMin").asDouble());
        return diagnosisRequest;
    }

    private EquipmentDispositionRequest toDispositionRequest(EquipmentWorkflowRequest request) {
        EquipmentDispositionRequest dispositionRequest = new EquipmentDispositionRequest();
        dispositionRequest.setImmediateShutdown(request.getImmediateShutdown());
        dispositionRequest.setReserveBearing(request.getReserveBearing());
        dispositionRequest.setPushWorkOrder(request.getPushWorkOrder());
        return dispositionRequest;
    }

    private boolean isSevere(JsonNode diagnosis, ObjectNode telemetry) {
        return "P0".equalsIgnoreCase(diagnosis.path("severity").asText(""))
                || telemetry.path("currentTemperatureC").asDouble(0.0D) >= 90.0D
                || telemetry.path("vibrationMmPerS").asDouble(0.0D) >= 4.0D;
    }

    private String determineRecommendedOption(boolean severe, String userIntent) {
        if (severe) {
            return "A";
        }
        String normalized = normalize(userIntent);
        if (normalized.contains("连续生产") || normalized.contains("不停机")) {
            return "B";
        }
        return "C";
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

    private JsonNode firstArrayItem(JsonNode node) {
        if (node != null && node.isArray() && node.size() > 0) {
            return node.get(0);
        }
        return null;
    }

    private EquipmentWorkflowRequest ensureRequest(EquipmentWorkflowRequest request) {
        return request == null ? new EquipmentWorkflowRequest() : request;
    }

    private boolean isClarified(EquipmentWorkflowRequest request) {
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
