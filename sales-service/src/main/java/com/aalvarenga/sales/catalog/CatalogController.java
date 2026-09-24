package com.aalvarenga.sales.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aalvarenga.sales.catalog.dto.CatalogProductResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public List<CatalogProductResponse> list() {
        return catalogService.list();
    }

    @GetMapping("/{codeId}")
    public CatalogProductResponse get(@PathVariable String codeId) {
        return catalogService.get(codeId);
    }
}
