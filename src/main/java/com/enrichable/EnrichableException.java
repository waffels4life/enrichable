package com.enrichable;

import com.enrichable.config.ConsoleConfig;
import com.enrichable.config.ErrorLevel;
import com.enrichable.config.LogConfig;
import com.enrichable.formatter.DefaultEnrichFormatter;
import com.enrichable.logging.EnrichLogger;
import com.enrichable.logging.FileEnrichLogger;
import com.enrichable.model.EnrichInformation;
import com.enrichable.validation.EnrichValidator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link RuntimeException} that carries structured, enriched error information.
 *
 * <p>Logging is exposed through the {@link EnrichLogger} infrastructure
 * contract. The default implementation is {@link FileEnrichLogger}, while
 * applications can provide another implementation when needed.</p>
 */
public class EnrichableException extends RuntimeException {
    private volatile ConsoleConfig consoleConfig = new ConsoleConfig();
    private volatile LogConfig logConfig = new LogConfig();
    private volatile EnrichLogger logger = FileEnrichLogger.getInstance();
    private final String thrownAt = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    private final List<EnrichInformation> informationList = new CopyOnWriteArrayList<>();

    /**
     * @deprecated Use {@link Builder} instead.
     */
    @Deprecated
    public EnrichableException(String context,
                               String code,
                               String message,
                               ErrorLevel level,
                               Throwable cause) {
        super(message, cause);
        addInformation(context, code, message, level);
    }

    /**
     * Constructs an exception from a {@link Builder}.
     */
    public EnrichableException(Builder builder) {
        super(builder.message, builder.throwable);
        addInformation(builder.context, builder.code, builder.message, builder.errorLevel);
    }

    /**
     * Builder for constructing {@link EnrichableException} instances.
     */
    public static class Builder {
        private final String context;
        private String code;
        private final String message;
        private ErrorLevel errorLevel = ErrorLevel.ERROR;
        private Throwable throwable;

        public Builder(String context, String message) {
            EnrichValidator.requireNonBlank(context, "context");
            EnrichValidator.requireNonBlank(message, "message");
            this.context = context;
            this.message = message;
        }

        public Builder code(String code) {
            EnrichValidator.requireNonBlank(code, "code");
            this.code = code;
            return this;
        }

        public Builder level(ErrorLevel errorLevel) {
            EnrichValidator.requireNonNull(errorLevel);
            this.errorLevel = errorLevel;
            return this;
        }

        public Builder cause(Throwable throwable) {
            EnrichValidator.requireNonNull(throwable, "cause");
            this.throwable = throwable;
            return this;
        }

        public EnrichableException build() {
            return new EnrichableException(this);
        }
    }

    public EnrichableException setConsoleConfig(ConsoleConfig consoleConfig) {
        EnrichValidator.requireNonNull(consoleConfig, "Console configuration");
        this.consoleConfig = consoleConfig;
        return this;
    }

    public EnrichableException setLogConfig(LogConfig logConfig) {
        EnrichValidator.requireNonNull(logConfig, "Log configuration");
        this.logConfig = logConfig;
        return this;
    }

    /**
     * Replaces the logger used by {@link #writeLog()}.
     *
     * <p>This is the infrastructure extension point for custom sinks such as
     * console, database, remote, structured JSON, or test loggers.</p>
     *
     * @param logger logger implementation to use
     * @return this exception, for method chaining
     * @throws IllegalArgumentException if {@code logger} is null
     */
    public EnrichableException setLogger(EnrichLogger logger) {
        EnrichValidator.requireNonNull(logger, "Logger");
        this.logger = logger;
        return this;
    }

    public synchronized EnrichableException addInformation(String context,
                                                           String code,
                                                           String message,
                                                           ErrorLevel level) {
        EnrichValidator.requireNonBlank(context, "Exception context");
        EnrichValidator.requireNonBlank(message, "Exception message");
        EnrichValidator.requireNonNull(level);
        if (code != null) EnrichValidator.requireNonBlank(code, "Exception code");
        informationList.add(new EnrichInformation(context, code, message, level));
        return this;
    }

    /**
     * @deprecated Use {@link EnrichInformation#addMetadata(String, String)} directly.
     */
    @Deprecated
    public synchronized EnrichableException addMetadata(String key, String value) {
        if (informationList.isEmpty())
            throw new IllegalStateException("Cannot add metadata without exception information.");
        informationList.getLast().addMetadata(
                EnrichValidator.normalizeMetadataKey(key),
                EnrichValidator.normalizeMetadataValue(value)
        );
        return this;
    }

    /**
     * @deprecated Use {@link #setLogConfig(LogConfig)} with
     * {@link LogConfig#onlyLevel(ErrorLevel)} instead.
     */
    @Deprecated
    public EnrichableException onlyLog(ErrorLevel level) {
        EnrichValidator.requireNonNull(level);
        this.logConfig.onlyLevel(level);
        return this;
    }

    /**
     * Writes the exception report through the configured {@link EnrichLogger}.
     *
     * @return the generated error code if enabled, otherwise {@code null}
     */
    public String writeLog() {
        return logger.write(informationList, thrownAt, logConfig);
    }

    @Override
    public synchronized String toString() {
        return new DefaultEnrichFormatter(consoleConfig).format(informationList);
    }

    public List<EnrichInformation> getInformationList() {
        return Collections.unmodifiableList(informationList);
    }
}
