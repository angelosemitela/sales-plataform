package com.aalvarenga.sales.purchase.dto;

import java.util.UUID;

import com.aalvarenga.sales.purchase.PurchaseStatus;

public record CheckoutResponse(UUID id, String protocol, PurchaseStatus status) {
}
