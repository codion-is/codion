# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository, the Codion framework itself.

## Project Overview

Codion is a full-stack Java rich client desktop CRUD application framework based solely on Java Standard Edition components. It follows Domain-Driven Design principles with Entity-Relationship concepts rather than ORM. The framework has been continuously refined for over 20 years and is designed for internal business/scientific applications with 1-10 users, though it can handle thousands of concurrent users (see `documentation/src/docs/asciidoc/images/monitoring`).

**Key Technologies:**
- Java 21 (the bytecode target), the Java Platform Module System, a `module-info.java` per module
- Gradle build system
- Swing UI framework
- JUnit 5 for testing, H2 for the test databases
- Multiple database support: Db2, Derby, H2, HSQLDB, MariaDB, MySQL, Oracle, PostgreSQL, SQLite and SQL Server

## Design Philosophy

- **Observable/Reactive Patterns Throughout** - Everything is observable (`Value<T>`, `State`, `Event<T>`)
- **Builders All the Way Down** - Fluent API configuration for consistency and discoverability
- **Continuous Refinement** - ~20 years of development, 3+ years of polishing (see `changelog.md` for a few years worth of mostly polishing, 700+ renames, 460+ removals)
- **Pragmatic Over Dogmatic** - Built for real internal business applications
- **Keyboard First** - Everything reachable without a mouse
- **Descending from Peak Complexity** - See `documentation/src/docs/asciidoc/images/complexity.png` for metrics showing decreasing complexity over time

## Essential Commands

```bash
./gradlew build                                  # Full build: compile, test, javadoc
./gradlew build -x test                          # Build without tests
./gradlew test                                   # Run all tests
./gradlew :codion-common-utilities:test          # Test a single module
./gradlew test --tests "ClassName.methodName"    # Run a single test class or method
./gradlew spotlessCheck                          # Check the license headers
./gradlew spotlessApply                          # Add missing license headers
./gradlew :demo-chinook:runClientLocal           # Run the Chinook demo with a local connection
```

- **A change is done when the full `./gradlew build` passes.** Javadoc is part of it and catches broken `{@link}`s that compilation and tests miss.
- **Spotless only checks license headers,** on Java sources including `package-info.java` and `module-info.java`. Formatting and import order are kept by hand, following the surrounding code.

## Modules

```
common/
  utilities/       Configuration and PropertyValue, Operator, User, text and formatting utilities
  reactive/        Observable, Observer, Value, State, Event
  model/           UI-agnostic condition, filter, selection and table models, ProgressWorker
  db/              Database (dialect and JDBC plumbing), connection pools, database exceptions
  rmi/             Remote server and client base
  i18n/            Shared messages
framework/
  domain/          Entity modeling: entities, attributes, conditions, function, procedure and report types
  db/              EntityConnection, the connection API
  db-local/        JDBC based connection
  db-rmi/          RMI based connection
  db-http/         HTTP based connection
  json-domain/     JSON serialization of entities and conditions
  json-db/         JSON serialization of the connection requests and errors
  server/          EntityServer
  servlet/         HTTP endpoint
  model/           UI-agnostic entity models: edit, table, condition, combo box and search models
  domain-db/       Domain models from database metadata
  domain-test/     Domain unit test support
  model-test/      Model unit test support
  i18n/            Shared framework messages
swing/
  common-model/    Swing adapters of the common models
  common-ui/       Standalone Swing toolkit: component builders, controls, tables, dialogs
  framework-model/ Swing entity models
  framework-ui/    Entity panels, edit and table panels, application frame
dbms/              One module per database
plugins/           Connection pools, JasperReports, FlatLaf and themes, logging proxies
tools/             Domain generator, load testing, server monitor, Swing robot and MCP
demos/             chinook, employees, petclinic, petstore, world, manual (the manual's examples), and more
documentation/     The asciidoc manual, technical docs and tutorials
```

