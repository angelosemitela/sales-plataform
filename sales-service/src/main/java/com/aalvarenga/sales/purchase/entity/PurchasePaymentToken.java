package com.aalvarenga.sales.purchase.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_payment_token")
@Getter
@Setter
@NoArgsConstructor
public class PurchasePaymentToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_payment_id")
    private PurchasePayment payment;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "token", nullable = false, length = 120)
    private String token;

    @Column(name = "gateway", nullable = false, length = 60)
    private String gateway;

    @Column(name = "expiration_at")
    private Instant expirationAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
