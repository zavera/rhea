package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.ConsentRequest;
import org.callistotech.rhea.model.ApplicationStatus;
import org.callistotech.rhea.model.CopayAssistanceApplication;
import org.callistotech.rhea.model.CopayAssistanceProgram;
import org.callistotech.rhea.repository.CopayAssistanceProgramRepository;
import org.callistotech.rhea.service.CopayAssistanceService;
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
public class CopayAssistanceController {

    private final CopayAssistanceService copayAssistanceService;
    private final CopayAssistanceProgramRepository copayAssistanceProgramRepository;

    public CopayAssistanceController(CopayAssistanceService copayAssistanceService,
                                      CopayAssistanceProgramRepository copayAssistanceProgramRepository) {
        this.copayAssistanceService = copayAssistanceService;
        this.copayAssistanceProgramRepository = copayAssistanceProgramRepository;
    }

    @GetMapping(value = "/api/copay-assistance-applications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDraft(@RequestParam Long patientId,
                                   @RequestParam(defaultValue = "false") boolean hasFederalCoverage) {
        SseEmitter emitter = new SseEmitter(60_000L);
        copayAssistanceService.streamDraft(patientId, hasFederalCoverage, emitter);
        return emitter;
    }

    @PostMapping("/api/copay-assistance-applications/{id}/consent")
    public CopayAssistanceApplication recordConsent(@PathVariable Long id, @RequestBody ConsentRequest request) {
        return copayAssistanceService.recordConsent(
                id, request.programName(), request.consentText(), request.agree());
    }

    @PostMapping("/api/copay-assistance-applications/{id}/decision")
    public CopayAssistanceApplication recordDecision(@PathVariable Long id, @RequestParam ApplicationStatus status) {
        return copayAssistanceService.recordDecision(id, status);
    }

    @GetMapping("/api/copay-assistance-applications/{id}")
    public CopayAssistanceApplication get(@PathVariable Long id) {
        return copayAssistanceService.get(id);
    }

    @GetMapping("/api/copay-assistance-programs")
    public List<CopayAssistanceProgram> knownPrograms() {
        return copayAssistanceProgramRepository.findAll();
    }
}
