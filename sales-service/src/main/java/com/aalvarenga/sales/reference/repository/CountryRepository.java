package com.aalvarenga.sales.reference.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aalvarenga.sales.reference.entity.Country;

public interface CountryRepository extends JpaRepository<Country, String> {

    List<Country> findAllByOrderByNameAsc();
}
