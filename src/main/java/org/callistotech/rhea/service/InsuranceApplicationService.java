package org.callistotech.rhea.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.callistotech.rhea.model.ApplicationStatus;
import org.callistotech.rhea.model.InsuranceApplication;
import org.callistotech.rhea.model.InsuranceProgram;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.InsuranceApplicationRepository;
import org.callistotech.rhea.repository.InsuranceProgramRepository;
import org.callistotech.rhea.repository.PatientRepository;
import org.callistotech.rhea.tool.ColoradoResourceSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * AI agent that drafts a Colorado public-program application for a patient whose
 * unemployment has just been verified. Drafting alone never enrolls anyone -- the resulting
 * {@link InsuranceApplication} sits at {@link ApplicationStatus#DRAFTED} until the patient
 * explicitly consents ({@link #recordConsent}), and only reaches {@link ApplicationStatus#APPROVED}
 * once pharmacy staff record the state's decision ({@link #recordDecision}).
 */
@Service
public class InsuranceApplicationService {

    private static final String SYSTEM_PROMPT = """
            You are Rhea's insurance-pairing agent. You help unemployed Colorado residents find a
            real, currently-available public health insurance or assistance program to apply for.
            You are only drafting a recommendation -- the patient must separately consent before
            anything is submitted, so do not imply the application has already been filed.

            Rules:
            - Colorado residency and unemployment status are the two governing criteria for every
              recommendation. Only recommend programs available to Colorado residents, and weight
              eligibility toward someone with no current income.
            - Use the searchColoradoPublicResources tool to confirm current eligibility rules or
              enrollment windows before finalizing your answer.
            - Prefer $0 or low-cost programs given the patient is unemployed.
            - Respond with: (1) the single best-fit program, (2) 1-2 backup options, (3) the specific
              next action, (4) source URLs for every claim.
            - Keep the answer under 200 words.
            """;

    private final ChatClient chatClient;
    private final PatientRepository patientRepository;
    private final InsuranceProgramRepository insuranceProgramRepository;
    private final InsuranceApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    public InsuranceApplicationService(ChatClient.Builder chatClientBuilder,
                                        ColoradoResourceSearchTool searchTool,
                                        PatientRepository patientRepository,
                                        InsuranceProgramRepository insuranceProgramRepository,
                                        InsuranceApplicationRepository applicationRepository,
                                        ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(searchTool)
                .build();
        this.patientRepository = patientRepository;
        this.insuranceProgramRepository = insuranceProgramRepository;
        this.applicationRepository = applicationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Streams the agent's draft to the client as raw text chunks over SSE as they arrive.
     * Persists a DRAFTED application once the stream finishes -- nothing is submitted yet.
     */
    public void streamDraft(Long patientId, SseEmitter emitter) {
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
                        InsuranceApplication application = saveDraft(patient, knownPrograms, full.toString());
                        completeStream(emitter, application.getId());
                    })
                    .doOnError(e -> {
                        String fallback = fallbackText(e, knownPrograms);
                        InsuranceApplication application = saveDraft(patient, knownPrograms, fallback);
                        sendChunk(emitter, fallback);
                        completeStream(emitter, application.getId());
                    })
                    .subscribe();
        } catch (Exception e) {
            String fallback = fallbackText(e, knownPrograms);
            InsuranceApplication application = saveDraft(patient, knownPrograms, fallback);
            sendChunk(emitter, fallback);
            completeStream(emitter, application.getId());
        }
    }

    /**
     * The patient's decision on whether to proceed. Agreeing moves the application to
     * SUBMITTED (as if filed with the state); declining moves it to DECLINED and nothing
     * further happens with it.
     */
    @Transactional
    public InsuranceApplication recordConsent(Long applicationId, String programName, String consentText,
                                               boolean agree) {
        InsuranceApplication application = get(applicationId);
        if (application.getStatus() != ApplicationStatus.DRAFTED) {
            throw new IllegalStateException("Application is not awaiting consent (status: "
                    + application.getStatus() + ")");
        }

        if (agree) {
            application.setProgramName(programName);
            application.setConsentText(consentText);
            application.setConsentedAt(Instant.now());
            application.setStatus(ApplicationStatus.SUBMITTED);
            application.setSubmittedAt(Instant.now());
        } else {
            application.setStatus(ApplicationStatus.DECLINED);
        }
        return applicationRepository.save(application);
    }

    /**
     * Records the state program's decision. There is no live state API to poll, so pharmacy
     * staff record this manually once they hear back -- same pattern as the CDLE manual
     * override.
     */
    @Transactional
    public InsuranceApplication recordDecision(Long applicationId, ApplicationStatus status) {
        if (status != ApplicationStatus.APPROVED && status != ApplicationStatus.DENIED) {
            throw new IllegalArgumentException("Decision must be APPROVED or DENIED, got: " + status);
        }
        InsuranceApplication application = get(applicationId);
        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Application is not awaiting a decision (status: "
                    + application.getStatus() + ")");
        }
        application.setStatus(status);
        application.setDecidedAt(Instant.now());
        return applicationRepository.save(application);
    }

    public InsuranceApplication get(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Insurance application not found: " + id));
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
     * Sends an explicit "done" named event (carrying the drafted application's id, so the
     * client can move on to the consent step) before completing. EventSource auto-reconnects
     * after a clean server close otherwise, which would re-trigger the AI call in a loop --
     * the client listens for this event to know it should call source.close() itself.
     */
    private void completeStream(SseEmitter emitter, Long applicationId) {
        try {
            emitter.send(SseEmitter.event().name("done").data(String.valueOf(applicationId)));
            emitter.complete();
        } catch (Exception ignored) {
            // client already disconnected or emitter already completed, nothing more to do
        }
    }

    private String fallbackText(Throwable e, List<InsuranceProgram> knownPrograms) {
        return "AI agent unavailable (" + e.getMessage() + "). "
                + "Set GROQ_API_KEY to enable live recommendations. Known programs on file:\n"
                + knownProgramsBlock(knownPrograms);
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
                Find them a qualifying program to apply for.

                Known Colorado programs (starting point, confirm with live search):
                %s
                """.formatted(patient.getFirstName(), patient.getLastName(), knownProgramsBlock(knownPrograms));
    }

    private InsuranceApplication saveDraft(Patient patient, List<InsuranceProgram> knownPrograms, String content) {
        InsuranceApplication application = new InsuranceApplication();
        application.setPatient(patient);
        application.setAiSummary(content);
        application.setSourceUrls(knownPrograms.stream()
                .map(InsuranceProgram::getOfficialUrl)
                .collect(Collectors.joining(", ")));
        return applicationRepository.save(application);
    }
}
