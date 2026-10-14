package com.markdownconverterpro.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "converter.max-request-bytes=500")
@AutoConfigureMockMvc
class RequestSizeLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestBodiesLargerThanTheConfiguredLimit() throws Exception {
        String body = "{\"markdown\":\"" + "x".repeat(1000) + "\"}";

        mockMvc.perform(post("/api/convert").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    void acceptsRequestBodiesWithinTheLimit() throws Exception {
        mockMvc.perform(post("/api/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"markdown\":\"# ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value("<h1>ok</h1>"));
    }
}
