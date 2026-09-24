package com.aalvarenga.sales.purchase.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aalvarenga.sales.purchase.AbandonReason;
import com.aalvarenga.sales.purchase.PurchaseStatus;
import com.aalvarenga.sales.purchase.entity.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    /** Busca SEMPRE filtrando pelo dono: um assinante nunca enxerga o carrinho de outro. */
    Optional<Purchase> findByPublicIdAndSubscriberId(UUID publicId, Long subscriberId);

    List<Purchase> findBySubscriberIdAndStatus(Long subscriberId, PurchaseStatus status);

    boolean existsBySubscriberIdAndProductIdAndStatusIn(Long subscriberId, Long productId, Collection<PurchaseStatus> statuses);

    /**
     * Abandona, num único UPDATE, todos os carrinhos parados desde antes de {@code cutoff}.
     * Operação idempotente: se duas instâncias do serviço rodarem o job ao mesmo tempo,
     * a segunda simplesmente não encontra mais linhas em CART.
     * (UPDATE em lote não passa pelo {@code @Version}, por isso o incremento manual.)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update Purchase p
               set p.status = :abandoned, p.abandonReason = :reason, p.abandonedAt = :now,
                   p.updatedAt = :now, p.version = p.version + 1
             where p.status = :cart and p.lastActivityAt < :cutoff
            """)
    int abandonInactiveCarts(@Param("cutoff") Instant cutoff,
                             @Param("now") Instant now,
                             @Param("cart") PurchaseStatus cart,
                             @Param("abandoned") PurchaseStatus abandoned,
                             @Param("reason") AbandonReason reason);
}
