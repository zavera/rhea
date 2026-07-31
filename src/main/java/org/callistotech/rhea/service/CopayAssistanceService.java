package org.callistotech.rhea.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.callistotech.rhea.model.ApplicationStatus;
import org.callistotech.rhea.model.CopayAssistanceApplication;
import org.callistotech.rhea.model.CopayAssistanceProgram;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.CopayAssistanceApplicationRepository;
import org.callistotech.rhea.repository.CopayAssistanceProgramRepository;
import org.callistotech.rhea.repository.PatientRepository;
import org.callistotech.rhea.tool.CopayAssistanceSearchTool;
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
 * AI agent that drafts a copay assistance match for a patient with a high out-of-pocket
 * prescription cost -- the adjacent, commercially-insured counterpart to
 * {@link InsuranceApplicationService}'s unemployment-verified state-program pairing. Same
 * consent-gated lifecycle: drafting never enrolls anyone, the resulting
 * {@link CopayAssistanceApplication} sits at {@link ApplicationStatus#DRAFTED} until the
 * patient explicitly consents.
 *
 * Federal anti-kickback law (Social Security Act Section 1128B(b)) bars manufacturer copay
 * cards for Medicare/Medicaid patients. That's enforced here in Java by filtering
 * {@code hasFederalCoverage} patients down to {@code federalProgramSafe} programs before the
 * AI ever sees the list -- not left to the model to remember from prompt instructions alone.
 */
@Service
public class CopayAssistanceService {

    private static final String SYSTEM_PROMPT = """
            You are Rhea's copay-assistance agent. You help patients with a high out-of-pocket
            prescription cost find a real, currently-available copay assistance program to apply
            for. You are only drafting a recommendation -- the patient must separately consent
            before anything is submitted, so do not imply the application has already been filed.

            Rules:
            - Only recommend programs from the eligible-programs list provided in the prompt --
              that list has already been filtered for the patient's Medicare/Medicaid status where
              required by federal anti-kickback law. Do not recommend a program outside that list.
            - Use the searchCopayAssistancePrograms tool to confirm current eligibility rules or
              fund availability before finalizing your answer.
            - Prefer $0 or low out-of-pocket programs given the patient's cost burden.
            - Respond with: (1) the single best-fit program, (2) 1-2 backup options, (3) the specific
              next action, (4) source URLs for every claim.
            - Keep the answer under 200 words.
            """;

    private final ChatClient chatClient;
    private final PatientRepository patientRepository;
    private final CopayAssistanceProgramRepository programRepository;
    private final CopayAssistanceApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    public CopayAssistanceService(ChatClient.Builder chatClientBuilder,
                                   CopayAssistanceSearchTool searchTool,
                                   PatientRepository patientRepository,
                                   CopayAssistanceProgramRepository programRepository,
                                   CopayAssistanceApplicationRepository applicationRepository,
                                   ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(searchTool)
                .build();
        this.patientRepository = patientRepository;
        this.programRepository = programRepository;
        this.applicationRepository = applicationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Streams the agent's draft to the client as raw text chunks over SSE as they arrive.
     * Persists a DRAFTED application once the stream finishes -- nothing is submitted yet.
     *
     * @param hasFederalCoverage whether the patient has Medicare/Medicaid; when true, the
     *                           program list is filtered to {@code federalProgramSafe} entries
     *                           only, before the AI ever sees it.
     */
    public void streamDraft(Long patientId, boolean hasFederalCoverage, SseEmitter emitter) {
        Patient patient = requirePatient(patientId);
        List<CopayAssistanceProgram> eligiblePrograms = programRepository.findAll().stream()
                .filter(p -> !hasFederalCoverage || p.isFederalProgramSafe())
                .toList();
        String userPrompt = buildUserPrompt(patient, hasFederalCoverage, eligiblePrograms);

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
                        CopayAssistanceApplication application = saveDraft(patient, eligiblePrograms, full.toString());
                        completeStream(emitter, application.getId());
                    })
                    .doOnError(e -> {
                        String fallback = fallbackText(e, eligiblePrograms);
                        CopayAssistanceApplication application = saveDraft(patient, eligiblePrograms, fallback);
                        sendChunk(emitter, fallback);
                        completeStream(emitter, application.getId());
                    })
                    .subscribe();
        } catch (Exception e) {
            String fallback = fallbackText(e, eligiblePrograms);
            CopayAssistanceApplication application = saveDraft(patient, eligiblePrograms, fallback);
            sendChunk(emitter, fallback);
            completeStream(emitter, application.getId());
        }
    }

    /**
     * The patient's decision on whether to proceed. Agreeing moves the application to
     * SUBMITTED; declining moves it to DECLINED and nothing further happens with it.
     */
    @Transactional
    public CopayAssistanceApplication recordConsent(Long applicationId, String programName, String consentText,
                                                      boolean agree) {
        CopayAssistanceApplication application = get(applicationId);
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
     * Records the program's decision. There is no live API to poll, so pharmacy staff record
     * this manually once they hear back -- same pattern as {@link InsuranceApplicationService}.
     */
    @Transactional
    public CopayAssistanceApplication recordDecision(Long applicationId, ApplicationStatus status) {
        if (status != ApplicationStatus.APPROVED && status != ApplicationStatus.DENIED) {
            throw new IllegalArgumentException("Decision must be APPROVED or DENIED, got: " + status);
        }
        CopayAssistanceApplication application = get(applicationId);
        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Application is not awaiting a decision (status: "
                    + application.getStatus() + ")");
        }
        application.setStatus(status);
        application.setDecidedAt(Instant.now());
        return applicationRepository.save(application);
    }

    public CopayAssistanceApplication get(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Copay assistance application not found: " + id));
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(objectMapper.writeValueAsString(chunk));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void completeStream(SseEmitter emitter, Long applicationId) {
        try {
            emitter.send(SseEmitter.event().name("done").data(String.valueOf(applicationId)));
            emitter.complete();
        } catch (Exception ignored) {
            // client already disconnected or emitter already completed, nothing more to do
        }
    }

    private String fallbackText(Throwable e, List<CopayAssistanceProgram> eligiblePrograms) {
        return "AI agent unavailable (" + e.getMessage() + "). "
                + "Set GROQ_API_KEY to enable live recommendations. Known programs on file:\n"
                + knownProgramsBlock(eligiblePrograms);
    }

    private Patient requirePatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found: " + patientId));
    }

    private String knownProgramsBlock(List<CopayAssistanceProgram> eligiblePrograms) {
        return eligiblePrograms.stream()
                .map(p -> "- %s [%s]: %s (%s) %s".formatted(
                        p.getName(), p.getProgramType(), p.getDescription(), p.getEligibilitySummary(), p.getOfficialUrl()))
                .collect(Collectors.joining("\n"));
    }

    private String buildUserPrompt(Patient patient, boolean hasFederalCoverage, List<CopayAssistanceProgram> eligiblePrograms) {
        return """
                Patient: %s %s, has a high out-of-pocket prescription cost.
                Medicare/Medicaid coverage: %s
                Find them a qualifying copay assistance program to apply for.

                Eligible programs (already filtered for this patient's federal-coverage status --
                only recommend from this list, confirm details with live search):
                %s
                """.formatted(patient.getFirstName(), patient.getLastName(),
                hasFederalCoverage ? "yes" : "no", knownProgramsBlock(eligiblePrograms));
    }

    private CopayAssistanceApplication saveDraft(Patient patient, List<CopayAssistanceProgram> eligiblePrograms, String content) {
        CopayAssistanceApplication application = new CopayAssistanceApplication();
        application.setPatient(patient);
        application.setAiSummary(content);
        application.setSourceUrls(eligiblePrograms.stream()
                .map(CopayAssistanceProgram::getOfficialUrl)
                .collect(Collectors.joining(", ")));
        return applicationRepository.save(application);
    }
}
