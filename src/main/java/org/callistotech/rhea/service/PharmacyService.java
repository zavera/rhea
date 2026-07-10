package org.callistotech.rhea.service;

import org.callistotech.rhea.dto.CreatePharmacyRequest;
import org.callistotech.rhea.model.Pharmacy;
import org.callistotech.rhea.repository.PharmacyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;

    public PharmacyService(PharmacyRepository pharmacyRepository) {
        this.pharmacyRepository = pharmacyRepository;
    }

    public Pharmacy create(CreatePharmacyRequest request) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setNcpdpId(request.ncpdpId());
        pharmacy.setName(request.name());
        pharmacy.setAddress(request.address());
        pharmacy.setCity(request.city());
        pharmacy.setState(request.state() != null ? request.state() : "CO");
        pharmacy.setZip(request.zip());
        return pharmacyRepository.save(pharmacy);
    }

    public Pharmacy get(Long id) {
        return pharmacyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Pharmacy not found: " + id));
    }

    public List<Pharmacy> list() {
        return pharmacyRepository.findAll();
    }
}
