package org.callistotech.rhea.repository;

import org.callistotech.rhea.model.UnemploymentVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnemploymentVerificationRepository extends JpaRepository<UnemploymentVerification, Long> {

    void deleteByPatient_Id(Long patientId);
}
