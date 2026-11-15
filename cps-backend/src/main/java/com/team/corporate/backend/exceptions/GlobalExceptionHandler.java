package com.team.corporate.backend.exceptions;

import com.team.corporate.core.exceptions.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INTERNAL_ERROR_DETAIL = "Не удалось выполнить операцию. Повторите попытку позже.";
    private static final String UNAVAILABLE_DETAIL = "Сервис временно недоступен. Повторите попытку позже.";
    private static final String VALIDATION_DETAIL = "Запрос содержит ошибки валидации.";

    @NonNull
    private static String validationMessage(@NonNull MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return message == null || message.isBlank() ? "Недопустимое значение." : message;
    }

    @NonNull
    private static String parameterName(@NonNull MethodParameter parameter) {
        String name = null;
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        if (requestParam != null) {
            name = requestParam.name().isBlank() ? requestParam.value() : requestParam.name();
        } else if (pathVariable != null) {
            name = pathVariable.name().isBlank() ? pathVariable.value() : pathVariable.name();
        } else if (requestHeader != null) {
            name = requestHeader.name().isBlank() ? requestHeader.value() : requestHeader.name();
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        name = parameter.getParameterName();
        return name == null ? "arg" + parameter.getParameterIndex() : name;
    }

    @ExceptionHandler(AuthenticationException.class)
    @Nullable
    public ResponseEntity<Object> handleAuthentication(
            @NonNull AuthenticationException exception, @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.UNAUTHORIZED, "Требуется вход с верным именем пользователя и паролем.", request);
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    @Nullable
    public ResponseEntity<Object> handleAuthenticationService(
            @NonNull AuthenticationServiceException exception, @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_DETAIL, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @Nullable
    public ResponseEntity<Object> handleAccessDenied(
            @NonNull AccessDeniedException exception, @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.FORBIDDEN, "Недостаточно прав для выполнения операции.", request);
    }

    @ExceptionHandler({
            CategoryNotFoundException.class,
            InventoryNotFoundException.class,
            OrderItemNotFoundException.class,
            OrderNotFoundException.class,
            ProductNotFoundException.class,
            UserNotFoundException.class,
            WarehouseNotFoundException.class
    })
    @Nullable
    public ResponseEntity<Object> handleNotFound(
            @NonNull RuntimeException exception,
            @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @Nullable
    public ResponseEntity<Object> handleInvalidArgument(
            @NonNull IllegalArgumentException exception,
            @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        List<ValidationError> errors = exception.getBindingResult().getAllErrors().stream()
                .map(error -> new ValidationError(
                        error instanceof FieldError fieldError ? fieldError.getField() : "$", validationMessage(error)))
                .toList();
        return validationProblem(exception, headers, status, errors, request);
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        if (exception.isForReturnValue()) {
            return super.handleHandlerMethodValidationException(exception, headers, status, request);
        }
        List<ValidationError> errors = new ArrayList<>();
        for (var result : exception.getParameterValidationResults()) {
            String parameter = parameterName(result.getMethodParameter());
            if (result.getContainerIndex() != null) {
                parameter += "[" + result.getContainerIndex() + "]";
            }
            for (var error : result.getResolvableErrors()) {
                String field = parameter;
                if (error instanceof FieldError fieldError) {
                    field = fieldError.getField();
                    if (result.getContainerIndex() != null) {
                        field = "[" + result.getContainerIndex() + "]." + field;
                    }
                } else if (result instanceof ParameterErrors) {
                    field = result.getContainerIndex() == null ? "$" : "[" + result.getContainerIndex() + "]";
                }
                errors.add(new ValidationError(field, validationMessage(error)));
            }
        }
        for (var error : exception.getCrossParameterValidationResults()) {
            errors.add(new ValidationError("$", validationMessage(error)));
        }
        return validationProblem(exception, headers, status, errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @Nullable
    public ResponseEntity<Object> handleConstraintViolation(
            @NonNull ConstraintViolationException exception,
            @NonNull WebRequest request
    ) {
        for (var violation : exception.getConstraintViolations()) {
            for (var node : violation.getPropertyPath()) {
                if (node.getKind() == ElementKind.RETURN_VALUE) {
                    return problem(exception, HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_DETAIL, request);
                }
            }
        }
        List<ValidationError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString().isBlank() ? "$" : violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        return validationProblem(exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, errors, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @Nullable
    public ResponseEntity<Object> handleDataIntegrity(
            @NonNull DataIntegrityViolationException exception,
            @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.CONFLICT, "Операция нарушает ограничения данных.", request);
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    @Nullable
    public ResponseEntity<Object> handleConcurrency(
            @NonNull ConcurrencyFailureException exception,
            @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.CONFLICT, "Данные изменяются другой операцией. Повторите попытку позже.", request);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, TransientDataAccessResourceException.class})
    @Nullable
    public ResponseEntity<Object> handleUnavailable(
            @NonNull RuntimeException exception,
            @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE_DETAIL, request);
    }

    @ExceptionHandler(Exception.class)
    @Nullable
    public ResponseEntity<Object> handleUnexpected(
            @NonNull Exception exception,
            @NonNull WebRequest request
    ) {
        return problem(exception, HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_DETAIL, request);
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception exception,
            @Nullable Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        if (status.is5xxServerError()) {
            LOGGER.error("Request failed: {}", request.getDescription(false), exception);
            String detail = status.value() == HttpStatus.SERVICE_UNAVAILABLE.value()
                    ? UNAVAILABLE_DETAIL : INTERNAL_ERROR_DETAIL;
            body = ProblemDetail.forStatusAndDetail(status, detail);
        }
        return super.handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    @NonNull
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        if (body instanceof ProblemDetail) {
            responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        }
        return super.createResponseEntity(body, responseHeaders, status, request);
    }

    @Nullable
    private ResponseEntity<Object> problem(
            @NonNull Exception exception,
            @NonNull HttpStatus status,
            @Nullable String detail,
            @NonNull WebRequest request) {
        String message = detail == null || detail.isBlank() ? status.getReasonPhrase() : detail;
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, message);
        return handleExceptionInternal(exception, body, new HttpHeaders(), status, request);
    }

    @Nullable
    private ResponseEntity<Object> validationProblem(
            @NonNull Exception exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull List<ValidationError> errors,
            @NonNull WebRequest request
    ) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, VALIDATION_DETAIL);
        body.setProperty("errors", errors.stream()
                .sorted(Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message))
                .toList());
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    public record ValidationError(@NonNull String field, @NonNull String message) {
    }
}
