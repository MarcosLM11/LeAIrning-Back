package com.marcos.leairning;

import com.marcos.leairning.logging.LoggingUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableAsync
@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class LeAIrningBackApplication {

    static {
        LoggingUtils.setupFlogger();
    }

    private LeAIrningBackApplication() {}

    static void main(String[] args) {
        SpringApplication.run(LeAIrningBackApplication.class, args);
    }

}
