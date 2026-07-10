package org.callistotech.rhea.service;

import org.callistotech.rhea.model.ApplicationStatus;
import org.callistotech.rhea.model.InsuranceApplication;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.InsuranceApplicationRepository;
import org.callistotech.rhea.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the consent and decision lifecycle. The AI-drafting step itself (which needs the
 * AI layer mocked per the Callisto brand testing rules) is covered end to end by
 * InsuranceMatchStreamControllerIT -- this test seeds a DRAFTED application directly to
 * exercise what happens after the draft exists.
 */
@SpringBootTest
@ActiveProfiles("test")
class InsuranceApplicationServiceIT {

    @Autowired
    private InsuranceApplicationService insuranceApplicationService;

    @Autowired
    private InsuranceApplicationRepository applicationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void consentingSubmitsTheApplication() {
        InsuranceApplication drafted = draftedApplication("CASE-CONSENT-1");

        InsuranceApplication submitted = insuranceApplicationService.recordConsent(
                drafted.getId(), "Health First Colorado (Medicaid)", "Patient agreed.", true);

        assertThat(submitted.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(submitted.getProgramName()).isEqualTo("Health First Colorado (Medicaid)");
        assertThat(submitted.getConsentedAt()).isNotNull();
        assertThat(submitted.getSubmittedAt()).isNotNull();
    }

    @Test
    void decliningNeverSubmitsAnything() {
        InsuranceApplication drafted = draftedApplication("CASE-CONSENT-2");

        InsuranceApplication declined = insuranceApplicationService.recordConsent(
                drafted.getId(), "Health First Colorado (Medicaid)", "", false);

        assertThat(declined.getStatus()).isEqualTo(ApplicationStatus.DECLINED);
        assertThat(declined.getSubmittedAt()).isNull();
    }

    @Test
    void decisionRequiresPriorConsent() {
        InsuranceApplication drafted = draftedApplication("CASE-DECISION-1");

        assertThatThrownBy(() -> insuranceApplicationService.recordDecision(drafted.getId(), ApplicationStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not awaiting a decision");
    }

    @Test
    void approvedDecisionFollowsConsent() {
        InsuranceApplication drafted = draftedApplication("CASE-DECISION-2");
        insuranceApplicationService.recordConsent(drafted.getId(), "Health First Colorado (Medicaid)", "ok", true);

        InsuranceApplication decided = insuranceApplicationService.recordDecision(drafted.getId(), ApplicationStatus.APPROVED);

        assertThat(decided.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(decided.getDecidedAt()).isNotNull();
    }

    private InsuranceApplication draftedApplication(String caseNumber) {
        Patient patient = new Patient();
        patient.setFirstName("Jordan");
        patient.setLastName("Reyes");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 14));
        patient.setStateCaseNumber(caseNumber);
        patient = patientRepository.save(patient);

        InsuranceApplication application = new InsuranceApplication();
        application.setPatient(patient);
        application.setAiSummary("Best fit: Health First Colorado (Medicaid).");
        application.setSourceUrls("https://www.healthfirstcolorado.com");
        return applicationRepository.save(application);
    }
}
