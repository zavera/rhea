package org.callistotech.rhea.repository;

import org.callistotech.rhea.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByStateCaseNumber(String stateCaseNumber);
}
