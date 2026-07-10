package org.callistotech.rhea.repository;

import org.callistotech.rhea.model.InsuranceApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceApplicationRepository extends JpaRepository<InsuranceApplication, Long> {

    void deleteByPatient_Id(Long patientId);
}
