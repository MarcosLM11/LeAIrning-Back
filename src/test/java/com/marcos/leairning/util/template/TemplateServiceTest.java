package com.marcos.leairning.util.template;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TemplateServiceTest {

    TemplateEngine templateEngine;
    TemplateService service;

    @BeforeEach
    void setUp() {
        templateEngine = mock(TemplateEngine.class);
        service = new TemplateService(templateEngine);
    }

    @Test
    void render_delegatesToTemplateEngine() {
        when(templateEngine.process(eq("my-template"), any(Context.class))).thenReturn("<html>rendered</html>");
        val result = service.render("my-template", Map.of("key", "value"));
        assertEquals("<html>rendered</html>", result);
        verify(templateEngine).process(eq("my-template"), any(Context.class));
    }

    @Test
    void renderText_appendsTextSuffix() {
        when(templateEngine.process(eq("my-template-text"), any(Context.class))).thenReturn("plain text");
        val result = service.renderText("my-template", Map.of("key", "value"));
        assertEquals("plain text", result);
        verify(templateEngine).process(eq("my-template-text"), any(Context.class));
    }
}
