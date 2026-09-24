package com.aalvarenga.sales.catalog.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import com.aalvarenga.sales.catalog.RecurrenceFrequency;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Produto do catálogo. Os preços ficam nos planos ({@link ProductPlan}). */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_id", nullable = false, length = 30)
    private String codeId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "expiration_service", nullable = false)
    private boolean expirationService;

    @Column(name = "exclusive_purchase", nullable = false)
    private boolean exclusivePurchase;

    @Column(name = "trial", nullable = false)
    private boolean trial;

    @Column(name = "trial_days")
    private Integer trialDays;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_model_code")
    private TaxModel taxModel;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "product")
    @OrderBy("sortOrder ASC")
    private List<ProductPlan> plans = new ArrayList<>();

    public Optional<ProductPlan> findPlan(RecurrenceFrequency frequency) {
        return plans.stream().filter(p -> p.getRecurrenceFrequency() == frequency).findFirst();
    }

    /** Plano exibido por padrão ao abrir o produto: o primeiro da ordenação do catálogo. */
    public ProductPlan defaultPlan() {
        return plans.getFirst();
    }
}
