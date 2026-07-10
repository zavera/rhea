package org.callistotech.rhea.controller;

import org.callistotech.rhea.dto.CreatePatientRequest;
import org.callistotech.rhea.model.Patient;
import org.callistotech.rhea.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping("/api/patients")
    @ResponseStatus(HttpStatus.CREATED)
    public Patient create(@RequestBody CreatePatientRequest request) {
        return patientService.create(request);
    }

    @GetMapping("/api/patients")
    public List<Patient> list() {
        return patientService.list();
    }

    @GetMapping("/api/patients/{id}")
    public Patient get(@PathVariable Long id) {
        return patientService.get(id);
    }
}
