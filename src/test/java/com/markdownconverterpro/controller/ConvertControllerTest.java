package com.markdownconverterpro.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ConvertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ResultActions convert(String markdown) throws Exception {
        return rawPost(objectMapper.writeValueAsString(Map.of("markdown", markdown)));
    }

    private ResultActions rawPost(String body) throws Exception {
        return mockMvc.perform(post("/api/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void convertsHeading() throws Exception {
        convert("# Hello World")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.html").value("<h1>Hello World</h1>"));
    }

    @Test
    void convertsBoldAndItalic() throws Exception {
        convert("**bold** and *italic*")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value("<p><strong>bold</strong> and <em>italic</em></p>"));
    }

    @Test
    void convertsListsBlockquotesTablesAndCodeBlocks() throws Exception {
        String markdown = "- one\n- two\n\n> quote\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n```js\nlet x = 1;\n```";

        convert(markdown)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value(containsString("<ul>")))
                .andExpect(jsonPath("$.html").value(containsString("<blockquote>")))
                .andExpect(jsonPath("$.html").value(containsString("<table>")))
                .andExpect(jsonPath("$.html").value(containsString("<pre><code class=\"language-js\">")));
    }

    @Test
    void convertsLinksAndImages() throws Exception {
        convert("[Site](https://example.com) ![Pic](https://example.com/p.png)")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value(containsString("href=\"https://example.com\"")))
                .andExpect(jsonPath("$.html").value(containsString("<img src=\"https://example.com/p.png\"")));
    }

    @Test
    void returnsEmptyHtmlForEmptyMarkdown() throws Exception {
        convert("")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value(""));
    }

    @Test
    void sanitizesScriptTagsAndEventHandlers() throws Exception {
        convert("<script>alert(1)</script> <img src=x onerror=alert(1)> [x](javascript:alert(1))")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value(not(containsString("<script"))))
                .andExpect(jsonPath("$.html").value(not(containsString("<img"))))
                .andExpect(jsonPath("$.html").value(not(containsString("href"))));
    }

    @Test
    void rejectsMissingMarkdownField() throws Exception {
        rawPost("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.markdown").value("markdown is required"));
    }

    @Test
    void rejectsNullMarkdownValue() throws Exception {
        rawPost("{\"markdown\": null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.markdown").exists());
    }

    @Test
    void rejectsMarkdownLongerThanTheLimit() throws Exception {
        convert("a".repeat(200_001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.markdown").value(containsString("200000")));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        rawPost("{not valid json")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("not valid JSON")));
    }

    @Test
    void rejectsEmptyRequestBody() throws Exception {
        rawPost("")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/api/convert")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("# Hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void rejectsWrongHttpMethod() throws Exception {
        mockMvc.perform(get("/api/convert"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void returnsJsonNotFoundForUnknownApiPath() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }

    @Test
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("markdown-to-html-converter-pro"));
    }

    @Test
    void servesTheFrontendFromStaticResources() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Markdown to HTML Converter Pro")));
    }
}
