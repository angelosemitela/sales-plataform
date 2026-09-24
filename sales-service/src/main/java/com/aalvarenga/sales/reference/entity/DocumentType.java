package com.aalvarenga.sales.reference.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tipo de documento (CPF, SSN, UE, PASSPORT, OTHER) e quais países ele aceita.
 *
 * <p>A regra "qual país vale para qual documento" mora no BANCO, não em código:
 * incluir um país novo na lista da UE é um INSERT, sem novo deploy.</p>
 */
@Entity
@Table(name = "document_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentType {

    public static final String CPF = "CPF";

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "description", nullable = false, length = 80)
    private String description;

    /** A tela deve abrir seleção de país? */
    @Column(name = "country_selectable", nullable = false)
    private boolean countrySelectable;

    /** Aceita qualquer país (PASSPORT/OTHER)? Se não, só os de {@link #allowedCountries}. */
    @Column(name = "all_countries", nullable = false)
    private boolean allCountries;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToMany
    @JoinTable(name = "document_type_country",
            joinColumns = @JoinColumn(name = "document_type_code"),
            inverseJoinColumns = @JoinColumn(name = "country_code"))
    private Set<Country> allowedCountries = new HashSet<>();

    public DocumentType(String code, boolean countrySelectable, boolean allCountries, Set<Country> allowedCountries) {
        this.code = code;
        this.description = code;
        this.countrySelectable = countrySelectable;
        this.allCountries = allCountries;
        this.allowedCountries = allowedCountries;
    }

    public boolean accepts(String countryCode) {
        return allCountries || allowedCountries.stream().anyMatch(c -> c.getCode().equals(countryCode));
    }
}
