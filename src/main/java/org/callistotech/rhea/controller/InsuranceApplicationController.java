package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.ConsentRequest;
import org.callistotech.rhea.model.ApplicationStatus;
import org.callistotech.rhea.model.InsuranceApplication;
import org.callistotech.rhea.model.InsuranceProgram;
import org.callistotech.rhea.repository.InsuranceProgramRepository;
import org.callistotech.rhea.service.InsuranceApplicationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class InsuranceApplicationController {

    private final InsuranceApplicationService insuranceApplicationService;
    private final InsuranceProgramRepository insuranceProgramRepository;

    public InsuranceApplicationController(InsuranceApplicationService insuranceApplicationService,
                                           InsuranceProgramRepository insuranceProgramRepository) {
        this.insuranceApplicationService = insuranceApplicationService;
        this.insuranceProgramRepository = insuranceProgramRepository;
    }

    @GetMapping(value = "/api/insurance-applications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDraft(@RequestParam Long patientId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        insuranceApplicationService.streamDraft(patientId, emitter);
        return emitter;
    }

    @PostMapping("/api/insurance-applications/{id}/consent")
    public InsuranceApplication recordConsent(@PathVariable Long id, @RequestBody ConsentRequest request) {
        return insuranceApplicationService.recordConsent(
                id, request.programName(), request.consentText(), request.agree());
    }

    @PostMapping("/api/insurance-applications/{id}/decision")
    public InsuranceApplication recordDecision(@PathVariable Long id, @RequestParam ApplicationStatus status) {
        return insuranceApplicationService.recordDecision(id, status);
    }

    @GetMapping("/api/insurance-applications/{id}")
    public InsuranceApplication get(@PathVariable Long id) {
        return insuranceApplicationService.get(id);
    }

    @GetMapping("/api/insurance-programs")
    public List<InsuranceProgram> knownPrograms() {
        return insuranceProgramRepository.findAll();
    }
}
