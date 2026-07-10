package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.DispenseRequest;
import org.callistotech.rhea.model.Prescription;
import org.callistotech.rhea.repository.PrescriptionRepository;
import org.callistotech.rhea.service.DispenseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
public class DispenseController {

    private final DispenseService dispenseService;
    private final PrescriptionRepository prescriptionRepository;

    public DispenseController(DispenseService dispenseService, PrescriptionRepository prescriptionRepository) {
        this.dispenseService = dispenseService;
        this.prescriptionRepository = prescriptionRepository;
    }

    @PostMapping("/api/prescriptions/dispense")
    @ResponseStatus(HttpStatus.CREATED)
    public Prescription dispense(@RequestBody DispenseRequest request) {
        return dispenseService.dispense(request);
    }

    @GetMapping("/api/prescriptions/{id}")
    public Prescription get(@PathVariable Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));
    }
}
