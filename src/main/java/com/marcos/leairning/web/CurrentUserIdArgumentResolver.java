package com.marcos.leairning.web;

import lombok.val;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.util.Objects;
import java.util.UUID;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Objects.equals(parameter.getParameterType(), UUID.class);
    }

    @Override
    public UUID resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        val principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return doResolve(jwt);
        }
        return null;
    }

    private UUID doResolve(Jwt jwt) {
        val sub = jwt.getClaimAsString("sub");
        if (isBlank(sub)) {
            return null;
        }
        return fromString(sub);
    }
}