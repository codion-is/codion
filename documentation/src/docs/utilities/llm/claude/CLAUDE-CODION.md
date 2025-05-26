# CLAUDE-CODION.md

This file provides guidance to Claude Code (claude.ai/code) when working with Codion-based applications. It's designed to be self-contained for use in monorepos that use the Codion framework.

## Codion Framework Overview

Codion is a full-stack Java rich client desktop CRUD application framework based solely on Java Standard Edition components. It follows Domain-Driven Design principles with Entity-Relationship concepts rather than ORM. The framework has been continuously refined for over 20 years and is designed for internal business/scientific applications, though it can handle thousands of concurrent users.

**Key Technologies:**
- Java 17+ (compiled with JDK 21)
- Gradle build system
- Swing UI framework  
- JUnit Jupiter for testing
- Multiple database support (PostgreSQL, Oracle, H2, MySQL, DB2, Derby, HSQLDB, MariaDB, SQLite, SQL Server)

## Design Philosophy

- **Observable/Reactive Patterns Throughout** - Everything is observable (`Value<T>`, `State`, `Event<T>`)
- **Builders All the Way Down** - Fluent API configuration for consistency and discoverability
- **Continuous Refinement** - ~20 years of development and polishing
- **Pragmatic Over Dogmatic** - Built for real internal business applications
- **Descending from Peak Complexity** - Continuous simplification over time

## Core Architectural Patterns

### 1. Observable/Reactive Pattern

The framework extensively uses `Observable<T>`, `Value<T>`, and `State` for reactive programming:

```java
// Mutable boolean state
State downloading = State.state();
State confirmExit = State.state(true); // with initial value
State readOnly = State.and(installing.not(), refreshing.not()); // composite states

// Mutable value holder
Value<String> filter = Value.builder()
    .<String>nullable()
    .listener(this::onFilterChanged)
    .build();

// Observer pattern - UI updates automatically
editModel.editor().value(Track.RATING).addConsumer(rating -> 
    System.out.println("Rating changed to: " + rating));
```

### 2. Entity System

Type-safe entity modeling with:
- `Entity`: Represents a database row
- `EntityDefinition`: Defines entity structure and metadata
- `Attribute<T>`: Type-safe attribute references
- `Column<T>`: Database column definitions
- `ForeignKey`: Foreign key definitions
- `EntityType`: The entity type identifier

### 3. Domain Modeling

Entities are defined using a fluent builder pattern:

```java
interface RecordLabel { 	
    EntityType TYPE = DOMAIN.entityType("chinook.record_label");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
}

interface Artist { 	
    EntityType TYPE = DOMAIN.entityType("chinook.artist");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<Integer> RECORD_LABEL_ID = TYPE.integerColumn("record_label_id");
    ForeignKey RECORD_LABEL_FK = TYPE.foreignKey("record_label_fk", RECORD_LABEL_ID, RecordLabel.ID);
}

EntityDefinition artist() {
    return Artist.TYPE.define(
        Artist.ID.define()
            .primaryKey(),
        Artist.NAME.define()
            .column()
            .caption("Name")
            .nullable(false)
            .maximumLength(120),
        Artist.RECORD_LABEL_FK.define()
            .foreignKey()
            .caption("Record label"))
    .keyGenerator(KeyGenerator.identity())
    .caption("Artist")
    .build();
}
```

### 4. Connection Abstraction

`EntityConnection` provides a unified interface for database operations:

```java
// Basic CRUD operations
Entity artist = connection.insertSelect(newArtist);
artist = connection.updateSelect(artist);
connection.delete(artist.primaryKey());

// Querying
List<Entity> artists = connection.select(
    Artist.NAME.like("Pink%"),
    orderBy(Artist.NAME)
);

// Transactions
EntityConnection connection = connectionProvider.connection();

// Without result
EntityConnection.transaction(connection, () -> {
    Entities entities = connection.entities();

    Entity artist = entities.builder(Artist.TYPE)
                    .with(Artist.NAME, "The Band")
                    .build();
    artist = connection.insertSelect(artist);

    Entity album = entities.builder(Album.TYPE)
                    .with(Album.ARTIST_FK, artist)
                    .with(Album.TITLE, "The Album")
                    .build();

    connection.insert(album);
});

// With result
EntityConnection connection = connectionProvider.connection();

Entity.Key albumKey = EntityConnection.transaction(connection, () -> {
    Entities entities = connection.entities();

    Entity artist = entities.builder(Artist.TYPE)
                    .with(Artist.NAME, "The Band")
                    .build();
    artist = connection.insertSelect(artist);

    Entity album = entities.builder(Album.TYPE)
                    .with(Album.ARTIST_FK, artist)
                    .with(Album.TITLE, "The Album")
                    .build();

    return connection.insert(album);
});
```

