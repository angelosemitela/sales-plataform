package com.aalvarenga.sales.catalog.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Modelo de tributação (SERVICE, PRODUCT) com suas alíquotas. */
@Entity
@Table(name = "tax_model")
@Getter
@Setter
@NoArgsConstructor
public class TaxModel {

    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "description", nullable = false, length = 80)
    private String description;

    @OneToMany(mappedBy = "taxModel")
    @OrderBy("name ASC")
    private List<TaxModelItem> items = new ArrayList<>();
}