Project names follow the directories, `framework/db-local` being `:codion-framework-db-local`, apart from the plugins (`:codion-plugin-hikari-pool`), `tools/loadtest/core` (`:codion-tools-loadtest`), `tools/logging/jul-classpath` (`:codion-tools-jul-classpath`) and the demos (`:demo-chinook`). `settings.gradle` has the full list.

## Architecture

- **Layering:** `common` → `framework` → `swing`. The model layers, `common/model` and `framework/model`, are UI-agnostic, the Swing modules adapting them.
- **Entities:**
  - `Entity` represents a row, `EntityDefinition` holds the metadata of its type.
  - `Attribute<T>` identifies a typed value, `Column<T>` and `ForeignKey` being the persistent ones, `AttributeDefinition<T>`, `ColumnDefinition<T>` and `ForeignKeyDefinition` their metadata.
  - A domain model extends `DomainModel`, defining entities via `EntityType.as()`. Domains are found via `ServiceLoader` (`META-INF/services/is.codion.framework.domain.Domain`), or instantiated directly.
- **Connections:** `EntityConnection` provides the same API whether local (JDBC), RMI or HTTP.
- **Models and panels:**
  - `SwingEntityModel` coordinates a `SwingEntityEditModel` and a `SwingEntityTableModel`.
  - `EntityPanel` coordinates an `EntityEditPanel` and an `EntityTablePanel`.
  - Both nest via `detail()`, the same master-detail pattern at every level.
- **Services:** pluggable parts are found via `ServiceLoader`: `Domain`, `DatabaseFactory`, `ConnectionPoolFactory` and the like.
- **Security:** authentication and authorization are delegated to the database by default, typically with `schema_read`/`schema_write` roles. An `Authenticator` on the server can authenticate the user instead (see `ChinookAuthenticator`). The RMI server belongs behind a VPN, with SSL and a deserialization filter.

## Conventions

These are established across the codebase. New code follows them, and code that doesn't is a finding.

- **Static factory methods are named after the type they return,** for clean static imports: `State.state()`, `ReportType.reportType("name")`.
- **Accessors use `name()`/`name(value)`,** no get/set/is prefixes, except where a contract requires bean names (JMX MXBeans). A class accessor is named `type()`.
- **Builder methods take parameters,** `enabled(true)` rather than `enable()`, which keeps them uniform and mechanically generatable.
- **`Abstract*` names a public extension base.** Implementations are package-private wherever possible.
- **Composite input components are `*Input`, leaf components `*Field`,** for example `TemporalInput` wrapping a `TemporalField`.
- **Configure rather than extend** where a customization hook is needed. For example, `EntityConditionModel.Builder.condition(attribute, consumer)` hands out a builder initialized with the defaults.
- **Packages are strictly acyclic.** A method belongs with the type at the level of what it returns: a factory returning a higher-level type is a static method there, not an instance method on the lower type.
- **Nullability:** every module is `@NullMarked` (JSpecify), with `@Nullable` exactly where null is allowed.
- **Configuration properties** are `PropertyValue` constants on the owning type, listed in the package javadoc.
  - Framework-wide keys start with `codion.`; component-specific keys use the class name, for example `is.codion.swing.common.ui.component.text.NumberField.convertGroupingToDecimalSeparator`.
  - A dot ends a namespace, camelCase composes within one: `codion.db.pool.maximumSize`, `codion.server.connection.idleTimeout`.
- **Internationalization:** resource bundles are named after the class using them, `EntityTablePanel_is_IS.properties`, loaded via `MessageBundle` so they can be overridden. Shared messages live in the i18n modules.
- **Serializable classes** declare `@Serial private static final long serialVersionUID = 1;`. Versioning starts after 1.0.
- **No `@SuppressWarnings`.** Unchecked compiler notes are tolerated.
- **Changelog:** every API or behaviour change gets one terse line in `changelog.md`, under its package heading in the topmost, unreleased version. Documentation-only changes get none.
- **Javadoc** states behaviour precisely, using `{@snippet}` for code. Keep the rationale short.
- **Check existing patterns in similar modules** before implementing something new.