### 5. MVC in Swing Layer

- `SwingEntityModel`: Coordinates edit and table models
- `SwingEntityEditModel`: Single entity editing
- `SwingEntityTableModel`: Entity collection management
- `EntityPanel`: Master UI panel
- `EntityEditPanel`: Form-based editing
- `EntityTablePanel`: Tabular display

## Essential Patterns

### Foreign Key Reference Patterns

Foreign keys are first-class citizens with automatic entity loading:

```java
// Define foreign key with attributes to fetch from referenced entity
Artist.RECORD_LABEL_FK.define()
    .foreignKey()
    .attributes(RecordLabel.NAME, RecordLabel.FOUNDED)  // Fetched with the Artist
    .referenceDepth(2)  // How deep to follow foreign key chains

// Usage - referenced entity is automatically loaded
Entity artist = connection.selectSingle(Artist.ID.equalTo(42));
Entity label = artist.get(Artist.RECORD_LABEL_FK);  // Already loaded!
String labelName = artist.get(Artist.RECORD_LABEL_FK).get(RecordLabel.NAME);
```

### Master-Detail Pattern

The framework uses a fractal master-detail pattern:

```java
// Models contain detail models
SwingEntityModel invoiceModel = new SwingEntityModel(invoiceEditModel);
SwingEntityModel invoiceLineModel = new SwingEntityModel(invoiceLineEditModel);
invoiceModel.detailModels().add(invoiceLineModel);

// Panels contain detail panels  
EntityPanel invoicePanel = new EntityPanel(invoiceModel);
EntityPanel invoiceLinePanel = new EntityPanel(invoiceLineModel);
invoicePanel.detailPanels().add(invoiceLinePanel);
```

### UI Component Builders

All components use fluent builders:

```java
// Text field with reactive binding
JTextField filter = stringField(model.filterValue())
    .hint("Filter...")
    .lowerCase(true)
    .selectAllOnFocusGained(true)
    .transferFocusOnEnter(true)
    .enabled(model.editEnabled())
    .build();

// Entity panel configuration
EntityPanel customerPanel = new EntityPanel(customerModel,
    new CustomerEditPanel(customerModel.editModel()),
    config -> config.detailLayout(TabbedDetailLayout.builder()
        .splitPaneResizeWeight(0.4)
        .build()));
```

### Async Task Handling

ProgressWorker provides elegant async execution:

```java
ProgressWorker.builder(task)
    .onStarted(this::taskStarted)
    .onProgress(this::updateProgress)
    .onResult(this::taskCompleted)
    .onException(this::handleError)
    .execute();
```

### Control Abstraction

Controls separate action logic from UI components:

```java
Control saveControl = Control.builder()
    .command(this::save)
    .enabled(State.and(
        modified,
        valid,
        notBusy))
    .build();

// Use with multiple UI components
button(saveControl).build();
menuItem(saveControl).build();
```

## Advanced Domain Patterns

### Custom Types and Converters

```java
// Custom value type
record Location(double latitude, double longitude) implements Serializable {}

// Define column with custom type
Column<Location> LOCATION = TYPE.column("location", Location.class);

// Converter handles database ↔ Java conversion
LOCATION.define()
    .column()
    .columnClass(String.class, new LocationConverter());
```

### Derived Attributes

Calculate values dynamically:

```java
Attribute<Integer> NO_OF_SPEAKERS = TYPE.integerAttribute("noOfSpeakers");

NO_OF_SPEAKERS.define()
    .derived(CountryLanguage.COUNTRY_FK, CountryLanguage.PERCENTAGE)
    .provider(sourceValues -> {
        Double percentage = sourceValues.get(CountryLanguage.PERCENTAGE);
        Entity country = sourceValues.get(CountryLanguage.COUNTRY_FK);
        if (percentage != null && country != null) {
            Integer population = country.get(Country.POPULATION);
            return (int)(population * (percentage / 100));
        }
        return null;
    });
```

### Database Functions

Expose database logic through the domain:

```java
// Simple function
FunctionType<EntityConnection, BigDecimal, Integer> RAISE_PRICE = 
    functionType("schema.raise_price");

// Complex function with custom parameters
FunctionType<EntityConnection, CustomParams, Entity> COMPLEX_FUNCTION = 
    functionType("schema.complex_function");

// Usage
Integer result = connection.function(RAISE_PRICE, new BigDecimal("0.1"));
```

### Performance Optimizations

