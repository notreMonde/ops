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

/**
 * 飞机维修三阶段互动流程服务。
 * 将事实检索、方案选择与最终反馈拆分为三个独立阶段输出。
 */
@Service
public class AircraftInteractionService {

    private static final String DEFAULT_COMPONENT = "左主起落架减震支柱";
    private static final String DEFAULT_REPAIR_TARGET = "完全修复";

    private final AircraftDecisionService aircraftDecisionService;
    private final ObjectMapper objectMapper;

    public AircraftInteractionService(AircraftDecisionService aircraftDecisionService, ObjectMapper objectMapper) {
        this.aircraftDecisionService = aircraftDecisionService;
        this.objectMapper = objectMapper;
    }

    public JsonNode getRetrieval(String tailNo, AircraftWorkflowRequest request) {
        AircraftWorkflowRequest actualRequest = request == null ? new AircraftWorkflowRequest() : request;
        ObjectNode statusHistory = (ObjectNode) aircraftDecisionService.getStatusHistory(tailNo);
        ObjectNode environment = (ObjectNode) aircraftDecisionService.getEnvironment(tailNo);

        ObjectNode result = baseStage("retrieval");
        ArrayNode confirmedItems = result.putArray("confirmedItems");
        confirmedItems.add(infoItem("机号", tailNo));
        confirmedItems.add(infoItem("机型", statusHistory.path("aircraftModel").asText("未知")));
        confirmedItems.add(infoItem("当前状态", statusHistory.path("flightStatus").asText("未知")));
        confirmedItems.add(infoItem("故障部件", resolveComponent(actualRequest.getComponent())));
        confirmedItems.add(infoItem("当前已知渗漏面积(cm2)", String.valueOf(resolveLeakArea(actualRequest, statusHistory))));

        if (hasText(actualRequest.getUserIntent())) {
            confirmedItems.add(infoItem("用户意图", actualRequest.getUserIntent().trim()));
        }
        if (actualRequest.getContinuousDrip() != null) {
            confirmedItems.add(infoItem("持续滴落", actualRequest.getContinuousDrip() ? "是" : "否"));
        }
        if (hasText(actualRequest.getRepairTarget())) {
            confirmedItems.add(infoItem("维修目标", actualRequest.getRepairTarget().trim()));
        }

        ArrayNode knownConstraints = result.putArray("knownConstraints");
        ObjectNode currentStand = (ObjectNode) environment.path("currentStand");
        if (!currentStand.path("hasCanopy").asBoolean()) {
            knownConstraints.add("当前机位无雨棚，夜间低温环境会抬高现场排故和保留放行复核成本。");
        }
        JsonNode availableHangar = findFirstAvailable(environment.path("hangars"));
        if (availableHangar != null) {
            knownConstraints.add("最近可用机库为 " + availableHangar.path("name").asText()
                    + "，距离当前机位 " + availableHangar.path("distanceMeters").asText() + " 米。");
        }
        if (actualRequest.getContinuousDrip() != null) {
            JsonNode melRelease = aircraftDecisionService.getMelRelease(
                    tailNo,
                    resolveComponent(actualRequest.getComponent()),
                    resolveLeakArea(actualRequest, statusHistory),
                    actualRequest.getContinuousDrip());
            knownConstraints.add(melRelease.path("reason").asText("需要补充 MEL 判断依据。"));
        }

        ArrayNode missingItems = result.putArray("missingItems");
        if (!hasText(actualRequest.getUserIntent())) {
            missingItems.add(missingItem("userIntent", "请补充本次目标，例如尽快放行、窗口期维修或完全修复。", "用于执行阶段确定推荐方案。"));
        }
        if (actualRequest.getContinuousDrip() == null) {
            missingItems.add(missingItem("continuousDrip", "请补充是否存在持续滴落。", "这是 MEL 保留放行判断的阻塞项。"));
        }
        if (!hasText(actualRequest.getRepairTarget())) {
            missingItems.add(missingItem("repairTarget", "请补充维修目标，例如保留放行、临时修复或完全修复。", "用于反馈阶段收敛最终执行方案。"));
        }

        ObjectNode factSnapshot = result.putObject("factSnapshot");
        factSnapshot.set("statusHistory", statusHistory);
        factSnapshot.set("environment", environment);

        result.put("nextStep", missingItems.size() > 0
                ? "请先补充待补充项，再进入执行阶段。"
                : "检索信息已完整，可进入执行阶段。");
        return result;
    }

