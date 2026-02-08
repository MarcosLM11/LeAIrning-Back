package com.marcos.leairning;

import com.marcos.leairning.util.logging.LoggingUtils;
import lombok.extern.flogger.Flogger;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Flogger
@EnableAsync
@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class LeAIrningBackApplication {

    static {
        LoggingUtils.setupFlogger();
    }

    static void main(String[] args) {
        SpringApplication.run(LeAIrningBackApplication.class, args);
    }

    @Bean
    @Profile("!test")
    ApplicationRunner go(FunctionCatalog catalog) {
        Runnable composedFunction = catalog.lookup(null);
        return _ -> {
            if (composedFunction != null) {
                composedFunction.run();
            } else {
                log.atWarning().log("No composed function found in the catalog");
            }
        };
    }

}
