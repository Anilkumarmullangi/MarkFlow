package com.markdownconverterpro.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.markdownconverterpro.dto.ConvertRequest;
import com.markdownconverterpro.dto.ConvertResponse;
import com.markdownconverterpro.dto.HealthResponse;
import com.markdownconverterpro.service.MarkdownConversionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ConvertController {

    private final MarkdownConversionService conversionService;
    private final String applicationName;

    public ConvertController(MarkdownConversionService conversionService,
                             @Value("${spring.application.name}") String applicationName) {
        this.conversionService = conversionService;
        this.applicationName = applicationName;
    }

    @PostMapping(value = "/convert", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ConvertResponse convert(@Valid @RequestBody ConvertRequest request) {
        return new ConvertResponse(conversionService.convert(request.markdown()));
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.up(applicationName);
    }
}
