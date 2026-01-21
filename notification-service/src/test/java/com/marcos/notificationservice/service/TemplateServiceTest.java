package com.marcos.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TemplateServiceTest {

    @Autowired
    private TemplateService templateService;

    @Test
    void shouldRenderHtmlTemplate() {
        var variables = Map.<String, Object>of("name", "John Doe", "username", "john.doe");
        var result = templateService.render("welcome-email", variables);
        assertThat(result).contains("John Doe");
        assertThat(result).contains("john.doe");
        assertThat(result).contains("Bienvenido a LeAIrning");
    }

    @Test
    void shouldRenderTextTemplate() {
        var variables = Map.<String, Object>of("name", "John Doe", "username", "john.doe");
        var result = templateService.renderText("welcome-email", variables);
        assertThat(result).contains("John Doe");
        assertThat(result).contains("john.doe");
        assertThat(result).doesNotContain("<html");
    }
}