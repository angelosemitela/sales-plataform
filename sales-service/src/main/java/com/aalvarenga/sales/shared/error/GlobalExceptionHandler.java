package com.aalvarenga.sales.shared.error;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * Tradução de exceções em respostas HTTP no padrão <b>Problem Details (RFC 9457)</b>.
 *
 * <p>Diferença didática em relação ao billing: lá o contrato de erro era próprio
 * ({@code result/code/reason}), definido pelo enunciado. Aqui, como o contrato é
 * nosso, usamos o padrão da indústria que o Spring já suporta nativamente
 * ({@link ProblemDetail}): {@code type, title, status, detail} + extensões
 * ({@code code} e {@code errors}). Qualquer cliente HTTP moderno entende.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setProperty("code", ex.getCode());
        return problem;
    }

    /** Falha de Bean Validation ({@code @Valid}): devolve TODOS os campos com erro de uma vez. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
        problem.setProperty("code", "MALFORMED_REQUEST");
        return problem;
    }

    /**
     * Rede de segurança: uma constraint do banco (ex: e-mail único) violada numa
     * corrida entre duas requisições simultâneas, que passaram juntas pela checagem
     * da aplicação. Vira 409 em vez de 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data conflict, please retry");
        problem.setProperty("code", "DATA_CONFLICT");
        return problem;
    }

    /** Duas abas alterando o mesmo carrinho ao mesmo tempo ({@code @Version}). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.info("Concurrent update rejected: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The resource was changed by another request, please reload");
        problem.setProperty("code", "CONCURRENT_UPDATE");
        return problem;
    }
}
