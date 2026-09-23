package com.marcos.leairning.email;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TemplateEngine templateEngine;

    public String render(String templateName, Map<String, Object> variables) {
        return process(templateName, variables);
    }

    public String renderText(String templateName, Map<String, Object> variables) {
        return process(templateName + "-text", variables);
    }

    private String process(String name, Map<String, Object> variables) {
        var context = new Context();
        context.setVariables(variables);
        return templateEngine.process(name, context);
    }
}