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

/**
 * 设备维修三阶段互动流程服务。
 * 将事实检索、方案选择与最终反馈拆分为三个独立阶段输出。
 */
@Service
public class EquipmentInteractionService {

    private final EquipmentDecisionService equipmentDecisionService;
    private final ObjectMapper objectMapper;

    public EquipmentInteractionService(EquipmentDecisionService equipmentDecisionService, ObjectMapper objectMapper) {
        this.equipmentDecisionService = equipmentDecisionService;
        this.objectMapper = objectMapper;
    }

    public JsonNode getRetrieval(String equipmentId, EquipmentWorkflowRequest request) {
        EquipmentWorkflowRequest actualRequest = request == null ? new EquipmentWorkflowRequest() : request;
        ObjectNode digitalTwin = (ObjectNode) equipmentDecisionService.getDigitalTwin(equipmentId);
        ObjectNode telemetry = buildTelemetrySnapshot(equipmentId, actualRequest);
        ObjectNode diagnosis = (ObjectNode) equipmentDecisionService.getDiagnosisConclusion(equipmentId, toDiagnosisRequest(actualRequest, telemetry));

        ObjectNode result = baseStage("retrieval");
        ArrayNode confirmedItems = result.putArray("confirmedItems");
        confirmedItems.add(infoItem("设备编号", equipmentId));
        confirmedItems.add(infoItem("设备名称", digitalTwin.path("equipmentName").asText("未知")));
        confirmedItems.add(infoItem("型号", digitalTwin.path("model").asText("未知")));
        confirmedItems.add(infoItem("轴承型号", digitalTwin.path("bearingModel").asText("未知")));
        confirmedItems.add(infoItem("当前温度(C)", telemetry.path("currentTemperatureC").asText("")));
        confirmedItems.add(infoItem("振动(mm/s)", telemetry.path("vibrationMmPerS").asText("")));
        confirmedItems.add(infoItem("温升速率(C/min)", telemetry.path("temperatureRiseRatePerMin").asText("")));
        if (hasText(actualRequest.getUserIntent())) {
            confirmedItems.add(infoItem("用户意图", actualRequest.getUserIntent().trim()));
        }

        ArrayNode knownConstraints = result.putArray("knownConstraints");
        knownConstraints.add("当前连续运行时长约 " + digitalTwin.path("continuousRunHours").asText("未知") + " 小时。");
        knownConstraints.add("润滑方式为 " + digitalTwin.path("lubricationMode").asText("未知") + "，对喷嘴状态较敏感。");
        knownConstraints.add("诊断严重度为 " + diagnosis.path("severity").asText("P1") + "。");

        ArrayNode missingItems = result.putArray("missingItems");
        if (!hasText(actualRequest.getUserIntent())) {
            missingItems.add(missingItem("userIntent", "请补充本次目标，例如保障连续生产、计划停机处理或立即止损。", "用于执行阶段确定推荐方案。"));
        }

        ObjectNode factSnapshot = result.putObject("factSnapshot");
        factSnapshot.set("digitalTwin", digitalTwin);
        factSnapshot.set("telemetry", telemetry);
        factSnapshot.set("historicalCases", equipmentDecisionService.getHistoricalCases(equipmentId));
        factSnapshot.set("changeRelations", equipmentDecisionService.getChangeRelations(equipmentId));
        factSnapshot.set("diagnosis", diagnosis);

        result.put("nextStep", missingItems.size() > 0
                ? "如需更精准推荐，请先补充用户意图；否则也可以直接进入执行阶段。"
                : "检索信息已完整，可进入执行阶段。");
        return result;
    }

