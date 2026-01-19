package com.marcos.discoveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiscoveryServiceApplicationTest {

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
    void dashboardShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardShouldBeAccessibleWithAuth() throws Exception {
        mockMvc.perform(get("/").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    void eurekaEndpointsShouldNotRequireAuthentication() throws Exception {
        var response = mockMvc.perform(get("/eureka/apps"))
                .andReturn().getResponse();
        var status = response.getStatus();
        org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403);
    }
}