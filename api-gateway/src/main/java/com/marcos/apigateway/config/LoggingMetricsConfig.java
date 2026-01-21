package com.marcos.apigateway.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingMetricsConfig {

    private final MeterRegistry meterRegistry;
    private Counter errorCounter;
    private Counter warnCounter;
    private Counter infoCounter;

    public LoggingMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        errorCounter = Counter.builder("log.events")
            .tag("level", "ERROR")
            .description("Number of ERROR log events")
            .register(meterRegistry);
        warnCounter = Counter.builder("log.events")
            .tag("level", "WARN")
            .description("Number of WARN log events")
            .register(meterRegistry);
        infoCounter = Counter.builder("log.events")
            .tag("level", "INFO")
            .description("Number of INFO log events")
            .register(meterRegistry);
        var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        var metricsAppender = new MetricsAppender();
        metricsAppender.setContext(context);
        metricsAppender.start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(metricsAppender);
    }

    private class MetricsAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            switch (event.getLevel().toInt()) {
                case Level.ERROR_INT -> errorCounter.increment();
                case Level.WARN_INT -> warnCounter.increment();
                case Level.INFO_INT -> infoCounter.increment();
            }
        }
    }
}
