# Enrichable

Java exceptions were starting to drive me a little crazy.

A message, a stack trace, maybe a cause... and when an error gets a little more complicated, things can get messy pretty quickly.

So I decided to make them a bit more organized.

**Enrichable** is a small Java library for creating exceptions that can carry more useful information, metadata, error levels, and configuration without turning the code into a mess.

It's still a work in progress, but it's slowly becoming something useful.

---

## Project Structure

```text
src/main/java/com/enrichable/
│   EnrichableException.java        ← Core exception and public API
│   Main.java                       ← Entry point / usage examples
│
├── annotation/
│   ├── AnnotationProcessor.java    ← Reads Enrichable annotations via reflection
│   ├── EnrichableCode.java         ← Annotation for custom exception classes
│   └── EnrichableHandler.java      ← Annotation for service classes
│
├── config/
│   ├── ConsoleConfig.java          ← Controls console output formatting
│   ├── ErrorLevel.java             ← INFO / WARNING / ERROR / CRITICAL
│   └── LogConfig.java              ← Controls file logging behavior
│
├── formatter/
│   ├── EnrichFormatter.java        ← Formatting contract
│   └── DefaultEnrichFormatter.java ← Default formatter implementation
│
├── logging/
│   ├── EnrichLogger.java           ← Logging infrastructure contract
│   └── FileEnrichLogger.java       ← Default thread-safe file logger
│
├── model/
│   └── EnrichInformation.java      ← Model for a single error entry
│
├── registry/
│   └── ErrorRegistry.java          ← Generates and stores error codes
│
└── validation/
    └── EnrichValidator.java        ← Validates and normalizes input values
```

The logging package intentionally separates the logging contract from its default file-based implementation:

```text
EnrichableException
        │
        ▼
  EnrichLogger
        ▲
        │
FileEnrichLogger
```

This keeps the exception model independent from a specific logging destination.

---

## Features

* Multiple error information entries
* Builder API for readable exception creation
* Error levels
* Custom metadata
* Configurable console output
* Configurable file logging
* Pluggable logging infrastructure
* Optional timestamps
* Optional error count
* Optional metadata output
* Exception cause preservation
* Thread-safe file logging
* Error-level log filtering
* Custom log file paths
* Optional log file clearing
* Input validation
* JUnit 5 tests
* Annotation-based exception creation
* Thread-safe exception building
* Unique error code generation per exception session
* Error code registry with lookup support

---

## Requirements

* Java 21+
* Maven 3+

---

## Installation

Clone the repository:

```bash
git clone https://github.com/waffels4life/enrichable.git

cd enrichable
```

Run the tests:

```bash
mvn test
```

---

## Quick Start

Create an `EnrichableException` using the Builder API:

```java
EnrichableException exception =
        new EnrichableException.Builder(
                "DATABASE",
                "Database connection failed"
        )
        .code("DB-001")
        .level(ErrorLevel.CRITICAL)
        .cause(new IllegalStateException("Connection refused."))
        .build();

System.out.println(exception);
```

The Builder requires `context` and `message`. `code`, `level`, and `cause` are optional. If no level is specified, `ErrorLevel.ERROR` is used.

---

## Builder API

The Builder API provides a readable way to create `EnrichableException` instances without relying on a long constructor.

| Field     | Required | Default |
| --------- | -------- | ------- |
| `context` | Yes      | —       |
| `message` | Yes      | —       |
| `code`    | No       | `null`  |
| `level`   | No       | `ERROR` |
| `cause`   | No       | `null`  |

Each Builder method returns the same Builder instance, allowing method chaining.

---

## Adding Information

Sometimes one error isn't enough. You can attach additional information to the same exception:

```java
exception.addInformation(
        "AUTH_SERVICE",
        "AUTH-001",
        "Authentication failed",
        ErrorLevel.WARNING
);
```

Each `EnrichInformation` entry keeps its own context, code, message, level, timestamp, and metadata.

---

## Metadata

Metadata belongs to a specific `EnrichInformation` entry.

The older `EnrichableException.addMetadata(...)` convenience method is deprecated. The preferred API is to add metadata directly to the relevant `EnrichInformation` instance:

```java
exception.getInformationList()
        .getLast()
        .addMetadata("userId", "1042");
```

This makes the ownership of metadata explicit and avoids coupling metadata operations to the exception's internal "latest entry" state.

Whether metadata appears in console output is controlled by `ConsoleConfig`.

```java
exception.setConsoleConfig(
        new ConsoleConfig()
                .showMetadata(true)
);
```

---

## Console Configuration

`ConsoleConfig` controls what information is included in the formatted console representation of the exception.

```java
ConsoleConfig configuration =
        new ConsoleConfig()
                .showTimestamp(true)
                .showErrorLevel(true)
                .showErrorCount(true)
                .showMetadata(true);

exception.setConsoleConfig(configuration);
```

