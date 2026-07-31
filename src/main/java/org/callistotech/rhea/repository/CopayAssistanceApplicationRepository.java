package org.callistotech.rhea.repository;

import org.callistotech.rhea.model.CopayAssistanceApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CopayAssistanceApplicationRepository extends JpaRepository<CopayAssistanceApplication, Long> {

    void deleteByPatient_Id(Long patientId);
}
