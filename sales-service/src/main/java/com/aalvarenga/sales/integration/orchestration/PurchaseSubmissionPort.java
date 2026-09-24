package com.aalvarenga.sales.integration.orchestration;

import com.aalvarenga.sales.integration.billing.BillingPurchaseDraft;
import com.aalvarenga.sales.purchase.entity.Purchase;

/**
 * PORTA de saída do clique final "Comprar". O domínio só sabe que a compra
 * precisa ser "submetida"; COMO (outbox + orquestrador, REST direto no billing,
 * fila) é decisão do adapter.
 *
 * <p>Implementação atual: {@link OutboxPurchaseSubmissionAdapter} (grava o evento
 * na mesma transação da compra). Alternativa futura para casos simples: um adapter
 * que chama o billing direto via {@code RestClient}/{@code @HttpExchange}.</p>
 */
public interface PurchaseSubmissionPort {

    void submit(Purchase purchase, BillingPurchaseDraft draft);
}
