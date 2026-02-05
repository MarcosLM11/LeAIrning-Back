package com.marcos.leairning.ai.chat;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

@Configuration
public class LogBookConfig {

    @Bean
    RestClientCustomizer logbookCustomizer(
            LogbookClientHttpRequestInterceptor interceptor) {
        return restClient -> restClient.requestInterceptor(interceptor);
    }
}
