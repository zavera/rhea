package org.callistotech.rhea.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.callistotech.rhea.model.InsuranceMatchRecommendation;
import org.callistotech.rhea.model.InsuranceProgram;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.InsuranceMatchRecommendationRepository;
import org.callistotech.rhea.repository.InsuranceProgramRepository;
import org.callistotech.rhea.repository.PatientRepository;
import org.callistotech.rhea.tool.ColoradoResourceSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * AI agent that finds a qualifying ongoing health insurance program for a
 * patient who has just received a $0 emergency dispense. It grounds its
 * answer in the curated {@link InsuranceProgram} reference list and
 * supplements it with live Tavily search results via
 * {@link ColoradoResourceSearchTool}.
 */
@Service
public class InsuranceMatchAgentService {

    private static final String SYSTEM_PROMPT = """
            You are Rhea's insurance-match agent. You help unemployed Colorado residents who just
            received a $0 emergency prescription find a real, currently-available public health
            insurance or assistance program to enroll in so they have ongoing coverage.

            Rules:
            - Colorado residency and unemployment status are the two governing criteria for every
              recommendation. Only recommend programs available to Colorado residents, and weight
              eligibility toward someone with no current income.
            - Use the searchColoradoPublicResources tool to confirm current eligibility rules or
              enrollment windows before finalizing your answer.
            - Prefer $0 or low-cost programs given the patient is unemployed.
            - Respond with: (1) the single best-fit program, (2) 1-2 backup options, (3) the specific
              next action the patient should take, (4) source URLs for every claim.
            - Keep the answer under 200 words.
            """;

    private final ChatClient chatClient;
    private final PatientRepository patientRepository;
    private final InsuranceProgramRepository insuranceProgramRepository;
    private final InsuranceMatchRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    public InsuranceMatchAgentService(ChatClient.Builder chatClientBuilder,
                                       ColoradoResourceSearchTool searchTool,
                                       PatientRepository patientRepository,
                                       InsuranceProgramRepository insuranceProgramRepository,
                                       InsuranceMatchRecommendationRepository recommendationRepository,
                                       ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(searchTool)
                .build();
        this.patientRepository = patientRepository;
        this.insuranceProgramRepository = insuranceProgramRepository;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
    }

    public InsuranceMatchRecommendation findMatch(Long patientId) {
        Patient patient = requirePatient(patientId);
        List<InsuranceProgram> knownPrograms = insuranceProgramRepository.findAll();
        String userPrompt = buildUserPrompt(patient, knownPrograms);

        String content;
        try {
            content = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            content = "AI agent unavailable (" + e.getMessage() + "). "
                    + "Set GROQ_API_KEY to enable live recommendations. Known programs on file:\n"
                    + knownProgramsBlock(knownPrograms);
        }

        return saveRecommendation(patient, knownPrograms, content);
    }

    /**
     * Streams the agent's answer to the client as raw text chunks over SSE as they arrive,
     * instead of waiting for the full response and returning JSON. Persists the completed
     * recommendation once the stream finishes.
     */
    public void streamMatch(Long patientId, SseEmitter emitter) {
        Patient patient = requirePatient(patientId);
        List<InsuranceProgram> knownPrograms = insuranceProgramRepository.findAll();
        String userPrompt = buildUserPrompt(patient, knownPrograms);

        StringBuilder full = new StringBuilder();
        try {
            chatClient.prompt()
                    .user(userPrompt)
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        full.append(chunk);
                        sendChunk(emitter, chunk);
                    })
                    .doOnComplete(() -> {
                        saveRecommendation(patient, knownPrograms, full.toString());
                        completeStream(emitter);
                    })
                    .doOnError(e -> {
                        String fallback = "AI agent unavailable (" + e.getMessage() + "). "
                                + "Set GROQ_API_KEY to enable live recommendations. Known programs on file:\n"
                                + knownProgramsBlock(knownPrograms);
                        saveRecommendation(patient, knownPrograms, fallback);
                        sendChunk(emitter, fallback);
                        completeStream(emitter);
                    })
                    .subscribe();
        } catch (Exception e) {
            String fallback = "AI agent unavailable (" + e.getMessage() + "). "
                    + "Set GROQ_API_KEY to enable live recommendations. Known programs on file:\n"
                    + knownProgramsBlock(knownPrograms);
            saveRecommendation(patient, knownPrograms, fallback);
            sendChunk(emitter, fallback);
            completeStream(emitter);
        }
    }

    /**
     * Sends a chunk as a JSON-encoded SSE data payload rather than raw text. Raw "data:"
     * lines have exactly one leading space stripped by the SSE spec, which silently eats a
     * chunk's real leading space (LLM token deltas are frequently " word" with a leading
     * space). JSON-encoding sidesteps this since the payload never starts with a space.
     *
     * Serializes with Jackson directly rather than passing MediaType.APPLICATION_JSON to
     * SseEmitter -- Spring's StringHttpMessageConverter also declares wildcard ("*&#47;*")
     * support, so it can win content negotiation over the Jackson converter for a String
     * payload and write it as raw unescaped text regardless of the requested media type.
     */
    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(objectMapper.writeValueAsString(chunk));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * Sends an explicit "done" named event before completing. EventSource auto-reconnects
     * after a clean server close otherwise, which would re-trigger the AI call in a loop --
     * the client listens for this event to know it should call source.close() itself.
     */
    private void completeStream(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data(""));
            emitter.complete();
        } catch (Exception ignored) {
            // client already disconnected or emitter already completed, nothing more to do
        }
    }

    private Patient requirePatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found: " + patientId));
    }

    private String knownProgramsBlock(List<InsuranceProgram> knownPrograms) {
        return knownPrograms.stream()
                .map(p -> "- %s: %s (%s) %s".formatted(
                        p.getName(), p.getDescription(), p.getEligibilitySummary(), p.getOfficialUrl()))
                .collect(Collectors.joining("\n"));
    }

    private String buildUserPrompt(Patient patient, List<InsuranceProgram> knownPrograms) {
        return """
                Patient: %s %s, Colorado resident, unemployment status verified via CDLE claim.
                Find them a qualifying ongoing health insurance / assistance program.

                Known Colorado programs (starting point, confirm with live search):
                %s
                """.formatted(patient.getFirstName(), patient.getLastName(), knownProgramsBlock(knownPrograms));
    }

    private InsuranceMatchRecommendation saveRecommendation(Patient patient, List<InsuranceProgram> knownPrograms,
                                                              String content) {
        InsuranceMatchRecommendation recommendation = new InsuranceMatchRecommendation();
        recommendation.setPatient(patient);
        recommendation.setSummary(content);
        recommendation.setSourceUrls(knownPrograms.stream()
                .map(InsuranceProgram::getOfficialUrl)
                .collect(Collectors.joining(", ")));
        return recommendationRepository.save(recommendation);
    }
}
