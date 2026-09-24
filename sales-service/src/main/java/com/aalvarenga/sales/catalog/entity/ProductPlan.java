package com.aalvarenga.sales.catalog.entity;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plano de cobrança de um produto (um item de "billingInformation" do catálogo).
 *
 * <p>Os métodos aceitos são uma {@code @ElementCollection}: uma tabela filha
 * simples (product_plan_payment_method) sem entidade própria, porque um método
 * de pagamento não tem identidade nem ciclo de vida fora do plano.</p>
 */
@Entity
@Table(name = "product_plan")
@Getter
@Setter
@NoArgsConstructor
public class ProductPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", nullable = false, length = 10)
    private RecurrenceFrequency recurrenceFrequency;

    @Column(name = "product_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal productValue;

    @Column(name = "max_installments", nullable = false)
    private int maxInstallments;

    @Column(name = "has_discount", nullable = false)
    private boolean hasDiscount;

    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "discount_cycles")
    private Integer discountCycles;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_plan_payment_method", joinColumns = @JoinColumn(name = "product_plan_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 10)
    private Set<PaymentMethod> paymentMethods = EnumSet.noneOf(PaymentMethod.class);

    public boolean accepts(PaymentMethod method) {
        return paymentMethods.contains(method);
    }
}
