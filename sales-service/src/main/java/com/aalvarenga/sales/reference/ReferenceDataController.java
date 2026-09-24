package com.aalvarenga.sales.reference;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aalvarenga.sales.reference.dto.CountryResponse;
import com.aalvarenga.sales.reference.dto.DocumentTypeResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reference")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService service;

    @GetMapping("/countries")
    public List<CountryResponse> countries() {
        return service.countries();
    }

    @GetMapping("/document-types")
    public List<DocumentTypeResponse> documentTypes() {
        return service.documentTypes();
    }
}