    public JsonNode getExecution(String equipmentId, EquipmentWorkflowRequest request) {
        EquipmentWorkflowRequest actualRequest = request == null ? new EquipmentWorkflowRequest() : request;
        ObjectNode telemetry = buildTelemetrySnapshot(equipmentId, actualRequest);
        ObjectNode diagnosis = (ObjectNode) equipmentDecisionService.getDiagnosisConclusion(equipmentId, toDiagnosisRequest(actualRequest, telemetry));

        boolean severe = isSevere(diagnosis, telemetry);
        String recommendedOptionCode = determineRecommendedOption(severe, actualRequest.getUserIntent());

        ObjectNode result = baseStage("execution");
        result.set("retrievalContext", summarizeRetrieval((ObjectNode) getRetrieval(equipmentId, actualRequest)));
        ArrayNode options = result.putArray("options");
        options.add(buildOption(
                "A",
                "立即停机并拆检润滑与轴承系统",
                "适用于高温、高振动或已判断为 P0 的紧急场景。",
                recommendedOptionCode.equals("A"),
                true,
                arrayOf("最快切断故障继续扩大的风险。", "便于一次性完成喷嘴、轴承和润滑链路检查。"),
                arrayOf("会立即影响当前产能。", "需要组织停机审批与维修资源。"),
                arrayOf("需要停机窗口立即可用。", "需要备件、加热器和液压拉马到位。")));
        options.add(buildOption(
                "B",
                "降载运行并加密监测",
                "适用于趋势可控、严重度不高且用户更关注连续生产的场景。",
                recommendedOptionCode.equals("B"),
                !severe,
                arrayOf("对产能影响最小。", "能为计划停机争取准备时间。"),
                arrayOf("一旦趋势判断失误，可能导致故障放大。", "需要严格执行阈值监测和升级机制。"),
                arrayOf("仅适用于非 P0 场景。", "需要落实每班次监测记录。")));
        options.add(buildOption(
                "C",
                "锁定最近停机窗口做计划性检修",
                "适用于希望平衡风险和产能、并提前准备人员备件的场景。",
                recommendedOptionCode.equals("C"),
                true,
                arrayOf("可提前协调人员、备件和审批。", "能减少临停造成的组织波动。"),
                arrayOf("如果设备状态继续恶化，仍需临时切换方案 A。", "窗口期间需要持续守住预警阈值。"),
                arrayOf("需要明确最近可用停机窗口。", "需要提前预留轴承与喷嘴备件。")));

        ObjectNode analysis = result.putObject("analysis");
        analysis.put("recommendedOptionCode", recommendedOptionCode);
        analysis.put("severity", diagnosis.path("severity").asText(""));
        analysis.put("diagnosisSummary", diagnosis.path("summary").asText(""));

        result.put("selectionPrompt", "请选择方案编码 A、B 或 C；如果你更关心产能或风险，我可以再调整推荐项。");
        result.put("nextStep", "选择方案后进入反馈阶段，输出最终执行方案。");
        return result;
    }

