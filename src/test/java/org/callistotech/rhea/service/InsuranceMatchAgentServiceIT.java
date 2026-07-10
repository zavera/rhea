package org.callistotech.rhea.service;

import org.callistotech.rhea.model.InsuranceMatchRecommendation;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The AI layer is mocked here per the Callisto brand testing rules -- no real
 * LLM API calls in tests. Confirms Rhea's agent service wires the ChatClient,
 * runs the prompt, and persists the recommendation.
 */
@SpringBootTest
@ActiveProfiles("test")
class InsuranceMatchAgentServiceIT {

    @MockitoBean
    private ChatModel chatModel;

    @Autowired
    private InsuranceMatchAgentService insuranceMatchAgentService;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void findMatchPersistsMockedAgentRecommendation() {
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(
                List.of(new Generation(new AssistantMessage(
                        "Best fit: Health First Colorado (Medicaid). Source: https://www.healthfirstcolorado.com")))));

        Patient patient = new Patient();
        patient.setFirstName("Jordan");
        patient.setLastName("Reyes");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 14));
        patient.setStateCaseNumber("CASE-MATCH-1");
        patient = patientRepository.save(patient);

        InsuranceMatchRecommendation recommendation = insuranceMatchAgentService.findMatch(patient.getId());

        assertThat(recommendation.getId()).isNotNull();
        assertThat(recommendation.getSummary()).contains("Health First Colorado");
        assertThat(recommendation.getPatient().getId()).isEqualTo(patient.getId());
    }
}
