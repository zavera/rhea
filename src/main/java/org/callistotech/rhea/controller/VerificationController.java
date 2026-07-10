package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.CreateVerificationRequest;
import org.callistotech.rhea.dto.ManualOverrideRequest;
import org.callistotech.rhea.model.UnemploymentVerification;
import org.callistotech.rhea.service.PatientService;
import org.callistotech.rhea.service.UnemploymentVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerificationController {

    private final UnemploymentVerificationService verificationService;
    private final PatientService patientService;

    public VerificationController(UnemploymentVerificationService verificationService,
                                   PatientService patientService) {
        this.verificationService = verificationService;
        this.patientService = patientService;
    }

    @PostMapping("/api/verifications")
    @ResponseStatus(HttpStatus.CREATED)
    public UnemploymentVerification verify(@RequestBody CreateVerificationRequest request) {
        return verificationService.verify(
                patientService.get(request.patientId()),
                request.cdleClaimNumber());
    }

    @PostMapping("/api/verifications/manual-override")
    @ResponseStatus(HttpStatus.CREATED)
    public UnemploymentVerification manualOverride(@RequestBody ManualOverrideRequest request) {
        return verificationService.manualOverride(
                patientService.get(request.patientId()),
                request.managerName(),
                request.managerNotes());
    }
}
