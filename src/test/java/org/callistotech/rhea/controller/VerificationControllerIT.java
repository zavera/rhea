package org.callistotech.rhea.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VerificationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "PHARMACY_STAFF")
    void managerCanManuallyOverrideVerification() throws Exception {
        Long patientId = createPatient("CASE-OVERRIDE-1");

        String body = """
                {"patientId":%d,"managerName":"Alex Kim","managerNotes":"Confirmed via printed CDLE letter"}
                """.formatted(patientId);

        mockMvc.perform(post("/api/verifications/manual-override")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.source").value("MANUAL_MANAGER_OVERRIDE"))
                .andExpect(jsonPath("$.managerName").value("Alex Kim"));
    }

    @Test
    @WithMockUser(roles = "PHARMACY_STAFF")
    void managerOverrideRequiresManagerName() throws Exception {
        Long patientId = createPatient("CASE-OVERRIDE-2");

        String body = """
                {"patientId":%d,"managerName":"","managerNotes":"no name given"}
                """.formatted(patientId);

        mockMvc.perform(post("/api/verifications/manual-override")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    private Long createPatient(String caseNumber) throws Exception {
        String body = """
                {"firstName":"Jordan","lastName":"Reyes","dateOfBirth":"1990-05-14","stateCaseNumber":"%s"}
                """.formatted(caseNumber);

        String response = mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return Long.valueOf(response.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}
