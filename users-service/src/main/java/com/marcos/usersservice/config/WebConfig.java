package com.marcos.usersservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .useVersionResolver(new InternalPathExcludingVersionResolver())
                .addSupportedVersions("1.0", "2.0")
                .setDefaultVersion("1.0")
                .setVersionParser(new ApiVersionParser());
    }

    private static class InternalPathExcludingVersionResolver implements ApiVersionResolver {
        private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

        @Override
        public String resolveVersion(HttpServletRequest request) {
            var path = URL_PATH_HELPER.getPathWithinApplication(request);
            if (!path.startsWith("/api/")) {
                return null;
            }
            var segments = path.split("/");
            if (segments.length > 2) {
                return segments[2];
            }
            return null;
        }
    }
}
