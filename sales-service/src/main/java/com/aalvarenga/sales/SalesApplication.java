package com.aalvarenga.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada do sales-service.
 *
 * <p>Organização em pacotes POR FUNCIONALIDADE (package-by-feature: catalog,
 * subscriber, purchase...) em vez de por camada (controller/service/repository
 * globais, como no billing). Cada pasta concentra tudo de um assunto - fica mais
 * fácil de navegar quando o projeto cresce e, se um dia uma parte virar um
 * microsserviço próprio, é só "recortar" a pasta.</p>
 *
 * <p>{@code @EnableScheduling} liga os jobs agendados (carrinho abandonado e relay
 * do outbox).</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class SalesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesApplication.class, args);
    }
}
