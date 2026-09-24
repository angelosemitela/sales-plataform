package com.aalvarenga.sales.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.auth.dto.AuthResponse;
import com.aalvarenga.sales.auth.dto.LoginRequest;
import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.subscriber.repository.SubscriberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Login por e-mail e senha.
 *
 * <p>Boas práticas aplicadas:</p>
 * <ul>
 *   <li>Mesma mensagem para "e-mail não existe" e "senha errada" - não revela quais
 *       e-mails estão cadastrados (enumeração de usuários);</li>
 *   <li>Quando o e-mail não existe, ainda assim roda um {@code matches} contra um
 *       hash fictício: o tempo de resposta fica parecido nos dois casos (evita que
 *       a diferença de tempo denuncie o e-mail).</li>
 * </ul>
 * Estudo futuro: limite de tentativas (rate limit / bloqueio temporário), ex. com
 * Bucket4j, e MFA.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SubscriberRepository subscriberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    private volatile String dummyHash;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<Subscriber> subscriber = subscriberRepository.findByEmailIgnoreCase(request.email().trim());
        String hash = subscriber.map(Subscriber::getPasswordHash).orElseGet(this::dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (subscriber.isEmpty() || !passwordMatches || !"ACTIVE".equals(subscriber.get().getStatus())) {
            throw BusinessException.unauthorized("INVALID_CREDENTIALS", "Invalid e-mail or password");
        }
        return jwtTokenService.issue(subscriber.get());
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("timing-attack-protection");
        }
        return dummyHash;
    }
}
