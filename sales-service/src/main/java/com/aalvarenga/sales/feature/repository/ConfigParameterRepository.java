package com.aalvarenga.sales.feature.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aalvarenga.sales.feature.entity.ConfigParameter;

public interface ConfigParameterRepository extends JpaRepository<ConfigParameter, String> {
}
