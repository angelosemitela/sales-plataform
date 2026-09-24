package com.aalvarenga.sales.subscriber;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.aalvarenga.sales.subscriber.entity.SubscriberAddress;
import com.aalvarenga.sales.subscriber.entity.SubscriberDocument;
import com.aalvarenga.sales.subscriber.entity.SubscriberPhone;
import com.aalvarenga.sales.subscriber.repository.SubscriberDocumentRepository;
import com.aalvarenga.sales.subscriber.repository.SubscriberRepository;
import com.aalvarenga.sales.subscriber.validation.CpfValidator;

import lombok.RequiredArgsConstructor;

/**
 * Cadastro do assinante. Validação em duas camadas:
 * <ol>
 *   <li>FORMATO (Bean Validation nos DTOs) - já barrado antes de chegar aqui;</li>
 *   <li>REGRAS que dependem de dados (este service): e-mail já usado, documento x
 *       país permitido, CPF válido, documento já usado por outro assinante,
 *       endereço nacional x internacional.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SubscriberRegistrationService {

    private static final String BRAZIL = "BR";

    private final SubscriberRepository subscriberRepository;
    private final SubscriberDocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final CountryRepository countryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Subscriber register(RegisterSubscriberRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (subscriberRepository.existsByEmailIgnoreCase(email)) {
            throw BusinessException.conflict("EMAIL_ALREADY_REGISTERED", "E-mail already registered");
        }

        Subscriber subscriber = new Subscriber();
        subscriber.setExternalId(UUID.randomUUID());
        subscriber.setName(request.name().trim());
        subscriber.setEmail(email);
        subscriber.setPasswordHash(passwordEncoder.encode(request.password()));
        subscriber.setAuthorizedFallback(request.isAuthorizedFallback());

        subscriber.addDocument(buildDocument(request.document()));
        subscriber.addAddress(buildAddress(request.address()));
        subscriber.addPhone(buildPhone(request.phone()));

        return subscriberRepository.save(subscriber);
    }

    SubscriberDocument buildDocument(DocumentInput input) {
        String typeCode = input.type().trim().toUpperCase(Locale.ROOT);
        String countryCode = normalizeCountry(input.country());
        DocumentType type = documentTypeRepository.findById(typeCode)
                .orElseThrow(() -> BusinessException.badRequest("INVALID_DOCUMENT_TYPE", "Unknown document type " + typeCode));
        requireCountry(countryCode);
        if (!type.accepts(countryCode)) {
            throw BusinessException.badRequest("DOCUMENT_COUNTRY_NOT_ALLOWED",
                    "Country " + countryCode + " is not allowed for document type " + typeCode);
        }

        String value = input.value().trim();
        if (DocumentType.CPF.equals(typeCode) && !CpfValidator.isValid(value)) {
            throw BusinessException.badRequest("INVALID_CPF", "Invalid CPF");
        }
        if (documentRepository.existsByDocumentTypeAndCountryCodeAndValue(typeCode, countryCode, value)) {
            throw BusinessException.conflict("DOCUMENT_ALREADY_REGISTERED", "Document already registered");
        }

        SubscriberDocument document = new SubscriberDocument();
        document.setDocumentType(typeCode);
        document.setDescription(blankToNull(input.description()));
        document.setValue(value);
        document.setCountryCode(countryCode);
        return document;
    }

    SubscriberAddress buildAddress(AddressInput input) {
        String countryCode = normalizeCountry(input.country());
        requireCountry(countryCode);
        String zipCode = input.zipCode().trim();
        if (!input.international()) {
            // Endereço nacional: país sempre BR e CEP com 8 dígitos.
            if (!BRAZIL.equals(countryCode)) {
                throw BusinessException.badRequest("INVALID_ADDRESS_COUNTRY", "National address must be in BR");
            }
            zipCode = zipCode.replaceAll("\\D", "");
            if (!zipCode.matches("\\d{8}")) {
                throw BusinessException.badRequest("INVALID_ZIP_CODE", "Brazilian zip code must have 8 digits");
            }
        }

        SubscriberAddress address = new SubscriberAddress();
        address.setType(input.type());
        address.setDescription(input.description().trim());
        address.setInternational(input.international());
        address.setZipCode(zipCode);
        address.setAddressName(input.addressName().trim());
        address.setNumber(blankToNull(input.number()));
        address.setComplement(blankToNull(input.complement()));
        address.setDistrict(blankToNull(input.district()));
        address.setCity(blankToNull(input.city()));
        address.setState(blankToNull(input.state()));
        address.setCountryCode(countryCode);
        return address;
    }

    SubscriberPhone buildPhone(PhoneInput input) {
        Country country = requireCountry(normalizeCountry(input.country()));
        SubscriberPhone phone = new SubscriberPhone();
        phone.setCountryCode(country.getCode());
        phone.setDialCode(country.getDialCode());
        phone.setNumber(input.number());
        return phone;
    }

    private Country requireCountry(String code) {
        return countryRepository.findById(code)
                .orElseThrow(() -> BusinessException.badRequest("INVALID_COUNTRY", "Unknown country " + code));
    }

    private static String normalizeCountry(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
