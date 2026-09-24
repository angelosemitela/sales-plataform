package com.aalvarenga.sales.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aalvarenga.sales.catalog.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrueOrderByIdAsc();

    Optional<Product> findByCodeIdAndActiveTrue(String codeId);
}