```java
// Control reference depth
Track.ALBUM_FK.define()
    .foreignKey()
    .referenceDepth(2);  // Fetch Track → Album → Artist

// Lazy loading
LargeEntity.BLOB_DATA.define()
    .column()
    .selected(false);  // Only loaded when explicitly requested

// Denormalized attributes
Country.CAPITAL_POPULATION.define()
    .denormalized(Country.CAPITAL_FK, City.POPULATION);
```

## Common Code Patterns

### Builder Pattern Usage
```java
// Nearly every component uses builders
EntityPanel.builder(Artist.TYPE, MyApp::createArtistPanel)
    .caption("Artists")
    .description("Manage artists")
    .preferredSize(new Dimension(800, 600))
    .build();
```

### Observable Values
```java
// Instead of: private String name;
private final Value<String> name = Value.value();

// Usage
name.set("John");
name.addListener(this::onNameChanged);
name.link(anotherValue); // Bidirectional binding
```

### Condition Building
```java
// Type-safe query conditions
Condition condition = and(
    Track.ARTIST_FK.equalTo(artist),
    Track.GENRE_FK.in(genres),
    Track.RATING.greaterThanOrEqualTo(4),
    or(
        Track.YEAR.isNull(),
        Track.YEAR.greaterThan(2000)
    )
);

List<Entity> tracks = connection.select(condition);
```

### Entity Validation
```java
class MyEntityValidator extends DefaultEntityValidator {
    @Override
    public <T> void validate(Entity entity, Attribute<T> attribute) {
        super.validate(entity, attribute);
        // Custom validation logic
        if (attribute.equals(Product.PRICE)) {
            BigDecimal price = entity.get(Product.PRICE);
            if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException(Product.PRICE, price, 
                    "Price cannot be negative");
            }
        }
    }
}
```

## Testing Patterns

### Domain Testing
```java
@Test
void entityDefinition() {
    Entity artist = entities.entity(Artist.TYPE);
    artist.put(Artist.NAME, "Pink Floyd");
    
    artist = connection.insertSelect(artist);
    assertNotNull(artist.get(Artist.ID));
    assertEquals("Pink Floyd", artist.get(Artist.NAME));
}
```

### Model Testing
```java
@Test
void editModel() {
    SwingEntityEditModel editModel = new SwingEntityEditModel(Artist.TYPE, connection);
    
    editModel.editor().value(Artist.NAME).set("Led Zeppelin");
    assertTrue(editModel.editor().modified().get());
    
    editModel.insert();
    assertFalse(editModel.editor().modified().get());
}
```

## Important Conventions

1. **Module System**: Uses Java Platform Module System (JPMS). Each module has a `module-info.java`.

2. **Service Discovery**: Domains are discovered via `ServiceLoader`. Domain implementations must be registered in `META-INF/services/is.codion.framework.domain.Domain`.

3. **Testing**: Tests typically use H2 in-memory database with initialization scripts in `src/test/sql/create_h2_db.sql`.

4. **Configuration**: System properties prefixed with `codion.` control framework behavior. Component-specific properties use the full class name as prefix.

5. **Internationalization**: Resource bundles follow the pattern `Messages_<locale>.properties`.

6. **Keyboard Navigation**: The framework emphasizes keyboard over mouse usage - all UI components support comprehensive keyboard shortcuts.

## Security Model

Codion delegates authentication and authorization to the underlying database by default:
- Role-based access control via database roles
- `schema_read` role for read-only access
- `schema_write` role for full access
- Custom authentication can be implemented via `Authenticator` interface
- RMI connections support SSL and deserialization filtering

## Best Practices

1. **Use the Observable Pattern** - Don't fight it, embrace it. State changes should propagate automatically.
2. **Let the Database Work** - Use views, functions, procedures. Don't replicate database logic in Java.
3. **Builder Pattern** - Use it consistently for all component configuration.
4. **Type Safety** - Leverage the type system, avoid string keys and magic constants.
5. **Test at the Right Level** - Unit test domains, integration test with H2, use load testing for performance.
6. **Foreign Key Design** - Define relationships properly with appropriate reference depth.
7. **Validate Early** - Use domain validators to catch errors before they hit the database.
8. **Async Operations** - Use ProgressWorker for long-running tasks to keep UI responsive.

## Framework Capabilities Summary

- **Scale**: Handles 7000+ concurrent users in production environments
- **Complexity**: Supports domains with 50+ entities and complex relationships
- **Performance**: Built-in connection pooling, smart fetching, lazy loading
- **Monitoring**: Comprehensive server monitoring and load testing capabilities
- **Reporting**: Integrated support for JasperReports and custom reporting
- **Database Support**: Works with all major databases via JDBC
- **UI Flexibility**: From simple CRUD to complex custom UIs
- **Keyboard-First**: Comprehensive keyboard navigation throughout