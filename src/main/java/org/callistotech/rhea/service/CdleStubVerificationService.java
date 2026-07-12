package org.callistotech.rhea.service;

import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.model.UnemploymentVerification;
import org.callistotech.rhea.model.VerificationStatus;
import org.callistotech.rhea.repository.UnemploymentVerificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Interim stand-in for a real state labor department claimant lookup -- no single
 * nationwide API exists today, and each state's agency (CDLE in Colorado, EDD in
 * California, TWC in Texas, etc.) has its own. A claim number is treated as verified
 * when it matches the generic state-prefixed format used across this demo
 * (XX-YYYY-NNNNNN, where XX is the two-letter state code). Swap this bean out for
 * real per-state integrations by providing another {@link UnemploymentVerificationService}
 * implementation.
 */
@Service
public class CdleStubVerificationService implements UnemploymentVerificationService {

    private static final Pattern CLAIM_NUMBER_PATTERN = Pattern.compile("^[A-Z]{2}-\\d{4}-\\d{6}$");
    private static final String SOURCE = "STATE-LABOR-DEPT-STUB";
    private static final String MANUAL_OVERRIDE_SOURCE = "MANUAL_MANAGER_OVERRIDE";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    // Fixed placeholder amount for demo purposes; a real CDLE integration would return the
    // claimant's actual computed weekly benefit amount, not a constant.
    private static final long DEMO_WEEKLY_BENEFIT_AMOUNT_CENTS = 45000L;

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
            LocalDate startDate = verification.getVerifiedAt()
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDate()
                    .minus(8, ChronoUnit.WEEKS);
            verification.setStatus(VerificationStatus.VERIFIED);
            verification.setUnemploymentStartDate(startDate);
            verification.setWeeklyBenefitAmountCents(DEMO_WEEKLY_BENEFIT_AMOUNT_CENTS);
            verification.setRawResponse(("Active unemployment insurance claim %s confirmed for %s %s. "
                    + "Benefits began %s. Weekly benefit amount: $%.2f. Verified at %s.").formatted(
                    cdleClaimNumber, patient.getFirstName(), patient.getLastName(),
                    startDate.format(DISPLAY_DATE),
                    DEMO_WEEKLY_BENEFIT_AMOUNT_CENTS / 100.0,
                    verification.getVerifiedAt()));
        } else {
            verification.setStatus(VerificationStatus.NOT_FOUND);
            verification.setRawResponse("No active unemployment insurance claim found matching state "
                    + "case number format XX-YYYY-NNNNNN. Verified at " + verification.getVerifiedAt() + ".");
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
