package com.aalvarenga.sales.feature.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aalvarenga.sales.feature.entity.FeatureToggle;

public interface FeatureToggleRepository extends JpaRepository<FeatureToggle, String> {
}
