package com.marcos.leairning.etl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class EtlMetrics {

    private static final String METRIC_PROCESSED = "etl.document.processed";
    private static final String METRIC_PROCESSING_TIME = "etl.document.processing.time";
    private final MeterRegistry registry;

    public EtlMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordSuccess(Timer.Sample sample) {
        record(sample, "success");
    }

    public void recordFailure(Timer.Sample sample) {
        record(sample, "failure");
    }

    private void record(Timer.Sample sample, String status) {
        sample.stop(Timer.builder(METRIC_PROCESSING_TIME).tag("status", status).register(registry));
        Counter.builder(METRIC_PROCESSED).tag("status", status).register(registry).increment();
    }
}