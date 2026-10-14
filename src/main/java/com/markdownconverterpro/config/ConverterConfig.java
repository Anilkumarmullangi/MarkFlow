package com.markdownconverterpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.markdownconverterpro.sanitizer.HtmlSanitizer;
import com.markdownconverterpro.service.MarkdownConversionService;

@Configuration
public class ConverterConfig {

    @Bean
    public HtmlSanitizer htmlSanitizer() {
        return new HtmlSanitizer();
    }

    @Bean
    public MarkdownConversionService markdownConversionService(HtmlSanitizer htmlSanitizer) {
        return new MarkdownConversionService(htmlSanitizer);
    }
}
