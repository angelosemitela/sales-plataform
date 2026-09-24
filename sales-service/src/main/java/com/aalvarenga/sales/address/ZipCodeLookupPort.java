package com.aalvarenga.sales.address;

import java.util.Optional;

import com.aalvarenga.sales.address.dto.ZipCodeAddress;

/**
 * PORTA (arquitetura hexagonal) para consulta de CEP.
 *
 * <p>O resto do sistema só conhece esta interface; a ViaCEP é um detalhe de
 * implementação ({@link com.aalvarenga.sales.address.viacep.ViaCepZipCodeAdapter}).
 * Trocar por BrasilAPI, Correios ou uma base própria = escrever outro adapter,
 * sem tocar em controller/service. Também dá para encadear dois adapters (um
 * principal e um de contingência).</p>
 */
public interface ZipCodeLookupPort {

    /**
     * @param zipCodeDigits CEP com 8 dígitos, sem máscara
     * @return vazio quando o CEP não existe
     * @throws ZipCodeUnavailableException quando o provedor está fora do ar/lento
     */
    Optional<ZipCodeAddress> lookup(String zipCodeDigits);
}
