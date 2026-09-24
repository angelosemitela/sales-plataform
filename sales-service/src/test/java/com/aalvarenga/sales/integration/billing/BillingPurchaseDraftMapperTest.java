package com.aalvarenga.sales.integration.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.pricing.PricingInput;
import com.aalvarenga.sales.pricing.PricingService;
import com.aalvarenga.sales.purchase.entity.Purchase;
import com.aalvarenga.sales.purchase.entity.PurchasePayment;
import com.aalvarenga.sales.subscriber.AddressType;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.subscriber.entity.SubscriberAddress;
import com.aalvarenga.sales.subscriber.entity.SubscriberDocument;
import com.aalvarenga.sales.subscriber.entity.SubscriberPhone;
import com.aalvarenga.sales.support.Fixtures;

/** Garante que o rascunho respeita as regras que o billing valida na entrada. */
class BillingPurchaseDraftMapperTest {

    private static final Instant NOW = Instant.parse("2026-09-24T15:00:00Z");

    private final BillingPurchaseDraftMapper mapper = new BillingPurchaseDraftMapper();

    @Test
    void mapsAccountProductPaymentAndBillingWithTheBillingContract() {
        Subscriber subscriber = fullSubscriber();
        Product product = Fixtures.streaming1();
        Purchase purchase = Purchase.start(subscriber, product, product.findPlan(RecurrenceFrequency.ANNUAL).orElseThrow(), NOW);
        purchase.setPaymentMethod(PaymentMethod.PIX);
        purchase.setInstallments(1);
        purchase.applyQuote(new PricingService().quote(PricingInput.of(purchase.getPlan(), PaymentMethod.PIX,
                new BigDecimal("5.00"), LocalDate.of(2026, 9, 24))));
        PurchasePayment payment = new PurchasePayment();
        payment.setMethod(PaymentMethod.PIX);
        payment.setInstallments(1);

        BillingPurchaseDraft draft = mapper.toDraft(purchase, subscriber, payment, NOW);

        assertThat(draft.channel()).isEqualTo("WEB");
        assertThat(draft.transactionDt()).isEqualTo(String.valueOf(NOW.toEpochMilli()));
        assertThat(draft.protocol()).isEqualTo(purchase.getProtocol());

        BillingPurchaseDraft.Account account = draft.account().getFirst();
        assertThat(account.id()).isNull();
        assertThat(account.externalId()).isEqualTo(subscriber.getExternalId().toString());
        assertThat(account.address().getFirst().number()).isEqualTo("S/N");
        assertThat(account.phone().getFirst().number()).isEqualTo("5521999999999");

        BillingPurchaseDraft.Product item = draft.product().getFirst();
        assertThat(item.type()).isEqualTo("RECURRENCE");
        assertThat(item.recurrenceFrequency()).isEqualTo("ANNUAL");
        assertThat(item.discountCycles()).isEqualTo(1);

        BillingPurchaseDraft.Billing billing = draft.billing().getFirst();
        // Regras validadas pelo billing: fórmula fecha e soma das taxas = taxValue.
        assertThat(billing.productValue().subtract(billing.discountValue()).add(billing.taxValue()))
                .isEqualByComparingTo(billing.chargedValue());
        assertThat(billing.tax().stream().map(BillingPurchaseDraft.Tax::value).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(billing.taxValue());
        assertThat(billing.currency()).isEqualTo("BRL");
        assertThat(billing.transactionId()).isNull(); // preenchido pelo orquestrador
    }

    @Test
    void oneShotProduct_hasNoRecurrenceFrequencyAndZeroDiscountCycles() {
        Subscriber subscriber = fullSubscriber();
        Product product = Fixtures.cap();
        Purchase purchase = Purchase.start(subscriber, product, product.defaultPlan(), NOW);
        purchase.setInstallments(2);
        purchase.applyQuote(new PricingService().quote(PricingInput.of(purchase.getPlan(), PaymentMethod.CREDIT,
                new BigDecimal("5.00"), LocalDate.of(2026, 9, 24))));
        PurchasePayment payment = new PurchasePayment();
        payment.setMethod(PaymentMethod.CREDIT);
        payment.setLastFour("4242");
        payment.setInstallments(2);

        BillingPurchaseDraft draft = mapper.toDraft(purchase, subscriber, payment, NOW);

        assertThat(draft.product().getFirst().type()).isEqualTo("ONESHOT");
        assertThat(draft.product().getFirst().recurrenceFrequency()).isNull();
        assertThat(draft.product().getFirst().discountCycles()).isZero();
        assertThat(draft.billing().getFirst().installments()).isEqualTo(2);
        assertThat(draft.payment().getFirst().cardNumber()).isEqualTo("************4242");
    }

    private static Subscriber fullSubscriber() {
        Subscriber subscriber = Fixtures.subscriber();
        SubscriberDocument document = new SubscriberDocument();
        document.setDocumentType("CPF");
        document.setValue("52998224725");
        document.setCountryCode("BR");
        subscriber.addDocument(document);
        SubscriberAddress address = new SubscriberAddress();
        address.setType(AddressType.RESIDENCIAL);
        address.setDescription("Casa");
        address.setAddressName("Rua Uno");
        address.setZipCode("24220000");
        address.setCountryCode("BR");
        subscriber.addAddress(address);
        SubscriberPhone phone = new SubscriberPhone();
        phone.setCountryCode("BR");
        phone.setDialCode("+55");
        phone.setNumber("21999999999");
        subscriber.addPhone(phone);
        return subscriber;
    }
}
