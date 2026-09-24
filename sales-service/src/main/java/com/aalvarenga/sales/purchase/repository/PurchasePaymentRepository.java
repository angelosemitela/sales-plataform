package com.aalvarenga.sales.purchase.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aalvarenga.sales.purchase.entity.PurchasePayment;

public interface PurchasePaymentRepository extends JpaRepository<PurchasePayment, Long> {
}
