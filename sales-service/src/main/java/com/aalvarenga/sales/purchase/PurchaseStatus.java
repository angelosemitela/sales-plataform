package com.aalvarenga.sales.purchase;

/** Ciclo de vida da compra - ver diagrama na migration V4. */
public enum PurchaseStatus {
    /** Carrinho aberto: assinante escolhendo plano/pagamento. */
    CART,
    /** Carrinho sem atividade (job) ou substituído por outro. */
    ABANDONED,
    /** "Comprar" clicado: evento enviado ao orquestrador, aguardando resultado. */
    PROCESSING,
    COMPLETED,
    FAILED
}
