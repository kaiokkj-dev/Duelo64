package com.duelo64.backend.shared.status;

import com.duelo64.backend.shared.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatusController.class)
@Import(SecurityConfig.class)
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnApplicationStatusWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("online"))
                .andExpect(jsonPath("$.application").value("Duelo64 API"));
    }
}
