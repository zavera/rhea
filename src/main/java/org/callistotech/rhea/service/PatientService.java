package org.callistotech.rhea.service;

import org.callistotech.rhea.dto.CreatePatientRequest;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.InsuranceMatchRecommendationRepository;
import org.callistotech.rhea.repository.PatientRepository;
import org.callistotech.rhea.repository.PrescriptionRepository;
import org.callistotech.rhea.repository.ReimbursementClaimRepository;
import org.callistotech.rhea.repository.UnemploymentVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UnemploymentVerificationRepository verificationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ReimbursementClaimRepository claimRepository;
    private final InsuranceMatchRecommendationRepository recommendationRepository;
    private final boolean demoResetDuplicates;

    public PatientService(PatientRepository patientRepository,
                           UnemploymentVerificationRepository verificationRepository,
                           PrescriptionRepository prescriptionRepository,
                           ReimbursementClaimRepository claimRepository,
                           InsuranceMatchRecommendationRepository recommendationRepository,
                           @Value("${rhea.demo-reset-duplicates:false}") boolean demoResetDuplicates) {
        this.patientRepository = patientRepository;
        this.verificationRepository = verificationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.claimRepository = claimRepository;
        this.recommendationRepository = recommendationRepository;
        this.demoResetDuplicates = demoResetDuplicates;
    }

    /**
     * In demo mode ({@code DEMO_RESET_DUPLICATES=true}), re-submitting the same state case
     * number expunges the prior patient and everything tied to it instead of failing with a
     * conflict -- demo data is throwaway by design. Real deploys keep the state case number
     * unique constraint enforced (see {@link org.callistotech.rhea.controller.GlobalExceptionHandler}).
     */
    @Transactional
    public Patient create(CreatePatientRequest request) {
        if (demoResetDuplicates) {
            Optional<Patient> existing = patientRepository.findByStateCaseNumber(request.stateCaseNumber());
            if (existing.isPresent()) {
                Long patientId = existing.get().getId();
                claimRepository.deleteByPrescription_Patient_Id(patientId);
                prescriptionRepository.deleteByPatient_Id(patientId);
                verificationRepository.deleteByPatient_Id(patientId);
                recommendationRepository.deleteByPatient_Id(patientId);
                patientRepository.delete(existing.get());
                patientRepository.flush();
            }
        }

        Patient patient = new Patient();
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setStateCaseNumber(request.stateCaseNumber());
        return patientRepository.save(patient);
    }

    public Patient get(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Patient not found: " + id));
    }

    public java.util.List<Patient> list() {
        return patientRepository.findAll();
    }
}
