package com.aalvarenga.sales.integration.billing;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.purchase.entity.Purchase;
import com.aalvarenga.sales.purchase.entity.PurchasePayment;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.subscriber.entity.SubscriberAddress;

/**
 * Traduz o modelo do Sales para o contrato do billing (regras gerais 3 a 6):
 * <ul>
 *   <li>assinante -> {@code account} ({@code externalId} = ID gerado no Sales);</li>
 *   <li>produto/plano -> {@code product};</li>
 *   <li>pagamento tokenizado -> {@code payment};</li>
 *   <li>cotação gravada na compra -> {@code billing} (com as taxas da compra).</li>
 * </ul>
 * Mapper isolado = o contrato externo nunca "vaza" para as entidades. Se o billing
 * mudar um nome de campo, só esta classe muda. (Estudo futuro: MapStruct gera
 * mapeamentos como este em tempo de compilação.)
 */
@Component
public class BillingPurchaseDraftMapper {

    /** O billing exige {@code address.number}; endereço sem número vai como "S/N". */
    static final String NO_NUMBER = "S/N";

    public BillingPurchaseDraft toDraft(Purchase purchase, Subscriber subscriber, PurchasePayment payment, Instant transactionDt) {
        Product product = purchase.getProduct();
        RecurrenceFrequency frequency = purchase.getPlan().getRecurrenceFrequency();
        boolean hasDiscount = purchase.getDiscountValue().signum() > 0;

        BillingPurchaseDraft.Account account = new BillingPurchaseDraft.Account(
                null,   // conta nova no billing: o id técnico é gerado lá
                subscriber.getName(),
                subscriber.getExternalId().toString(),
                subscriber.getEmail(),
                subscriber.isAuthorizedFallback(),
                subscriber.getDocuments().stream()
                        .map(d -> new BillingPurchaseDraft.Document(d.getDocumentType(), d.getDescription(), d.getValue(), d.getCountryCode()))
                        .toList(),
                subscriber.getAddresses().stream().map(BillingPurchaseDraftMapper::toAddress).toList(),
                subscriber.getPhones().stream().map(p -> new BillingPurchaseDraft.Phone(p.fullNumberDigits())).toList());

        BillingPurchaseDraft.Product productItem = new BillingPurchaseDraft.Product(
                product.getCodeId(),
                product.getName(),
                frequency.isRecurring() ? "RECURRENCE" : "ONESHOT",
                product.isExpirationService(),
                frequency.isRecurring() ? frequency.name() : null,
                purchase.getProductValue(),
                purchase.getDiscountValue(),
                hasDiscount ? purchase.getDiscountCycles() : 0,
                purchase.getCurrency(),
                product.isTrial(),
                product.getTrialDays());

        BillingPurchaseDraft.Payment paymentItem = new BillingPurchaseDraft.Payment(
                payment.getMethod().name(),
                payment.getIssuer(),
                // O billing exige cardNumber em CREDIT/DEBIT; mandamos só os 4 finais,
                // mascarados - o número completo nunca existiu no Sales.
                payment.getLastFour() == null ? null : "************" + payment.getLastFour(),
                payment.getExpiration(),
                payment.getMultiple(),
                Boolean.TRUE,
                payment.getInstallments(),
                payment.getBrand() == null ? null : payment.getBrand().name(),
                payment.getTokens().stream()
                        .map(t -> new BillingPurchaseDraft.Token(t.getName(), t.getToken(), t.getGateway(),
                                t.getExpirationAt() == null ? null : String.valueOf(t.getExpirationAt().toEpochMilli())))
                        .toList());

        // Regra do billing: fatura com chargedValue = 0 não é registrada (ex: trial).
        List<BillingPurchaseDraft.Billing> billing = purchase.getChargedValue().signum() == 0
                ? List.of()
                : List.of(new BillingPurchaseDraft.Billing(
                        product.getCodeId(),
                        purchase.getProductValue(),
                        purchase.getDiscountValue(),
                        purchase.getTaxValue(),
                        purchase.getChargedValue(),
                        purchase.getCurrency(),
                        null,   // transactionId: preenchido pelo orquestrador após capturar o pagamento
                        purchase.getInstallments(),
                        null,   // provider: idem
                        payment.getMethod().name(),
                        purchase.getTaxes().stream().map(t -> new BillingPurchaseDraft.Tax(t.getName(), t.getValue())).toList()));

        return new BillingPurchaseDraft(
                purchase.getChannel(),
                // O billing trabalha com epoch em milissegundos (String); aqui guardamos Instant.
                String.valueOf(transactionDt.toEpochMilli()),
                purchase.getProtocol(),
                List.of(account),
                List.of(productItem),
                List.of(paymentItem),
                billing);
    }

    private static BillingPurchaseDraft.Address toAddress(SubscriberAddress a) {
        return new BillingPurchaseDraft.Address(
                a.getType().name(),
                a.getDescription(),
                a.getAddressName(),
                a.getNumber() == null ? NO_NUMBER : a.getNumber(),
                a.getComplement(),
                a.getZipCode(),
                a.getCountryCode());
    }
}
