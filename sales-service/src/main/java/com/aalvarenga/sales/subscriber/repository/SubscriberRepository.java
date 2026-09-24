package com.aalvarenga.sales.subscriber.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aalvarenga.sales.subscriber.entity.Subscriber;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    /** Casa com o índice único funcional {@code lower(email)} criado na migration V3. */
    @Query("select s from Subscriber s where lower(s.email) = lower(:email)")
    Optional<Subscriber> findByEmailIgnoreCase(@Param("email") String email);

    @Query("select count(s) > 0 from Subscriber s where lower(s.email) = lower(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    Optional<Subscriber> findByExternalId(UUID externalId);
}
