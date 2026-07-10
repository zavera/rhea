package org.callistotech.rhea.service;

import org.callistotech.rhea.dto.CreatePatientRequest;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient create(CreatePatientRequest request) {
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
