package com.aalvarenga.sales.catalog.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Uma taxa de um modelo: nome (CBS, IBS, ISS) e alíquota PERCENTUAL. */
@Entity
@Table(name = "tax_model_item")
@Getter
@Setter
@NoArgsConstructor
public class TaxModelItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_model_code")
    private TaxModel taxModel;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal rate;
}
