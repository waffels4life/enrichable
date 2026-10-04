package com.enrichable.logging;

import com.enrichable.config.LogConfig;
import com.enrichable.model.EnrichInformation;

import java.util.List;

/**
 * Infrastructure boundary for writing enriched exception reports.
 *
 * <p>The exception model depends on this contract rather than on a concrete
 * logging implementation. This allows file, console, database, remote, or
 * test loggers to be introduced without changing {@code EnrichableException}.</p>
 */
@FunctionalInterface
public interface EnrichLogger {

    /**
     * Writes the supplied enriched error information.
     *
     * @param informationList enriched error entries
     * @param thrownAt timestamp representing when the exception was created
     * @param config logging configuration
     * @return a generated registry code when enabled; otherwise {@code null}
     */
    String write(
            List<EnrichInformation> informationList,
            String thrownAt,
            LogConfig config
    );
}
