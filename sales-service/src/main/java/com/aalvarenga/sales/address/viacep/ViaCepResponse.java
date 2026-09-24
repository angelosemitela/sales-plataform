package com.aalvarenga.sales.address.viacep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Formato da resposta da <a href="https://viacep.com.br">ViaCEP</a>. Só os campos usados.
 * Para CEP inexistente a ViaCEP responde 200 com {@code {"erro": true}} (ou "true",
 * como texto, dependendo da versão) - por isso {@code erro} é {@code Object}.
 *
 * <p>Obs.: no Jackson 3 as ANOTAÇÕES continuam no pacote {@code com.fasterxml.jackson.annotation}.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(String cep, String logradouro, String bairro, String localidade, String uf, Object erro) {

    public boolean notFound() {
        return erro != null && Boolean.parseBoolean(String.valueOf(erro));
    }
}
