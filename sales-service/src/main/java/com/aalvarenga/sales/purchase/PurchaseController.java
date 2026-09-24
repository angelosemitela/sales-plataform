package com.aalvarenga.sales.purchase;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aalvarenga.sales.purchase.dto.CartResponse;
import com.aalvarenga.sales.purchase.dto.CheckoutRequest;
import com.aalvarenga.sales.purchase.dto.CheckoutResponse;
import com.aalvarenga.sales.purchase.dto.StartCartRequest;
import com.aalvarenga.sales.purchase.dto.UpdateCartSelectionRequest;
import com.aalvarenga.sales.subscriber.CurrentSubscriberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Compras do assinante logado. O ID na URL é o UUID público da compra.
 *
 * <pre>
 * POST /api/v1/purchases                  abre carrinho (clique no produto)
 * GET  /api/v1/purchases/{id}             consulta carrinho/compra
 * PUT  /api/v1/purchases/{id}/selection   atualiza plano/método/parcelas
 * POST /api/v1/purchases/{id}/checkout    "Comprar" (desligado por toggle)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final CurrentSubscriberService currentSubscriberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse start(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartCartRequest request) {
        return cartService.start(currentSubscriberService.require(jwt), request);
    }

    @GetMapping("/{id}")
    public CartResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return cartService.get(currentSubscriberService.require(jwt), id);
    }

    @PutMapping("/{id}/selection")
    public CartResponse updateSelection(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                        @Valid @RequestBody UpdateCartSelectionRequest request) {
        return cartService.updateSelection(currentSubscriberService.require(jwt), id, request);
    }

    /** 202 Accepted: a compra foi ACEITA para processamento assíncrono (orquestrador), não concluída. */
    @PostMapping("/{id}/checkout")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CheckoutResponse checkout(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @Valid @RequestBody CheckoutRequest request) {
        return checkoutService.checkout(currentSubscriberService.require(jwt), id, request);
    }
}
