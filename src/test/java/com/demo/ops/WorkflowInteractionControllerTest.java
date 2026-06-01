package com.demo.ops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aircraftClarifyShouldReturnCompactBasicInfo() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft/B-1234/workflow/clarify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("clarify"))
                .andExpect(jsonPath("$.data.basicInfo.tailNo").value("B-1234"))
                .andExpect(jsonPath("$.data.basicInfo.aircraftModel").exists())
                .andExpect(jsonPath("$.data.basicInfo.flightStatus").exists())
                .andExpect(jsonPath("$.data.canProceedToRetrieval").value(false));
    }

    @Test
    void aircraftRetrievalShouldBlockWithoutClarifyConfirmation() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft/B-1234/workflow/retrieval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("retrieval"))
                .andExpect(jsonPath("$.data.retrievalStatus").value("blocked"))
                .andExpect(jsonPath("$.data.canProceedToExecution").value(false));
    }

    @Test
    void aircraftRetrievalShouldReturnSummaryAndFullDetailsAfterClarify() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft/B-1234/workflow/retrieval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("retrieval"))
                .andExpect(jsonPath("$.data.retrievedInfo.statusHistorySummary").exists())
                .andExpect(jsonPath("$.data.retrievedInfo.melSummary").exists())
                .andExpect(jsonPath("$.data.retrievedInfo.environmentSummary").exists())
                .andExpect(jsonPath("$.data.retrievedInfo.resourceSummary").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.statusHistory").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.melAssessment").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.environment").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.troubleshootingKnowledge").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.knowledgeGraph").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.personnelMatch").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.inventory").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.diagnosisConclusion").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.melAssessment.continuousDripKnown").value(false))
                .andExpect(jsonPath("$.data.canProceedToExecution").value(true));
    }

    @Test
    void aircraftExecutionShouldReturnOptionsAfterClarify() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft/B-1234/workflow/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true,\"continuousDrip\":false,\"repairTarget\":\"保留放行\",\"userIntent\":\"尽快恢复运行\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("execution"))
                .andExpect(jsonPath("$.data.options.length()").value(3))
                .andExpect(jsonPath("$.data.analysis.recommendedOptionCode").exists());
    }

    @Test
    void equipmentRetrievalShouldReturnSummaryAndFullDetailsAfterClarify() throws Exception {
        mockMvc.perform(post("/api/v1/equipment/MOT-2024-A07/workflow/retrieval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("retrieval"))
                .andExpect(jsonPath("$.data.retrievedInfo.telemetrySummary").exists())
                .andExpect(jsonPath("$.data.retrievedInfo.historicalCaseSummary").exists())
                .andExpect(jsonPath("$.data.retrievedInfo.changeRelationSummary").exists())
                .andExpect(jsonPath("$.data.retrievedInfo.resourceSummary").exists())
                .andExpect(jsonPath("$.data.retrievedInfo.diagnosisSummary").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.digitalTwin").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.telemetry").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.historicalCases").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.changeRelations").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.personnelMatch").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.inventory").exists())
                .andExpect(jsonPath("$.data.retrievedDetails.diagnosisConclusion").exists());
    }

    @Test
    void equipmentFeedbackShouldReturnReceiptAfterSelection() throws Exception {
        mockMvc.perform(post("/api/v1/equipment/MOT-2024-A07/workflow/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basicInfoConfirmed\":true,\"followUpAcknowledged\":true,\"selectedOptionCode\":\"A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("feedback"))
                .andExpect(jsonPath("$.data.executionStatus").value("confirmed"))
                .andExpect(jsonPath("$.data.selectedOptionCode").value("A"))
                .andExpect(jsonPath("$.data.finalPlan.summary").exists())
                .andExpect(jsonPath("$.data.nextAction").exists());
    }
}
