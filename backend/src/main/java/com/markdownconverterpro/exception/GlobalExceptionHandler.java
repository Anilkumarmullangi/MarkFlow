package com.markdownconverterpro.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.markdownconverterpro.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON",
                request, null, headers);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return build(status, messageFor(status), request, null, headers);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error while handling request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null,
                new HttpHeaders());
    }

    private ResponseEntity<Object> build(HttpStatus status,
                                         String message,
                                         WebRequest request,
                                         Map<String, String> fieldErrors,
                                         HttpHeaders sourceHeaders) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(sourceHeaders);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String path = request.getDescription(false).replaceFirst("^uri=", "");
        ErrorResponse body = ErrorResponse.of(status, message, path, fieldErrors);
        return new ResponseEntity<>(body, headers, status);
    }

    private static String messageFor(HttpStatus status) {
        return switch (status) {
            case METHOD_NOT_ALLOWED -> "HTTP method not allowed for this endpoint";
            case UNSUPPORTED_MEDIA_TYPE -> "Unsupported content type; send application/json";
            case NOT_FOUND -> "Resource not found";
            case NOT_ACCEPTABLE -> "Requested response format is not supported";
            case BAD_REQUEST -> "Bad request";
            default -> status.getReasonPhrase();
        };
    }
}
