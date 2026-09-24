package com.aalvarenga.sales.feature.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "config_parameter")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfigParameter {

    @Id
    @Column(name = "name", length = 60)
    private String name;

    @Column(name = "value", nullable = false)
    private String value;

    public ConfigParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