    public JsonNode getExecution(String tailNo, AircraftWorkflowRequest request) {
        AircraftWorkflowRequest actualRequest = request == null ? new AircraftWorkflowRequest() : request;
        ObjectNode retrieval = (ObjectNode) getRetrieval(tailNo, actualRequest);
        ObjectNode result = baseStage("execution");
        result.set("retrievalContext", summarizeRetrieval(retrieval));

        ArrayNode blockingItems = result.putArray("blockingItems");
        if (actualRequest.getContinuousDrip() == null) {
            blockingItems.add(missingItem("continuousDrip", "执行阶段前需要先确认是否持续滴落。", "否则无法判断保留放行方案是否可执行。"));
            result.put("nextStep", "请先回到检索阶段补充阻塞项。");
            return result;
        }

        ObjectNode statusHistory = (ObjectNode) aircraftDecisionService.getStatusHistory(tailNo);
        double leakArea = resolveLeakArea(actualRequest, statusHistory);
        String repairTarget = resolveRepairTarget(actualRequest.getRepairTarget());
        String userIntent = actualRequest.getUserIntent();

        JsonNode melRelease = aircraftDecisionService.getMelRelease(
                tailNo,
                resolveComponent(actualRequest.getComponent()),
                leakArea,
                actualRequest.getContinuousDrip());
        JsonNode diagnosis = aircraftDecisionService.getDiagnosisConclusion(tailNo, toDiagnosisRequest(actualRequest, leakArea, repairTarget));

        boolean releasable = melRelease.path("releasable").asBoolean(false);
        String recommendedOptionCode = determineRecommendedOption(releasable, userIntent, repairTarget);

        ArrayNode options = result.putArray("options");
        options.add(buildOption(
                "A",
                "机库内完整修复",
                "适用于当前不满足 MEL 条件，或用户目标就是一次性消除渗漏风险的场景。",
                recommendedOptionCode.equals("A"),
                true,
                arrayOf("直接闭环渗漏故障，减少反复排故。", "便于同步安排航材、工装和放行检查。"),
                arrayOf("停场时间更长，需要占用机库资源。", "若现场拖机条件受限，组织成本更高。"),
                arrayOf("需要可用机库位。", "需要封严组合件、液压油与专用工装到位。")));
        options.add(buildOption(
                "B",
                "按 MEL 保留放行并安排窗口期维修",
                "适用于已满足 MEL 条件，且用户更关注短时恢复运行能力的场景。",
                recommendedOptionCode.equals("B"),
                releasable,
                arrayOf("可在规则允许范围内尽快恢复运行。", "能把完整维修转移到计划窗口期执行。"),
                arrayOf("需要持续跟踪渗漏扩大和滴落变化。", "窗口期内若条件恶化，需要立刻切换为完整修复。"),
                arrayOf("必须确认无持续滴落。", "渗漏面积需保持在 MEL 阈值以内。")));
        options.add(buildOption(
                "C",
                "先做现场排查，再决定更换范围",
                "适用于用户还想先缩小故障边界，或准备在执行前再确认一次现场状态的场景。",
                recommendedOptionCode.equals("C"),
                true,
                arrayOf("适合在信息尚不充分时先缩小决策范围。", "可避免直接投入完整拆解资源。"),
                arrayOf("如果最终仍需完整修复，会拉长总体处理时间。", "排查期间仍需控制飞机移动与机位资源。"),
                arrayOf("需要现场观察滴落趋势。", "需要安排至少 1 名授权主修先行排故。")));

        ObjectNode analysis = result.putObject("analysis");
        analysis.put("recommendedOptionCode", recommendedOptionCode);
        analysis.put("melReleasable", releasable);
        analysis.put("diagnosisSummary", diagnosis.path("summary").asText(""));
        analysis.put("severity", diagnosis.path("severity").asText(""));

        result.put("selectionPrompt", "请选择方案编码 A、B 或 C；如果需要，我也可以根据你的偏好调整推荐项。");
        result.put("nextStep", "选择方案后进入反馈阶段，输出最终执行方案。");
        return result;
    }

