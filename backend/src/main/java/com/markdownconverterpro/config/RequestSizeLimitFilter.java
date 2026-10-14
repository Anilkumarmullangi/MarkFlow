package com.markdownconverterpro.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.markdownconverterpro.dto.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final long maxRequestBytes;

    public RequestSizeLimitFilter(ObjectMapper objectMapper,
                                  @Value("${converter.max-request-bytes:1048576}") long maxRequestBytes) {
        this.objectMapper = objectMapper;
        this.maxRequestBytes = maxRequestBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getContentLengthLong() > maxRequestBytes) {
            ErrorResponse body = ErrorResponse.of(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Request body exceeds the maximum allowed size of " + maxRequestBytes + " bytes",
                    request.getRequestURI(),
                    null);
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
