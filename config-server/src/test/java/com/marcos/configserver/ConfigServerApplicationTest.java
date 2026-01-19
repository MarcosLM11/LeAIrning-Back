package com.marcos.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"native", "test"})
class ConfigServerApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void configEndpointShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/test-app/default"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configEndpointShouldBeAccessibleWithAuth() throws Exception {
        mockMvc.perform(get("/test-app/default")
                        .with(httpBasic("configadmin", "configsecret")))
                .andExpect(status().isOk());
    }

    @Test
    void encryptEndpointShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/encrypt")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("test-value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void encryptEndpointShouldBeAccessibleWithAuth() throws Exception {
        var result = mockMvc.perform(post("/encrypt")
                        .with(httpBasic("configadmin", "configsecret"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("test-value"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).isNotBlank();
    }

    @Test
    void decryptEndpointShouldWorkWithEncryptedValue() throws Exception {
        var encryptResult = mockMvc.perform(post("/encrypt")
                        .with(httpBasic("configadmin", "configsecret"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("secret-password"))
                .andExpect(status().isOk())
                .andReturn();
        var encrypted = encryptResult.getResponse().getContentAsString();
        var decryptResult = mockMvc.perform(post("/decrypt")
                        .with(httpBasic("configadmin", "configsecret"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(encrypted))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(decryptResult.getResponse().getContentAsString()).isEqualTo("secret-password");
    }
}