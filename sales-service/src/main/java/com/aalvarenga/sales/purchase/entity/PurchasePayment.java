package com.aalvarenga.sales.purchase.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.purchase.CardBrand;
import com.aalvarenga.sales.purchase.WalletProvider;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados de pagamento da compra, gravados na FINALIZAÇÃO a partir da tokenização.
 * Nunca guarda número completo de cartão nem CVV (PCI-DSS): só bandeira, últimos
 * 4 dígitos, validade e os tokens devolvidos pelo gateway.
 */
@Entity
@Table(name = "purchase_payment")
@Getter
@Setter
@NoArgsConstructor
public class PurchasePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 10)
    private PaymentMethod method;

    @Column(name = "issuer", length = 60)
    private String issuer;

    @Enumerated(EnumType.STRING)
    @Column(name = "brand", length = 20)
    private CardBrand brand;

    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Column(name = "expiration", length = 5)
    private String expiration;

    @Column(name = "multiple")
    private Boolean multiple;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_provider", length = 20)
    private WalletProvider walletProvider;

    @Column(name = "installments", nullable = false)
    private int installments;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchasePaymentToken> tokens = new ArrayList<>();

    public void addToken(PurchasePaymentToken token) {
        token.setPayment(this);
        tokens.add(token);
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
