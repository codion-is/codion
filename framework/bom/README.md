# Codion BOM (Bill of Materials)

The Codion BOM manages compatible versions of all Codion modules, simplifying dependency management for users.

## Usage

### For Gradle Users

Add the BOM to your `build.gradle.kts`:

```kotlin
dependencies {
    // Import the BOM - manages all Codion module versions
    implementation(platform("is.codion:codion-framework-bom:0.18.39"))
    
    // Now add Codion modules WITHOUT version numbers
    implementation("is.codion:codion-framework-domain")
    implementation("is.codion:codion-framework-db-local")
    implementation("is.codion.swing:codion-swing-framework-ui")
    implementation("is.codion.dbms:codion-dbms-h2") // or your database
    
    // Optional plugins (no versions needed)
    implementation("is.codion.plugin:codion-plugin-flatlaf")
    implementation("is.codion.plugin:codion-plugin-jasperreports")
}
```

### For Maven Users

Add the BOM to your `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>is.codion</groupId>
            <artifactId>codion-framework-bom</artifactId>
            <version>0.18.39</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Now add Codion modules WITHOUT version numbers -->
    <dependency>
        <groupId>is.codion</groupId>
        <artifactId>codion-framework-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>is.codion</groupId>
        <artifactId>codion-framework-db-local</artifactId>
    </dependency>
    <dependency>
        <groupId>is.codion.swing</groupId>
        <artifactId>codion-swing-framework-ui</artifactId>
    </dependency>
    <dependency>
        <groupId>is.codion.dbms</groupId>
        <artifactId>codion-dbms-h2</artifactId>
    </dependency>
</dependencies>
```

## Benefits

1. **Version Consistency**: All Codion modules use compatible versions
2. **Simplified Upgrades**: Change one version number to upgrade everything
3. **Reduced Errors**: No version mismatches between Codion modules
4. **Cleaner Build Scripts**: No need to specify versions for each module
5. **Framework Completeness**: Clear view of all available modules

## Module Categories

### Common Foundation
Core classes and utilities used throughout the framework:
- `codion-common-core` - Observable pattern, State, Value, Events, Configuration
- `codion-common-db` - Database abstractions and utilities
- `codion-common-model` - Base model interfaces and table models
- `codion-common-rmi` - RMI communication utilities
- `codion-common-i18n` - Internationalization support

### Framework Core
Domain modeling and data access layer:
- `codion-framework-domain` - Entity definitions and domain modeling
- `codion-framework-domain-db` - Database-specific domain implementations
- `codion-framework-domain-test` - Testing utilities for domains
- `codion-framework-db-core` - Core database connection interfaces
- `codion-framework-db-local` - Local JDBC connections
- `codion-framework-db-rmi` - Remote RMI connections  
- `codion-framework-db-http` - HTTP-based connections
- `codion-framework-json-domain` - JSON serialization for domains
- `codion-framework-json-db` - JSON serialization for database operations
- `codion-framework-model` - UI-agnostic business logic models
- `codion-framework-model-test` - Testing utilities for models
- `codion-framework-server` - Server-side components and RMI server
- `codion-framework-servlet` - Servlet-based server components
- `codion-framework-i18n` - Framework internationalization

### Swing UI Layer
Desktop UI components and framework integration:
- `codion-swing-common-model` - Swing model classes (table, combo, etc.)
- `codion-swing-common-ui` - Basic UI components and utilities
- `codion-swing-framework-model` - Entity-aware Swing models
- `codion-swing-framework-ui` - Entity-based Swing UI components

### Database Support
Database-specific implementations and drivers:
- `codion-dbms-db2` - IBM DB2 database support
- `codion-dbms-derby` - Apache Derby database support  
- `codion-dbms-h2` - H2 database support
- `codion-dbms-hsqldb` - HSQLDB database support
- `codion-dbms-mariadb` - MariaDB database support
- `codion-dbms-mysql` - MySQL database support
- `codion-dbms-oracle` - Oracle database support
- `codion-dbms-postgresql` - PostgreSQL database support
- `codion-dbms-sqlite` - SQLite database support
- `codion-dbms-sqlserver` - Microsoft SQL Server support

### Plugins
Optional functionality and third-party integrations:
- `codion-plugin-flatlaf` - Modern FlatLaf Look & Feel
- `codion-plugin-flatlaf-intellij-themes` - IntelliJ-based FlatLaf themes
- `codion-plugin-hikari-pool` - HikariCP connection pooling
- `codion-plugin-tomcat-pool` - Tomcat JDBC connection pooling
- `codion-plugin-jasperreports` - JasperReports integration
- `codion-plugin-imagepanel` - Enhanced image display component
- `codion-plugin-jul-proxy` - Java Util Logging integration
- `codion-plugin-log4j-proxy` - Log4j logging integration
- `codion-plugin-logback-proxy` - Logback logging integration
- `codion-plugin-swing-mcp` - AI/LLM integration via Model Context Protocol

### Development Tools
Code generation, testing, and monitoring utilities:
- `codion-tools-loadtest-core` - Core load testing framework
- `codion-tools-loadtest-model` - Load testing model components
- `codion-tools-loadtest-ui` - Load testing UI components
- `codion-tools-generator-domain` - Domain code generation
- `codion-tools-generator-model` - Model code generation  
- `codion-tools-generator-ui` - UI code generation
- `codion-tools-monitor-model` - Application monitoring models
- `codion-tools-monitor-ui` - Application monitoring UI

## Common Combinations

### Basic Desktop Application
```kotlin
implementation(platform("is.codion:codion-framework-bom:0.18.39"))
implementation("is.codion:codion-framework-domain")
implementation("is.codion:codion-framework-db-local")
implementation("is.codion.swing:codion-swing-framework-ui")
implementation("is.codion.dbms:codion-dbms-h2")
implementation("is.codion.plugin:codion-plugin-flatlaf")
```

### Client-Server Application
```kotlin
implementation(platform("is.codion:codion-framework-bom:0.18.39"))
implementation("is.codion:codion-framework-domain")
implementation("is.codion:codion-framework-db-rmi")
implementation("is.codion.swing:codion-swing-framework-ui")
implementation("is.codion.plugin:codion-plugin-flatlaf")
```

### Server Application
```kotlin
implementation(platform("is.codion:codion-framework-bom:0.18.39"))
implementation("is.codion:codion-framework-server")
implementation("is.codion.dbms:codion-dbms-postgresql")
implementation("is.codion.plugin:codion-plugin-hikari-pool")
```

## Version Management

The BOM ensures that when you use Codion version `0.18.39`, all modules are at exactly that version. This eliminates the possibility of accidentally mixing incompatible versions.

To upgrade Codion, simply change the BOM version:
```kotlin
// Before
implementation(platform("is.codion:codion-framework-bom:0.18.39"))

// After
implementation(platform("is.codion:codion-framework-bom:0.18.40"))
```

All Codion modules will automatically use the new version.