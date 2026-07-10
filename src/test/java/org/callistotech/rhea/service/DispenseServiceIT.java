package org.callistotech.rhea.service;

import org.callistotech.rhea.dto.DispenseRequest;
import org.callistotech.rhea.model.BillStatus;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.model.Pharmacy;
import org.callistotech.rhea.model.Prescription;
import org.callistotech.rhea.model.PrescriptionStatus;
import org.callistotech.rhea.model.UnemploymentVerification;
import org.callistotech.rhea.model.VerificationStatus;
import org.callistotech.rhea.repository.PatientRepository;
import org.callistotech.rhea.repository.PharmacyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DispenseServiceIT {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private UnemploymentVerificationService verificationService;

    @Autowired
    private DispenseService dispenseService;

    @Test
    void dispensesAndBillsPatientByDefault() {
        Patient patient = patientRepository.save(patient("CASE-DISPENSE-1"));
        Pharmacy pharmacy = pharmacyRepository.save(pharmacy());
        UnemploymentVerification verification = verificationService.verify(patient, "CO-2026-000123");
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.VERIFIED);

        Prescription prescription = dispenseService.dispense(new DispenseRequest(
                patient.getId(), pharmacy.getId(), verification.getId(),
                "Metformin 500mg", "00093-1048-01", 60, 30, "1234567890", 4250L));

        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.DISPENSED);
        assertThat(prescription.getRetailPriceCents()).isEqualTo(4250L);
        assertThat(prescription.getBillStatus()).isEqualTo(BillStatus.PATIENT_OWES);
    }

    @Test
    void refusesToDispenseWhenVerificationFailed() {
        Patient patient = patientRepository.save(patient("CASE-DISPENSE-2"));
        Pharmacy pharmacy = pharmacyRepository.save(pharmacy());
        UnemploymentVerification verification = verificationService.verify(patient, "NOT-A-VALID-CLAIM");
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.NOT_FOUND);

        DispenseRequest request = new DispenseRequest(
                patient.getId(), pharmacy.getId(), verification.getId(),
                "Metformin 500mg", "00093-1048-01", 60, 30, "1234567890", 4250L);

        assertThatThrownBy(() -> dispenseService.dispense(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not verified");
    }

    private Patient patient(String caseNumber) {
        Patient patient = new Patient();
        patient.setFirstName("Jordan");
        patient.setLastName("Reyes");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 14));
        patient.setStateCaseNumber(caseNumber);
        return patient;
    }

    private Pharmacy pharmacy() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setNcpdpId("DEMO" + System.nanoTime());
        pharmacy.setName("Rhea Community Pharmacy (Demo)");
        pharmacy.setCity("Denver");
        pharmacy.setState("CO");
        pharmacy.setZip("80203");
        return pharmacy;
    }
}