    public JsonNode getFeedback(String tailNo, AircraftWorkflowRequest request) {
        AircraftWorkflowRequest actualRequest = request == null ? new AircraftWorkflowRequest() : request;
        ObjectNode execution = (ObjectNode) getExecution(tailNo, actualRequest);
        ObjectNode result = baseStage("feedback");

        if (execution.path("blockingItems").size() > 0) {
            result.put("executionStatus", "pending-info");
            result.set("blockingItems", execution.path("blockingItems"));
            result.put("nextStep", "请先补充阻塞项，再请求最终执行方案。");
            return result;
        }

        String selectedOptionCode = normalizeOptionCode(actualRequest.getSelectedOptionCode());
        if (selectedOptionCode == null) {
            result.put("executionStatus", "pending-selection");
            result.set("options", execution.path("options"));
            result.put("nextStep", "请先在执行阶段从 A/B/C 中选择一个方案。");
            return result;
        }

        ObjectNode statusHistory = (ObjectNode) aircraftDecisionService.getStatusHistory(tailNo);
        double leakArea = resolveLeakArea(actualRequest, statusHistory);
        String repairTarget = resolveRepairTarget(actualRequest.getRepairTarget());
        JsonNode melRelease = aircraftDecisionService.getMelRelease(
                tailNo,
                resolveComponent(actualRequest.getComponent()),
                leakArea,
                actualRequest.getContinuousDrip());
        boolean releasable = melRelease.path("releasable").asBoolean(false);

        if ("B".equals(selectedOptionCode) && !releasable) {
            result.put("executionStatus", "blocked");
            result.put("selectedOptionCode", "B");
            result.put("blockingReason", "当前渗漏状态不满足 MEL 保留放行条件，不能收敛为方案 B。");
            result.put("nextStep", "请改选方案 A 或 C。");
            return result;
        }

        ObjectNode finalPlan = result.putObject("finalPlan");
        result.put("executionStatus", "confirmed");
        result.put("selectedOptionCode", selectedOptionCode);

        if ("A".equals(selectedOptionCode)) {
            JsonNode diagnosis = aircraftDecisionService.getDiagnosisConclusion(tailNo, toDiagnosisRequest(actualRequest, leakArea, repairTarget));
            JsonNode disposition = aircraftDecisionService.getDispositionDecision(tailNo, toDispositionRequest(leakArea, actualRequest.getContinuousDrip(), DEFAULT_REPAIR_TARGET));

            finalPlan.put("summary", disposition.path("decisionSummary").asText(""));
            finalPlan.set("executionSteps", disposition.path("planSteps"));
            finalPlan.set("expectedResults", arrayOf(
                    "完成封严组合件更换后，渗漏风险降至可接受范围。",
                    "完成复检后可按标准流程恢复适航状态。"));
            finalPlan.set("cautions", arrayOf(
                    "拖机前确认机位与机库资源已经协调完成。",
                    "拆装与复装过程需同步记录关键扭矩与试验结果。"));
            finalPlan.set("rollbackPlan", arrayOf(
                    "如拆解后发现镜面或内筒损伤超出预期，升级为更大范围部件更换。",
                    "如试验后仍存在异常渗漏，保留停场状态并重新执行根因排查。"));
            finalPlan.set("evidence", diagnosis.path("evidence"));
            finalPlan.set("recommendedPeople", disposition.path("recommendedPeople"));
            finalPlan.set("materials", disposition.path("materials"));
            finalPlan.set("tools", disposition.path("tools"));
            finalPlan.set("locationRecommendation", disposition.path("locationRecommendation"));
            finalPlan.put("nextAction", disposition.path("nextActionPrompt").asText(""));
        } else if ("B".equals(selectedOptionCode)) {
            JsonNode personnel = aircraftDecisionService.getPersonnelMatch(tailNo);
            JsonNode inventory = aircraftDecisionService.getInventory(tailNo);
            JsonNode environment = aircraftDecisionService.getEnvironment(tailNo);
            finalPlan.put("summary", "按 MEL 条件保留放行，并在维修窗口期内完成完整修复。");
            finalPlan.set("executionSteps", arrayOf(
                    "复核渗漏面积、滴落状态和 MEL 条款匹配关系，并形成放行记录。",
                    "在飞行前后安排加密观察，记录渗漏扩大趋势。",
                    "预留可用机库位、航材和工装，锁定 10 天内维修窗口。",
                    "若渗漏面积扩大或转为持续滴落，立即取消保留放行并切换方案 A。"));
            finalPlan.set("expectedResults", arrayOf(
                    "在不突破 MEL 条件的前提下维持短期运行。",
                    "把完整维修安排到更可控的停场窗口期。"));
            finalPlan.set("cautions", arrayOf(
                    "放行依据必须随每次执行状态同步复核。",
                    "任何滴落状态变化都要触发重新评估。"));
            finalPlan.set("rollbackPlan", arrayOf(
                    "一旦出现持续滴落，立刻转为机库内完整修复。",
                    "若可用机库资源失效，重新评估后续航班安排。"));
            finalPlan.set("recommendedPeople", selectRecommendedCandidates(personnel.path("candidates")));
            finalPlan.set("materials", inventory.path("materials"));
            finalPlan.set("tools", inventory.path("tools"));
            finalPlan.set("locationRecommendation", buildMigrateToHangar(environment));
            finalPlan.put("nextAction", "请确认是否采用 MEL 保留放行，并锁定窗口期维修时间。");
        } else {
            JsonNode personnel = aircraftDecisionService.getPersonnelMatch(tailNo);
            JsonNode inventory = aircraftDecisionService.getInventory(tailNo);
            JsonNode environment = aircraftDecisionService.getEnvironment(tailNo);
            finalPlan.put("summary", "先做现场排查和状态复核，再决定是窗口期维修还是直接完整修复。");
            finalPlan.set("executionSteps", arrayOf(
                    "安排授权主修对故障部位做清洁、复测和滴落观察。",
                    "核对历史维修记录、知识库步骤和现场环境限制。",
                    "根据复测结果更新 MEL 判断，并在排查结束后重新进入执行阶段收敛方案。",
                    "如现场已确认不满足保留放行条件，直接转入方案 A。"));
            finalPlan.set("expectedResults", arrayOf(
                    "尽快补齐关键现场信息，避免在假设条件下拍板。",
                    "把后续资源投入范围控制在必要的最小集合。"));
            finalPlan.set("cautions", arrayOf(
                    "排查期间仍需控制飞机移动和机位作业风险。",
                    "如观察到状态恶化，不应继续停留在排查阶段。"));
            finalPlan.set("rollbackPlan", arrayOf(
                    "如排查结果不明确，升级为完整修复处理。",
                    "如观察到持续滴落，取消保留放行路径。"));
            finalPlan.set("recommendedPeople", selectRecommendedCandidates(personnel.path("candidates")));
            finalPlan.set("materials", inventory.path("materials"));
            finalPlan.set("tools", inventory.path("tools"));
            finalPlan.set("locationRecommendation", buildMigrateToHangar(environment));
            finalPlan.put("nextAction", "请确认是否先做现场排查；排查后可再次进入执行阶段更新方案。");
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

    private ObjectNode buildMigrateToHangar(JsonNode environment) {
        ObjectNode node = objectMapper.createObjectNode();
        JsonNode availableHangar = findFirstAvailable(environment.path("hangars"));
        if (availableHangar != null) {
            node.put("name", availableHangar.path("name").asText(""));
            node.put("reason", "可作为后续窗口期维修或状态升级后的首选场地。");
            node.put("distanceMeters", availableHangar.path("distanceMeters").asInt(0));
        }
        return node;
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

    private double resolveLeakArea(AircraftWorkflowRequest request, ObjectNode statusHistory) {
        if (request != null && request.getLeakAreaCm2() != null) {
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

    private String determineRecommendedOption(boolean releasable, String userIntent, String repairTarget) {
        if (releasable && prefersRelease(userIntent, repairTarget)) {
            return "B";
        }
        if (!hasText(userIntent) && !hasText(repairTarget)) {
            return "C";
        }
        return "A";
    }

    private boolean prefersRelease(String userIntent, String repairTarget) {
        String intent = normalize(userIntent);
        String target = normalize(repairTarget);
        return intent.contains("放行")
                || intent.contains("周转")
                || intent.contains("尽快恢复")
                || target.contains("保留放行");
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
