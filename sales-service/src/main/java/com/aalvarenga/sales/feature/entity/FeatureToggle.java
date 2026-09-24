package com.aalvarenga.sales.feature.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feature_toggle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureToggle {

    @Id
    @Column(name = "name", length = 60)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public FeatureToggle(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }
}
