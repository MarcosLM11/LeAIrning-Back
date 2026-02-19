package com.marcos.leairning.logging;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
public class LoggingConfiguration {

    @Bean
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration() {
        var registration = new FilterRegistrationBean<>(new RequestLoggingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
