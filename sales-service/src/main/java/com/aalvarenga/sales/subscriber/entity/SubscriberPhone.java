package com.aalvarenga.sales.subscriber.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscriber_phone")
@Getter
@Setter
@NoArgsConstructor
public class SubscriberPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id")
    private Subscriber subscriber;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    /** DDI copiado no cadastro, ex: "+55". */
    @Column(name = "dial_code", nullable = false, length = 8)
    private String dialCode;

    /** Só dígitos, sem o DDI. */
    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Formato do billing ({@code account.phone[].number}): DDI + número, só dígitos. Ex: 5521999999999. */
    public String fullNumberDigits() {
        return dialCode.replaceAll("\\D", "") + number;
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
