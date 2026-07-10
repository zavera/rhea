package org.callistotech.rhea.service;

import org.callistotech.rhea.model.InsuranceMatchRecommendation;
import org.callistotech.rhea.model.InsuranceProgram;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.InsuranceMatchRecommendationRepository;
import org.callistotech.rhea.repository.InsuranceProgramRepository;
import org.callistotech.rhea.repository.PatientRepository;
import org.callistotech.rhea.tool.ColoradoResourceSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

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
            - Only recommend programs available to Colorado residents.
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

    public InsuranceMatchAgentService(ChatClient.Builder chatClientBuilder,
                                       ColoradoResourceSearchTool searchTool,
                                       PatientRepository patientRepository,
                                       InsuranceProgramRepository insuranceProgramRepository,
                                       InsuranceMatchRecommendationRepository recommendationRepository) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(searchTool)
                .build();
        this.patientRepository = patientRepository;
        this.insuranceProgramRepository = insuranceProgramRepository;
        this.recommendationRepository = recommendationRepository;
    }

    public InsuranceMatchRecommendation findMatch(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found: " + patientId));

        List<InsuranceProgram> knownPrograms = insuranceProgramRepository.findAll();
        String knownProgramsBlock = knownPrograms.stream()
                .map(p -> "- %s: %s (%s) %s".formatted(
                        p.getName(), p.getDescription(), p.getEligibilitySummary(), p.getOfficialUrl()))
                .collect(Collectors.joining("\n"));

        String userPrompt = """
                Patient: %s %s, Colorado resident, unemployment status verified via CDLE claim.
                Find them a qualifying ongoing health insurance / assistance program.

                Known Colorado programs (starting point, confirm with live search):
                %s
                """.formatted(patient.getFirstName(), patient.getLastName(), knownProgramsBlock);

        String content;
        try {
            content = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            content = "AI agent unavailable (" + e.getMessage() + "). "
                    + "Set GROQ_API_KEY to enable live recommendations. Known programs on file:\n"
                    + knownProgramsBlock;
        }

        InsuranceMatchRecommendation recommendation = new InsuranceMatchRecommendation();
        recommendation.setPatient(patient);
        recommendation.setSummary(content);
        recommendation.setSourceUrls(knownPrograms.stream()
                .map(InsuranceProgram::getOfficialUrl)
                .collect(Collectors.joining(", ")));
        return recommendationRepository.save(recommendation);
    }
}