    public JsonNode getFeedback(String equipmentId, EquipmentWorkflowRequest request) {
        EquipmentWorkflowRequest actualRequest = request == null ? new EquipmentWorkflowRequest() : request;
        ObjectNode telemetry = buildTelemetrySnapshot(equipmentId, actualRequest);
        ObjectNode diagnosis = (ObjectNode) equipmentDecisionService.getDiagnosisConclusion(equipmentId, toDiagnosisRequest(actualRequest, telemetry));
        boolean severe = isSevere(diagnosis, telemetry);

        ObjectNode result = baseStage("feedback");
        String selectedOptionCode = normalizeOptionCode(actualRequest.getSelectedOptionCode());
        if (selectedOptionCode == null) {
            result.put("executionStatus", "pending-selection");
            result.set("options", getExecution(equipmentId, actualRequest).path("options"));
            result.put("nextStep", "请先在执行阶段从 A/B/C 中选择一个方案。");
            return result;
        }
        if ("B".equals(selectedOptionCode) && severe) {
            result.put("executionStatus", "blocked");
            result.put("selectedOptionCode", "B");
            result.put("blockingReason", "当前温度/振动趋势已达到高风险水平，不建议继续运行。");
            result.put("nextStep", "请改选方案 A，或退一步选择方案 C 并立即锁定停机窗口。");
            return result;
        }

        ObjectNode finalPlan = result.putObject("finalPlan");
        result.put("executionStatus", "confirmed");
        result.put("selectedOptionCode", selectedOptionCode);

        if ("A".equals(selectedOptionCode)) {
            JsonNode disposition = equipmentDecisionService.getDispositionDecision(equipmentId, toDispositionRequest(actualRequest));
            finalPlan.put("summary", disposition.path("decisionSummary").asText(""));
            finalPlan.set("executionSteps", disposition.path("planSteps"));
            finalPlan.set("expectedResults", arrayOf(
                    "快速隔离故障扩散风险，完成喷嘴和轴承状态确认。",
                    "停机检修后恢复到可控运行状态。"));
            finalPlan.set("cautions", arrayOf(
                    "停机后先控温，再执行手动盘车与拆检。",
                    "更换轴承前需要同步复核润滑系统状态。"));
            finalPlan.set("rollbackPlan", arrayOf(
                    "若喷嘴清理后问题仍在，升级为轴承更换。",
                    "若更换轴承后振动仍异常，继续排查主轴对中与润滑回路。"));
            finalPlan.set("recommendedPeople", disposition.path("recommendedPeople"));
            finalPlan.set("materials", disposition.path("materials"));
            finalPlan.set("tools", disposition.path("tools"));
            finalPlan.put("workOrderDraft", disposition.path("workOrderDraft").asText(""));
            finalPlan.put("nextAction", disposition.path("nextActionPrompt").asText(""));
        } else if ("B".equals(selectedOptionCode)) {
            JsonNode personnel = equipmentDecisionService.getPersonnelMatch(equipmentId);
            JsonNode inventory = equipmentDecisionService.getInventory(equipmentId);
            finalPlan.put("summary", "在受控降载前提下维持生产，并执行班次级趋势监测。");
            finalPlan.set("executionSteps", arrayOf(
                    "将主轴负载降到保守区间，并限制高频重载工况。",
                    "每班次记录温度、振动和温升速率，超过阈值立刻停机。",
                    "提前预留轴承和润滑喷嘴，准备随时切换停机检修。",
                    "在最近计划窗口内复核趋势并重新进入执行阶段确认最终方案。"));
            finalPlan.set("expectedResults", arrayOf(
                    "在风险可控的前提下争取短时间连续生产。",
                    "为计划检修争取人员和备件准备时间。"));
            finalPlan.set("cautions", arrayOf(
                    "任何高温高振动抬升都要立即触发停机。",
                    "此方案不适用于新的异常声响或卡滞迹象。"));
            finalPlan.set("rollbackPlan", arrayOf(
                    "若任一监测指标恶化，立即切换方案 A。",
                    "若停机窗口提前释放，直接升级为方案 C 或 A。"));
            finalPlan.set("recommendedPeople", selectRecommendedCandidates(personnel.path("candidates")));
            finalPlan.set("materials", inventory.path("spareParts"));
            finalPlan.set("tools", inventory.path("tools"));
            finalPlan.put("nextAction", "请确认是否接受降载运行，并指定监测责任人与阈值。");
        } else {
            JsonNode personnel = equipmentDecisionService.getPersonnelMatch(equipmentId);
            JsonNode inventory = equipmentDecisionService.getInventory(equipmentId);
            finalPlan.put("summary", "锁定最近停机窗口，在不停产与风险控制之间做计划性检修。");
            finalPlan.set("executionSteps", arrayOf(
                    "确认最近可用停机窗口，并提前推送检修准备任务。",
                    "预留轴承、润滑喷嘴和专用工装，组织主修与电气配合。",
                    "窗口开始后执行喷嘴检查、润滑链路复核，并按结果决定是否更换轴承。",
                    "检修完成后试车复核，再根据结果决定是否恢复标准工况。"));
            finalPlan.set("expectedResults", arrayOf(
                    "减少临时停机对产能的冲击。",
                    "在计划窗口内完成高概率根因的处理。"));
            finalPlan.set("cautions", arrayOf(
                    "窗口到来前仍需持续监测趋势。",
                    "若状态升级为 P0，不应继续等待计划窗口。"));
            finalPlan.set("rollbackPlan", arrayOf(
                    "若窗口前状态恶化，立即切换方案 A。",
                    "若窗口内排查结果不理想，直接执行轴承更换。"));
            finalPlan.set("recommendedPeople", selectRecommendedCandidates(personnel.path("candidates")));
            finalPlan.set("materials", inventory.path("spareParts"));
            finalPlan.set("tools", inventory.path("tools"));
            finalPlan.put("nextAction", "请确认最近可用停机窗口，我会基于该窗口继续细化执行安排。");
        }
        return result;
    }

    private ObjectNode baseStage(String stage) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("stage", stage);
        node.put("interactionMode", "three-phase");
        return node;
    }

    private ObjectNode infoItem(String label, String value) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("label", label);
        node.put("value", value);
        return node;
    }

    private ObjectNode missingItem(String field, String prompt, String reason) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("field", field);
        node.put("prompt", prompt);
        node.put("reason", reason);
        return node;
    }

    private ObjectNode summarizeRetrieval(ObjectNode retrieval) {
        ObjectNode node = objectMapper.createObjectNode();
        node.set("confirmedItems", retrieval.path("confirmedItems"));
        node.set("missingItems", retrieval.path("missingItems"));
        node.set("knownConstraints", retrieval.path("knownConstraints"));
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

    private EquipmentDiagnosisRequest toDiagnosisRequest(EquipmentWorkflowRequest request, ObjectNode telemetry) {
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
        if (normalize(userIntent).contains("连续生产") || normalize(userIntent).contains("不停机")) {
            return "B";
        }
        return "C";
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
