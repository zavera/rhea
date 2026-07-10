package org.callistotech.rhea.service;

import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.model.UnemploymentVerification;
import org.callistotech.rhea.model.VerificationStatus;
import org.callistotech.rhea.repository.UnemploymentVerificationRepository;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Interim stand-in for a real Colorado Department of Labor and Employment
 * (CDLE) claimant lookup, which has no public API today. A claim number is
 * treated as verified when it matches CDLE's published format
 * (CO-YYYY-NNNNNN). Swap this bean out for a real CDLE integration by
 * providing another {@link UnemploymentVerificationService} implementation.
 */
@Service
public class CdleStubVerificationService implements UnemploymentVerificationService {

    private static final Pattern CLAIM_NUMBER_PATTERN = Pattern.compile("^CO-\\d{4}-\\d{6}$");
    private static final String SOURCE = "CDLE-STUB";
    private static final String MANUAL_OVERRIDE_SOURCE = "MANUAL_MANAGER_OVERRIDE";

    private final UnemploymentVerificationRepository repository;

    public CdleStubVerificationService(UnemploymentVerificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public UnemploymentVerification verify(Patient patient, String cdleClaimNumber) {
        UnemploymentVerification verification = new UnemploymentVerification();
        verification.setPatient(patient);
        verification.setCdleClaimNumber(cdleClaimNumber);
        verification.setSource(SOURCE);

        if (cdleClaimNumber != null && CLAIM_NUMBER_PATTERN.matcher(cdleClaimNumber).matches()) {
            verification.setStatus(VerificationStatus.VERIFIED);
            verification.setRawResponse("Active unemployment claim confirmed for claim number "
                    + cdleClaimNumber + " (stub CDLE response -- format CO-YYYY-NNNNNN).");
        } else {
            verification.setStatus(VerificationStatus.NOT_FOUND);
            verification.setRawResponse("No active claim found matching format CO-YYYY-NNNNNN "
                    + "(stub CDLE response).");
        }

        return repository.save(verification);
    }

    @Override
    public UnemploymentVerification manualOverride(Patient patient, String managerName, String managerNotes) {
        if (managerName == null || managerName.isBlank()) {
            throw new IllegalArgumentException("Manager name is required to record a manual override");
        }

        UnemploymentVerification verification = new UnemploymentVerification();
        verification.setPatient(patient);
        verification.setCdleClaimNumber("MANUAL-OVERRIDE");
        verification.setSource(MANUAL_OVERRIDE_SOURCE);
        verification.setStatus(VerificationStatus.VERIFIED);
        verification.setManagerName(managerName);
        verification.setManagerNotes(managerNotes);
        verification.setRawResponse("Manually confirmed by pharmacy manager " + managerName
                + " (automated CDLE lookup bypassed).");

        return repository.save(verification);
    }
}
