package hr.algebra.cloudbased_inventory_management_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String TYPE_BASE = "https://example.com/errors/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "field", fieldError.getField(),
                        "message", defaultMessage(fieldError)
                ))
                .toList();

        ProblemDetail problem = buildProblem(HttpStatus.UNPROCESSABLE_ENTITY,
                "validation", "Validation failed", "Request validation failed", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem(HttpStatus.CONFLICT,
                "illegal-state", "Invalid state transition", ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String reason = status.getReasonPhrase();
        String slug = StringUtils.hasText(reason) ? reason.toLowerCase(Locale.ROOT).replace(' ', '-') : status.name().toLowerCase(Locale.ROOT);
        ProblemDetail problem = buildProblem(status, slug, status.getReasonPhrase(), ex.getReason(), request);
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleGeneral(Exception ex, HttpServletRequest request) {
        LOGGER.error("Unhandled exception", ex);
        ProblemDetail problem = buildProblem(HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error", "Internal server error", "An unexpected error occurred", request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail buildProblem(HttpStatus status, String slug, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_BASE + slug));
        if (StringUtils.hasText(detail)) {
            problem.setDetail(detail);
        }
        String requestUri = request.getRequestURI();
        if (StringUtils.hasText(requestUri)) {
            problem.setInstance(URI.create(requestUri));
        }
        String requestId = request.getHeader("X-Request-Id");
        if (!StringUtils.hasText(requestId)) {
            Object attribute = request.getAttribute("X-Request-Id");
            if (attribute instanceof String attributeValue && StringUtils.hasText(attributeValue)) {
                requestId = attributeValue;
            }
        }
        if (StringUtils.hasText(requestId)) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }

    private String defaultMessage(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        if (StringUtils.hasText(message)) {
            return message;
        }
        return "Invalid value";
    }
}