| Option           | What it does                           | Default |
| ---------------- | -------------------------------------- | ------- |
| `showTimestamp`  | Shows timestamps for error information | `true`  |
| `showErrorLevel` | Shows the error level                  | `true`  |
| `showErrorCount` | Shows the total number of errors       | `true`  |
| `showMetadata`   | Shows metadata attached to errors      | `true`  |

---

## Logging

`EnrichableException` writes reports through the `EnrichLogger` infrastructure contract:

```java
exception.writeLog();
```

The default logger is `FileEnrichLogger`, so existing applications still get file-based logging without additional configuration.

By default, the log file is:

```text
enrichable.log
```

File logging is thread-safe, so concurrent exceptions can safely write to the shared log file without interleaving their reports.

### Custom Logger

The logging implementation can be replaced when an application needs another destination or format.

```java
exception.setLogger((information, thrownAt, config) -> {
    System.out.println("Custom logger received " + information.size() + " entries");
    return null;
});

exception.writeLog();
```

`EnrichLogger` is a functional interface, so lambdas are supported. This extension point can be used for custom console logging, JSON output, database persistence, remote logging, or test doubles without changing `EnrichableException` itself.

The dependency direction is intentionally:

```text
EnrichableException → EnrichLogger ← FileEnrichLogger
```

`EnrichableException` depends on the contract, while `FileEnrichLogger` provides the default infrastructure implementation.

---

## Log Configuration

File logging can be configured independently from console output using `LogConfig`.

```java
LogConfig logConfig =
        new LogConfig()
                .showTimestamp(true)
                .showErrorLevel(true)
                .showMetadata(true)
                .filePath("application.log");

exception.setLogConfig(logConfig);
exception.writeLog();
```

`LogConfig` currently provides:

| Option             | What it does                            | Default                   |
|--------------------|-----------------------------------------|---------------------------|
| `showTimestamp`    | Shows timestamps in the log report      | `true`                    |
| `showErrorLevel`   | Shows error levels                      | `true`                    |
| `showMetadata`     | Shows metadata                          | `true`                    |
| `filePath`         | Changes the log file path               | `enrichable.log`          |
| `clearBeforeWrite` | Clears the existing file before writing | `false`                   |
| `generateCode`     | Generates a unique error code           | `false`                   |
| `registryPath`     | Changes the registry file path           | `enrichable-registry.log` |

Console and logging configuration are independent.

---

## Error Code Registry

When `generateCode` is enabled, each call to `writeLog()` generates a 6-character code for that exception session and stores the full report in a separate registry file.

```java
exception.setLogConfig(
        new LogConfig()
                .generateCode(true)
);

String code = exception.writeLog();
System.out.println("Error code: " + code); // af45cb
```

The registry file defaults to:

```text
enrichable-registry.log
```

It can be changed with `registryPath()`:

```java
exception.setLogConfig(
        new LogConfig()
                .generateCode(true)
                .registryPath("logs/registry.log")
);
```

### Lookup

You can retrieve a previously logged report by its code:

```java
LogConfig config = new LogConfig()
        .generateCode(true);

ErrorRegistry.getInstance()
        .lookup("af45cb", config)
        .ifPresent(System.out::println);
```

`lookup()` returns an `Optional<String>` — empty if the code does not exist or the registry file cannot be read.

### How Codes Are Generated

Each code is the first 6 characters of a SHA-256 hash computed from the exception content and timestamp. The timestamp makes identical exception runs produce different codes in normal operation.

This is a readability and lookup feature, not a security guarantee.

---

## Log-Level Filtering

`LogConfig` can filter which errors are written to the log file.

### `onlyLevel()`

```java
exception.setLogConfig(
        new LogConfig()
                .onlyLevel(ErrorLevel.CRITICAL)
);

exception.writeLog();
```

Only entries matching the selected level are written.

### `minimumLevel()`

```java
exception.setLogConfig(
        new LogConfig()
                .minimumLevel(ErrorLevel.ERROR)
);

exception.writeLog();
```

With the current ordering:

```text
INFO < WARNING < ERROR < CRITICAL
```

`minimumLevel(ErrorLevel.ERROR)` logs `ERROR` and `CRITICAL` entries while excluding `WARNING` and `INFO`.

`onlyLevel()` and `minimumLevel()` are mutually exclusive. Setting one clears the other.

---

## Custom Log File

You can change the destination of the default file logger:

```java
exception.setLogConfig(
        new LogConfig()
                .filePath("application.log")
);

exception.writeLog();
```

---

## Clearing Previous Logs

By default, writing to a log file appends the new report. To replace the existing file before writing:

```java
exception.setLogConfig(
        new LogConfig()
                .clearBeforeWrite(true)
);

exception.writeLog();
```

---

## Error Levels

There are currently four levels:

```java
ErrorLevel.INFO
ErrorLevel.WARNING
ErrorLevel.ERROR
ErrorLevel.CRITICAL
```

If no level is explicitly specified, the Builder uses `ErrorLevel.ERROR`.

---

## Exception Cause

You can pass the original exception as the cause using the Builder API:

