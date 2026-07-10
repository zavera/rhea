package org.callistotech.rhea.service;

import org.callistotech.rhea.dto.CreatePatientRequest;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEMO_RESET_DUPLICATES=true (used by the local demo launch config) should let repeated
 * "Create patient" clicks with the same demo state case number silently replace the prior
 * patient instead of failing with a 409 -- demo data is throwaway by design.
 */
@SpringBootTest(properties = "rhea.demo-reset-duplicates=true")
@ActiveProfiles("test")
class PatientServiceDemoResetIT {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void recreatingWithSameCaseNumberExpungesThePriorPatientInstead() {
        Patient first = patientService.create(
                new CreatePatientRequest("Jordan", "Reyes", LocalDate.of(1990, 5, 14), "CO-2026-DEMO-1"));

        Patient second = patientService.create(
                new CreatePatientRequest("Jordan", "Reyes", LocalDate.of(1990, 5, 14), "CO-2026-DEMO-1"));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(patientRepository.findById(first.getId())).isEmpty();
        assertThat(patientRepository.findByStateCaseNumber("CO-2026-DEMO-1"))
                .map(Patient::getId)
                .contains(second.getId());
    }
}
