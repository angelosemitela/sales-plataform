package com.aalvarenga.sales.subscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.aalvarenga.sales.reference.entity.Country;
import com.aalvarenga.sales.reference.entity.DocumentType;
import com.aalvarenga.sales.reference.repository.CountryRepository;
import com.aalvarenga.sales.reference.repository.DocumentTypeRepository;
import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.subscriber.dto.AddressInput;
import com.aalvarenga.sales.subscriber.dto.DocumentInput;
import com.aalvarenga.sales.subscriber.dto.PhoneInput;
import com.aalvarenga.sales.subscriber.dto.RegisterSubscriberRequest;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.subscriber.repository.SubscriberDocumentRepository;
import com.aalvarenga.sales.subscriber.repository.SubscriberRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriberRegistrationServiceTest {

    private static final Country BR = new Country("BR", "Brasil", "+55");
    private static final Country US = new Country("US", "Estados Unidos", "+1");
    private static final Country DE = new Country("DE", "Alemanha", "+49");

    @Mock
    private SubscriberRepository subscriberRepository;
    @Mock
    private SubscriberDocumentRepository documentRepository;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private SubscriberRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new SubscriberRegistrationService(subscriberRepository, documentRepository, documentTypeRepository,
                countryRepository, passwordEncoder);
        when(countryRepository.findById("BR")).thenReturn(Optional.of(BR));
        when(countryRepository.findById("US")).thenReturn(Optional.of(US));
        when(countryRepository.findById("DE")).thenReturn(Optional.of(DE));
        when(documentTypeRepository.findById("CPF")).thenReturn(Optional.of(new DocumentType("CPF", false, false, Set.of(BR))));
        when(documentTypeRepository.findById("UE")).thenReturn(Optional.of(new DocumentType("UE", true, false, Set.of(DE))));
        when(documentTypeRepository.findById("PASSPORT")).thenReturn(Optional.of(new DocumentType("PASSPORT", true, true, Set.of())));
        when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}hash");
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_validRequest_buildsTheWholeAggregate() {
        Subscriber saved = service.register(request("Angelo@Example.com", cpf("52998224725"), nationalAddress(), phone("BR")));

        assertThat(saved.getExternalId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("angelo@example.com"); // normalizado
        assertThat(saved.getPasswordHash()).isEqualTo("{bcrypt}hash");  // nunca a senha em texto
        assertThat(saved.isAuthorizedFallback()).isTrue();
        assertThat(saved.getDocuments()).singleElement().satisfies(d -> {
            assertThat(d.getDocumentType()).isEqualTo("CPF");
            assertThat(d.getSubscriber()).isSameAs(saved);
        });
        assertThat(saved.getAddresses()).singleElement().satisfies(a -> {
            assertThat(a.getZipCode()).isEqualTo("24220000");
            assertThat(a.getNumber()).isNull(); // "sem número" é permitido
        });
        assertThat(saved.getPhones()).singleElement().satisfies(p -> {
            assertThat(p.getDialCode()).isEqualTo("+55");
            assertThat(p.fullNumberDigits()).isEqualTo("5521999999999");
        });
    }

    @Test
    void register_emailAlreadyUsed_isConflict() {
        when(subscriberRepository.existsByEmailIgnoreCase("angelo@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request("angelo@example.com", cpf("52998224725"), nationalAddress(), phone("BR"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void register_invalidCpf_isBadRequest() {
        assertThatThrownBy(() -> service.register(request("a@b.com", cpf("52998224724"), nationalAddress(), phone("BR"))))
                .hasFieldOrPropertyWithValue("code", "INVALID_CPF");
    }

    @Test
    void register_cpfFromAnotherCountry_isBadRequest() {
        DocumentInput cpfUs = new DocumentInput("CPF", null, "52998224725", "US");

        assertThatThrownBy(() -> service.register(request("a@b.com", cpfUs, nationalAddress(), phone("BR"))))
                .hasFieldOrPropertyWithValue("code", "DOCUMENT_COUNTRY_NOT_ALLOWED");
    }

    @Test
    void register_euDocumentOutsideTheEu_isBadRequest() {
        DocumentInput ueUs = new DocumentInput("UE", null, "X123", "US");

        assertThatThrownBy(() -> service.register(request("a@b.com", ueUs, nationalAddress(), phone("BR"))))
                .hasFieldOrPropertyWithValue("code", "DOCUMENT_COUNTRY_NOT_ALLOWED");
    }

    @Test
    void register_passportAcceptsAnyCountry() {
        DocumentInput passport = new DocumentInput("PASSPORT", null, "FX123456", "US");

        Subscriber saved = service.register(request("a@b.com", passport, nationalAddress(), phone("US")));

        assertThat(saved.getDocuments().getFirst().getCountryCode()).isEqualTo("US");
        assertThat(saved.getPhones().getFirst().fullNumberDigits()).isEqualTo("121999999999");
    }

    @Test
    void register_documentAlreadyUsed_isConflict() {
        when(documentRepository.existsByDocumentTypeAndCountryCodeAndValue("CPF", "BR", "52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request("a@b.com", cpf("52998224725"), nationalAddress(), phone("BR"))))
                .hasFieldOrPropertyWithValue("code", "DOCUMENT_ALREADY_REGISTERED");
    }

    @Test
    void register_nationalAddressOutsideBrazil_isBadRequest() {
        AddressInput address = new AddressInput(AddressType.RESIDENCIAL, "Casa", false, "24220000", "Rua Uno",
                null, null, null, null, null, "US");

        assertThatThrownBy(() -> service.register(request("a@b.com", cpf("52998224725"), address, phone("BR"))))
                .hasFieldOrPropertyWithValue("code", "INVALID_ADDRESS_COUNTRY");
    }

    @Test
    void register_internationalAddress_keepsFreeZipCode() {
        AddressInput address = new AddressInput(AddressType.COMERCIAL, "Office", true, "10115", "Invalidenstr.",
                "117", null, null, "Berlin", null, "DE");

        Subscriber saved = service.register(request("a@b.com", cpf("52998224725"), address, phone("BR")));

        assertThat(saved.getAddresses().getFirst().getZipCode()).isEqualTo("10115");
        assertThat(saved.getAddresses().getFirst().isInternational()).isTrue();
    }

    private static RegisterSubscriberRequest request(String email, DocumentInput document, AddressInput address, PhoneInput phone) {
        return new RegisterSubscriberRequest("Angelo Alvarenga", email, "s3cr3t-pass", true, document, address, phone);
    }

    private static DocumentInput cpf(String value) {
        return new DocumentInput("CPF", null, value, "BR");
    }

    private static AddressInput nationalAddress() {
        return new AddressInput(AddressType.RESIDENCIAL, "Casa", false, "24220-000", "Rua Uno",
                "", null, "Icaraí", "Niterói", "RJ", "BR");
    }

    private static PhoneInput phone(String country) {
        return new PhoneInput(country, "21999999999");
    }
}
