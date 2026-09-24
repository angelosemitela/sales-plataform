package com.aalvarenga.sales.subscriber.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Assinante - a RAIZ DO AGREGADO (conceito de DDD): documentos, endereços e
 * telefones só são criados/alterados através dele ({@code cascade = ALL}), então
 * um único {@code save(subscriber)} grava tudo numa transação.
 */
@Entity
@Table(name = "subscriber")
@Getter
@Setter
@NoArgsConstructor
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID público, enviado ao billing como {@code account.externalId}. */
    @Column(name = "external_id", nullable = false, updatable = false)
    private UUID externalId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "authorized_fallback", nullable = false)
    private boolean authorizedFallback;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubscriberDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubscriberAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubscriberPhone> phones = new ArrayList<>();

    public void addDocument(SubscriberDocument document) {
        document.setSubscriber(this);
        documents.add(document);
    }

    public void addAddress(SubscriberAddress address) {
        address.setSubscriber(this);
        addresses.add(address);
    }

    public void addPhone(SubscriberPhone phone) {
        phone.setSubscriber(this);
        phones.add(phone);
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
