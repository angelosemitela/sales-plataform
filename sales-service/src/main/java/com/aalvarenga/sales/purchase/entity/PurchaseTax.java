package com.aalvarenga.sales.purchase.entity;

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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Taxa registrada na compra (regra geral 9): alíquota e valor usados naquela venda. */
@Entity
@Table(name = "purchase_tax")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseTax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal rate;

    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    public PurchaseTax(Purchase purchase, String name, BigDecimal rate, BigDecimal value) {
        this.purchase = purchase;
        this.name = name;
        this.rate = rate;
        this.value = value;
    }

    /** Atualiza a taxa já existente (UPDATE), em vez de apagar e recriar - ver Purchase#applyQuote. */
    public void update(BigDecimal newRate, BigDecimal newValue) {
        this.rate = newRate;
        this.value = newValue;
    }
}
