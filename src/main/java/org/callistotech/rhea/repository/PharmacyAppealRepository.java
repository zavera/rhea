package org.callistotech.rhea.repository;

import org.callistotech.rhea.model.PharmacyAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyAppealRepository extends JpaRepository<PharmacyAppeal, Long> {

    void deleteByPrescription_Patient_Id(Long patientId);
}
