package com.marcos.leairning.util.template;

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
        var context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    public String renderText(String templateName, Map<String, Object> variables) {
        var context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName + "-text", context);
    }
}
