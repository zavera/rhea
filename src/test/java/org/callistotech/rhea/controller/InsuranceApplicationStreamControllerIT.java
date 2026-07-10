package org.callistotech.rhea.controller;

import org.callistotech.rhea.model.ApplicationStatus;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.InsuranceApplicationRepository;
import org.callistotech.rhea.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The AI layer is mocked here per the Callisto brand testing rules, same as
 * InsuranceApplicationServiceIT, but exercises the SSE draft endpoint end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InsuranceApplicationStreamControllerIT {

    @MockitoBean
    private ChatModel chatModel;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private InsuranceApplicationRepository applicationRepository;

    @Test
    @WithMockUser(roles = "PHARMACY_STAFF")
    void streamsRawTextChunksAndPersistsADraftedApplication() throws Exception {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                chatResponseChunk("Best fit: "),
                chatResponseChunk("Health First Colorado.")));

        Patient patient = new Patient();
        patient.setFirstName("Jordan");
        patient.setLastName("Reyes");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 14));
        patient.setStateCaseNumber("CASE-STREAM-1");
        patient = patientRepository.save(patient);

        MvcResult mvcResult = mockMvc.perform(get("/api/insurance-applications/stream")
                        .param("patientId", String.valueOf(patient.getId())))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Health First Colorado")))
                .andExpect(content().string(containsString("event:done")));

        assertThat(applicationRepository.findAll())
                .anySatisfy(application -> {
                    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.DRAFTED);
                    assertThat(application.getAiSummary()).contains("Health First Colorado");
                });
    }

    private ChatResponse chatResponseChunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
