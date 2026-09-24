package com.aalvarenga.sales.reference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** País (ISO 3166-1 alpha-2) com nome em pt-BR e DDI. Dado de referência somente leitura. */
@Entity
@Table(name = "country")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Country {

    @Id
    @Column(name = "code", length = 2)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "dial_code", nullable = false, length = 8)
    private String dialCode;

    public Country(String code, String name, String dialCode) {
        this.code = code;
        this.name = name;
        this.dialCode = dialCode;
    }
}