## Testing

- Tests run against in-memory H2 databases. Each module with database tests has its schema and data in `src/test/sql/create_h2_db.sql`, the test user `scott:tiger` (`codion.test.user`), with `codion.db.url` set by the root build.
- Database or driver specific behaviour is verified against the real databases, not reasoned from documentation. When modifying database related code, test with more than H2.
- A condition model means the same whether translated into a query condition or used as a table filter, and tests compare the two, row for row.

## Documentation

- The manual is in `documentation/src/docs/asciidoc/manual/`, starting from `manual.adoc`, with technical docs in `technical/` and tutorials in `tutorials/`.
- **Never embed Java code in the asciidoc.** Code examples are tagged regions of compiled sources, included with `include::...[tags=name]`, so they are refactored and compiled along with the framework:
  - most live in `demos/manual` (`is.codion.manual.*`, following the manual's structure) and `demos/chinook` (`is.codion.demos.chinook.manual`);
  - examples take their dependencies as parameters, an `EntityConnection connection` for example, keeping setup out of the example;
  - they use the real demo entities, never artificial ones.
- Javadoc links in the asciidoc use module placeholders, `{url-javadoc}{framework-model}/is/codion/framework/model/EntityModel.html`, defined in `documentation/build.gradle.kts`.

## Where to Find Working Examples

| Topic | Where |
|---|---|
| Domain modeling: entities, foreign keys, converters, derived and denormalized attributes, custom conditions, functions and procedures, reports | `demos/chinook/.../chinook/domain` (API in `api/Chinook.java`), `demos/world`, manual `framework-domain-model.adoc` |
| Queries and conditions | manual `framework-conditions.adoc`, `framework-entity-connection.adoc`; `ConditionDemo`, `EntityConnectionDemo` in `.../chinook/manual` |
| Entity models and panels, master-detail | `FrameworkModelDemo`, `FrameworkUIDemo` in `.../chinook/manual`; `demos/chinook/.../chinook/ui` |
| The Swing toolkit without entities | manual `swing-common-ui*.adoc`, `demos/manual/.../manual/swing/common`; SDKBOY (github.com/codion-is/sdkboy) |
| Minimal entity usage with a custom UI | Llemmy (github.com/codion-is/llemmy) |
| Server, authentication, remote connections | `demos/server`, `ChinookAuthenticator`, technical `server.adoc` |
| Load testing | `ChinookLoadTest`, `EmployeesLoadTest` in the demos' `testing` packages |

## API Refinement Window

**IMPORTANT**: Codion is in its final API refinement phase before promotion, and version 1.0 freezes the public API, [Semantic Versioning](https://semver.org/) following. Until then, breaking changes are welcome: every suboptimal name or behaviour fixed now is one we won't be stuck with.

If you spot any of these while working with Codion, **speak up immediately**:

1. **Redundant context** - `modifiesEntity()` in an attribute class → `modifies()`
2. **Unnecessary prefixes/suffixes** - `Notify.WHEN_SET` → `SET`
3. **Verbose names** where shorter would be clearer
4. **Inconsistencies** with the conventions above
5. **Confusing method or parameter names**
6. **Behaviour that would be awkward to keep forever** - operator semantics, null handling, defaults and customization hooks are API too

Recent examples: `valueClass()`/`columnClass()` → `type()`, `codion.configurationFile` → `codion.config.file`, `EntityConditions` subclassing → `EntityConditionModel.Builder.condition()`, and `NOT_BETWEEN` made the exact complement of `BETWEEN`, null values included.

Remember: Once the API freezes, these names are forever. Help make Codion something we'll all be happy using and maintaining for the next 20 years!