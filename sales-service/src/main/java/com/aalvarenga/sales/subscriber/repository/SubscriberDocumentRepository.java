package com.aalvarenga.sales.subscriber.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aalvarenga.sales.subscriber.entity.SubscriberDocument;

public interface SubscriberDocumentRepository extends JpaRepository<SubscriberDocument, Long> {

    boolean existsByDocumentTypeAndCountryCodeAndValue(String documentType, String countryCode, String value);
}
