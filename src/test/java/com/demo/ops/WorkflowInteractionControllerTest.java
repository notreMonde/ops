package com.demo.ops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aircraftRetrievalShouldExposeMissingItems() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft/B-1234/workflow/retrieval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("retrieval"))
                .andExpect(jsonPath("$.data.missingItems[*].field", hasItems("userIntent", "continuousDrip", "repairTarget")));
    }

    @Test
    void aircraftExecutionShouldRecommendMelRetentionWhenConditionsAllow() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft/B-1234/workflow/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leakAreaCm2\":8,\"continuousDrip\":false,\"repairTarget\":\"保留放行\",\"userIntent\":\"尽快放行后再安排窗口期维修\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("execution"))
                .andExpect(jsonPath("$.data.blockingItems.length()").value(0))
                .andExpect(jsonPath("$.data.analysis.recommendedOptionCode").value("B"))
                .andExpect(jsonPath("$.data.options.length()").value(3));
    }

    @Test
    void equipmentExecutionShouldRecommendImmediateShutdownForSevereSignals() throws Exception {
        mockMvc.perform(post("/api/v1/equipment/MOT-2024-A07/workflow/execution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIntent\":\"控制风险，必要时立即停机\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("execution"))
                .andExpect(jsonPath("$.data.analysis.recommendedOptionCode").value("A"))
                .andExpect(jsonPath("$.data.options.length()").value(3));
    }

    @Test
    void equipmentFeedbackShouldBlockUnsafeSelection() throws Exception {
        mockMvc.perform(post("/api/v1/equipment/MOT-2024-A07/workflow/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionCode\":\"B\",\"userIntent\":\"尽量不停机\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("feedback"))
                .andExpect(jsonPath("$.data.executionStatus").value("blocked"));
    }
}
