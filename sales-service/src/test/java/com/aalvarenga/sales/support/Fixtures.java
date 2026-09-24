package com.aalvarenga.sales.support;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.catalog.entity.ProductPlan;
import com.aalvarenga.sales.catalog.entity.TaxModel;
import com.aalvarenga.sales.catalog.entity.TaxModelItem;
import com.aalvarenga.sales.subscriber.entity.Subscriber;

/** Massa de teste em memória, espelhando a carga inicial do catálogo (migration V6). */
public final class Fixtures {

    private Fixtures() {
    }

    public static Subscriber subscriber() {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(10L);
        subscriber.setExternalId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        subscriber.setName("Angelo Alvarenga");
        subscriber.setEmail("angelo@example.com");
        subscriber.setAuthorizedFallback(true);
        return subscriber;
    }

    public static TaxModel serviceTaxModel() {
        return taxModel("SERVICE", item("CBS", "8.8"), item("IBS", "3"));
    }

    public static TaxModel productTaxModel() {
        return taxModel("PRODUCT", item("CBS", "7.6"), item("IBS", "1"), item("ISS", "2.3"));
    }

    /** TS1: mensal 25,90 (1x) e anual 268,80 (12x, desconto de 30 por 1 ciclo). Compra exclusiva. */
    public static Product streaming1() {
        Product product = product(1L, "TS1", "Teste Streaming 1", true, serviceTaxModel());
        product.setExclusivePurchase(true);
        product.getPlans().add(plan(product, 11L, RecurrenceFrequency.MONTH, "25.90", 1, null, null,
                EnumSet.of(PaymentMethod.CREDIT, PaymentMethod.DEBIT, PaymentMethod.PIX)));
        product.getPlans().add(plan(product, 12L, RecurrenceFrequency.ANNUAL, "268.80", 12, "30.00", 1,
                EnumSet.allOf(PaymentMethod.class)));
        return product;
    }

    /** VR1 (Boné): avulso 10,90, até 12x no catálogo (mas o mínimo de R$ 5 limita a 2x). */
    public static Product cap() {
        Product product = product(3L, "VR1", "Bone", false, productTaxModel());
        product.getPlans().add(plan(product, 31L, RecurrenceFrequency.ONESHOT, "10.90", 12, null, null,
                EnumSet.of(PaymentMethod.CREDIT, PaymentMethod.DEBIT, PaymentMethod.PIX)));
        return product;
    }

    private static Product product(Long id, String codeId, String name, boolean expirationService, TaxModel taxModel) {
        Product product = new Product();
        product.setId(id);
        product.setCodeId(codeId);
        product.setName(name);
        product.setExpirationService(expirationService);
        product.setTaxModel(taxModel);
        product.setActive(true);
        return product;
    }

    private static ProductPlan plan(Product product, Long id, RecurrenceFrequency frequency, String value, int maxInstallments,
                                    String discount, Integer cycles, EnumSet<PaymentMethod> methods) {
        ProductPlan plan = new ProductPlan();
        plan.setId(id);
        plan.setProduct(product);
        plan.setRecurrenceFrequency(frequency);
        plan.setProductValue(new BigDecimal(value));
        plan.setMaxInstallments(maxInstallments);
        plan.setHasDiscount(discount != null);
        plan.setDiscountValue(discount == null ? null : new BigDecimal(discount));
        plan.setDiscountCycles(cycles);
        plan.setPaymentMethods(methods);
        return plan;
    }

    private static TaxModel taxModel(String code, TaxModelItem... items) {
        TaxModel model = new TaxModel();
        model.setCode(code);
        model.setItems(List.of(items));
        return model;
    }

    private static TaxModelItem item(String name, String rate) {
        TaxModelItem item = new TaxModelItem();
        item.setName(name);
        item.setRate(new BigDecimal(rate));
        return item;
    }
}
