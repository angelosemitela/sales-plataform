package com.aalvarenga.sales.purchase.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.catalog.entity.ProductPlan;
import com.aalvarenga.sales.pricing.PriceQuote;
import com.aalvarenga.sales.pricing.TaxLine;
import com.aalvarenga.sales.purchase.AbandonReason;
import com.aalvarenga.sales.purchase.CardBrand;
import com.aalvarenga.sales.purchase.PurchaseStatus;
import com.aalvarenga.sales.purchase.PurchaseStep;
import com.aalvarenga.sales.purchase.WalletProvider;
import com.aalvarenga.sales.subscriber.entity.Subscriber;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compra - que nasce como CARRINHO (status CART) no clique do produto.
 *
 * <p>Os métodos de negócio ({@link #start}, {@link #applyQuote}, {@link #abandon},
 * {@link #submit}) concentram as transições de estado aqui dentro, em vez de
 * espalhar {@code setStatus(...)} pelos services ("modelo rico" x "modelo anêmico").</p>
 */
@Entity
@Table(name = "purchase")
@Getter
@Setter
@NoArgsConstructor
public class Purchase {

    public static final String CHANNEL_WEB = "WEB";
    public static final String CURRENCY_BRL = "BRL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "protocol", nullable = false, updatable = false, length = 40)
    private String protocol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id")
    private Subscriber subscriber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_plan_id")
    private ProductPlan plan;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel = CHANNEL_WEB;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = CURRENCY_BRL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_step", nullable = false, length = 30)
    private PurchaseStep lastStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 10)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_provider", length = 20)
    private WalletProvider walletProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand", length = 20)
    private CardBrand cardBrand;

    @Column(name = "installments")
    private Integer installments;

    @Column(name = "product_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal productValue;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "discount_cycles")
    private Integer discountCycles;

    @Column(name = "tax_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxValue;

    @Column(name = "charged_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargedValue;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "first_charge_date")
    private LocalDate firstChargeDate;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "abandon_reason", length = 30)
    private AbandonReason abandonReason;

    @Column(name = "abandoned_at")
    private Instant abandonedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseTax> taxes = new ArrayList<>();

    /** Abre um carrinho novo para o produto/plano escolhido. */
    public static Purchase start(Subscriber subscriber, Product product, ProductPlan plan, Instant now) {
        Purchase purchase = new Purchase();
        purchase.publicId = UUID.randomUUID();
        // "SLS-" + UUID = 40 caracteres, único e sem depender de sequência do banco.
        purchase.protocol = "SLS-" + purchase.publicId;
        purchase.subscriber = subscriber;
        purchase.product = product;
        purchase.plan = plan;
        purchase.status = PurchaseStatus.CART;
        purchase.lastStep = PurchaseStep.PRODUCT_SELECTED;
        purchase.startedAt = now;
        purchase.lastActivityAt = now;
        return purchase;
    }

    /**
     * Grava a "foto" da cotação (valores + taxas) na compra.
     *
     * <p><b>Atenção - ordem de flush do Hibernate:</b> a versão anterior fazia
     * {@code taxes.clear()} + novos {@code PurchaseTax}. Parece correto, mas no flush
     * o Hibernate executa os INSERTs ANTES dos DELETEs do {@code orphanRemoval}; a
     * taxa nova "CBS" era inserida enquanto a antiga "CBS" ainda existia, violando a
     * constraint única {@code uk_purchase_tax (purchase_id, name)} -> HTTP 409 em toda
     * alteração do carrinho.</p>
     *
     * <p>Correção: sincronizar a coleção PELO NOME da taxa - atualiza as que já existem
     * (UPDATE), inclui as novas (INSERT) e remove só as que saíram (DELETE). Nenhuma
     * linha com o mesmo nome é inserida enquanto a antiga existe.</p>
     */
    public void applyQuote(PriceQuote quote) {
        productValue = quote.productValue();
        discountValue = quote.discountValue();
        discountCycles = quote.discountCycles();
        taxValue = quote.taxValue();
        chargedValue = quote.chargedValue();
        trialDays = quote.trialDays();
        firstChargeDate = quote.firstChargeDate();
        syncTaxes(quote.taxes());
    }

    private void syncTaxes(List<TaxLine> lines) {
        Map<String, TaxLine> byName = new LinkedHashMap<>();
        lines.forEach(line -> byName.put(line.name(), line));
        taxes.removeIf(tax -> !byName.containsKey(tax.getName()));
        for (TaxLine line : byName.values()) {
            taxes.stream()
                    .filter(tax -> tax.getName().equals(line.name()))
                    .findFirst()
                    .ifPresentOrElse(
                            tax -> tax.update(line.rate(), line.value()),
                            () -> taxes.add(new PurchaseTax(this, line.name(), line.rate(), line.value())));
        }
    }

    public boolean isOpen() {
        return status == PurchaseStatus.CART;
    }

    public void touch(Instant now) {
        lastActivityAt = now;
    }

    public void abandon(AbandonReason reason, Instant now) {
        status = PurchaseStatus.ABANDONED;
        abandonReason = reason;
        abandonedAt = now;
    }

    public void submit(Instant now) {
        status = PurchaseStatus.PROCESSING;
        lastStep = PurchaseStep.SUBMITTED;
        submittedAt = now;
        lastActivityAt = now;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
