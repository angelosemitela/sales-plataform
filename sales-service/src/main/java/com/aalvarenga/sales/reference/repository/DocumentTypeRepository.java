package com.aalvarenga.sales.reference.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aalvarenga.sales.reference.entity.DocumentType;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, String> {

    List<DocumentType> findAllByOrderBySortOrderAsc();
}
