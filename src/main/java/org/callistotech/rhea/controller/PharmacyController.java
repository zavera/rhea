package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.CreatePharmacyRequest;
import org.callistotech.rhea.model.Pharmacy;
import org.callistotech.rhea.service.PharmacyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @PostMapping("/api/pharmacies")
    @ResponseStatus(HttpStatus.CREATED)
    public Pharmacy create(@RequestBody CreatePharmacyRequest request) {
        return pharmacyService.create(request);
    }

    @GetMapping("/api/pharmacies")
    public List<Pharmacy> list() {
        return pharmacyService.list();
    }
}
