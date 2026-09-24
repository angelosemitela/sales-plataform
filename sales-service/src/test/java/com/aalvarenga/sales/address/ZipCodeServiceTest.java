package com.aalvarenga.sales.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.aalvarenga.sales.address.dto.ZipCodeAddress;
import com.aalvarenga.sales.shared.error.BusinessException;

/** Testa o service contra a PORTA mockada - nenhuma chamada real à ViaCEP. */
@ExtendWith(MockitoExtension.class)
class ZipCodeServiceTest {

    @Mock
    private ZipCodeLookupPort port;
    @InjectMocks
    private ZipCodeService service;

    @Test
    void lookup_acceptsMaskedZipCode() {
        ZipCodeAddress address = new ZipCodeAddress("24220000", "Rua X", "Icaraí", "Niterói", "RJ", "BR");
        when(port.lookup("24220000")).thenReturn(Optional.of(address));

        assertThat(service.lookup("24220-000")).isEqualTo(address);
    }

    @Test
    void lookup_invalidFormat_isBadRequest() {
        assertThatThrownBy(() -> service.lookup("1234"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_ZIP_CODE");
    }

    @Test
    void lookup_notFound_is404() {
        when(port.lookup("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookup("99999999"))
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void lookup_providerDown_is503SoTheFrontEnablesManualInput() {
        when(port.lookup("24220000")).thenThrow(new ZipCodeUnavailableException("timeout", null));

        assertThatThrownBy(() -> service.lookup("24220000"))
                .hasFieldOrPropertyWithValue("code", "ZIP_CODE_SERVICE_UNAVAILABLE")
                .hasFieldOrPropertyWithValue("status", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
