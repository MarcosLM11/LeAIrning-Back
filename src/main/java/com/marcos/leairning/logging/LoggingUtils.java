package com.marcos.leairning.logging;

import com.google.common.flogger.backend.slf4j.Slf4jBackendFactory;
import lombok.NoArgsConstructor;
import static java.lang.System.setProperty;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class LoggingUtils {

    private static final String FLOGGER_FACTORY_INSTANCE = Slf4jBackendFactory.class.getName() + "#getInstance";

    public static void setupFlogger() {
        setProperty("flogger.backend_factory", FLOGGER_FACTORY_INSTANCE);
    }
}