```java
IllegalStateException cause =
        new IllegalStateException("Connection refused.");

EnrichableException exception =
        new EnrichableException.Builder(
                "DATABASE",
                "Database operation failed"
        )
        .code("DB-001")
        .level(ErrorLevel.CRITICAL)
        .cause(cause)
        .build();
```

The original cause is preserved and can still be retrieved normally with `exception.getCause()`.

---

## Annotations

Instead of repeating context, codes, and levels every time, you can define them once on the class itself.

`@EnrichableHandler` goes on service classes:

```java
@EnrichableHandler(
        context = "Database",
        defaultLevel = ErrorLevel.CRITICAL
)
public class DatabaseService {

    public void connect() {
        throw AnnotationProcessor.processHandler(
                DatabaseService.class,
                "DB-001",
                "Connection failed"
        );
    }
}
```

`@EnrichableCode` goes on custom exception classes:

```java
@EnrichableCode(
        code = "DB-001",
        level = ErrorLevel.CRITICAL
)
public class DatabaseConnectionException
        extends EnrichableException {
    // ...
}
```

The annotated values are picked up automatically by `AnnotationProcessor`.

---

## Validation

The library performs input validation so invalid information does not silently make its way into an exception.

Required text values such as `context` and `message` cannot be `null` or blank. The optional `code`, when provided, also cannot be blank.

Metadata is validated and normalized by `EnrichValidator` when added through the `EnrichInformation` API.

Configuration values are also validated. For example, null error levels and invalid log file paths are rejected.

---

## Testing

The project uses JUnit 5.

Run all tests with:

```bash
mvn test
```

The test suite covers the Builder API, validation, metadata, console configuration, logging configuration, exception causes, multiple error entries, output behavior, file logging, log-level filtering, registry behavior, concurrency, and the pluggable `EnrichLogger` contract.

---

## Example

Here's a more complete example using the default file logger:

```java
EnrichableException databaseError =
        new EnrichableException.Builder(
                "DATABASE",
                "Failed to execute query: table 'users' not found"
        )
        .code("DB-001")
        .level(ErrorLevel.CRITICAL)
        .cause(new IllegalStateException(
                "Table 'users' does not exist."
        ))
        .build();

databaseError.getInformationList()
        .getLast()
        .addMetadata("userId", "1042");

databaseError.getInformationList()
        .getLast()
        .addMetadata("query", "SELECT * FROM users");

databaseError.getInformationList()
        .getLast()
        .addMetadata("retryCount", "3");

databaseError.setConsoleConfig(
        new ConsoleConfig()
                .showTimestamp(true)
                .showErrorLevel(true)
                .showErrorCount(true)
                .showMetadata(true)
);

databaseError.setLogConfig(
        new LogConfig()
                .minimumLevel(ErrorLevel.ERROR)
                .showTimestamp(true)
                .showErrorLevel(true)
                .showMetadata(true)
                .filePath("enrichable.log")
);

System.out.println(databaseError);
databaseError.writeLog();
```

---

## Backward Compatibility

Some older methods are still present for compatibility with earlier versions of the library.

For example:

```java
exception
        .onlyLog(ErrorLevel.CRITICAL)
        .writeLog();
```

is still supported, but `onlyLog()` is deprecated. The recommended API is:

```java
exception.setLogConfig(
        new LogConfig()
                .onlyLevel(ErrorLevel.CRITICAL)
);

exception.writeLog();
```

Similarly, `EnrichableException.addMetadata(...)` remains available for compatibility but is deprecated. New code should add metadata directly to the relevant `EnrichInformation` entry.

The Builder API is the recommended way to create new `EnrichableException` instances.

---

## Why EnrichableException?

Java already provides `Throwable.addSuppressed()` for attaching additional exceptions to a throwable. That's useful, but it solves a different problem.

| Feature                                 | `Throwable.addSuppressed()`       | `EnrichableException`            |
|-----------------------------------------|-----------------------------------|----------------------------------|
| Attach another `Throwable`              | Yes                               | Yes, through the exception cause |
| Add structured error information        | No                                | Yes                              |
| Error context                           | No                                | Yes                              |
| Error code                              | No                                | Yes                              |
| Error level                             | No                                | Yes                              |
| Timestamp per error                     | No                                | Yes                              |
| Custom metadata                         | No                                | Yes                              |
| Multiple related error entries          | Limited to suppressed exceptions  | Yes                              |
| Configurable output                     | No                                | Yes                              |
| Formatted error report                  | No                                | Yes                              |
| Designed for structured error reporting | No                                | Yes                              |
| Annotation-based exception creation     | No                                | Yes                              |
| Unique error code per session           | No                                | Yes                              |
| Error code registry with lookup         | No                                | Yes                              |
| Pluggable logging                       | No                                | Yes                              |

`addSuppressed()` is mainly useful when one operation encounters additional exceptions that should not replace the original exception.

`EnrichableException` is designed for a different job: **making errors carry structured, human-readable context that can be logged and inspected later.**

---

## Project Status

This project is actively evolving as I continue exploring better ways to design and manage exceptions in Java.

It is still a work in progress, and the API and design may change as the project grows.
