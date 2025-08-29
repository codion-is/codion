# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Introductory package

If you find a claude directory in the project root dir, then that is meant for you. It contains comprehensive information about Codion, what it is, where it's from and where it's going. Nothing in the claude directory is superfluous. claude/CLAUDE_INGESTION_GUIDE.md contains a guide for the directory contents.

## Project Overview

Codion is a full-stack Java rich client desktop CRUD application framework based solely on Java Standard Edition components. It follows Domain-Driven Design principles with Entity-Relationship concepts rather than ORM. The framework has been continuously refined for over 20 years and is designed for internal business/scientific applications with 1-10 users, though it can handle thousands of concurrent users (see `documentation/src/docs/asciidoc/images/monitoring`).

**Key Technologies:**
- Java 17+ (compiled with JDK 21)
- Gradle 8.14 build system
- Swing UI framework
- JUnit Jupiter 5.12.2 for testing
- Multiple database support (PostgreSQL, Oracle, H2, MySQL, etc.)

## Design Philosophy

- **Observable/Reactive Patterns Throughout** - Everything is observable (`Value<T>`, `State`, `Event<T>`)
- **Builders All the Way Down** - Fluent API configuration for consistency and discoverability
- **Continuous Refinement** - ~20 years of development, 2+ years of polishing (see `changelog.md` for a couple of years worth of mostly polishing, 360+ renames, 237+ removals)
- **Pragmatic Over Dogmatic** - Built for real internal business applications
- **Descending from Peak Complexity** - See `documentation/src/docs/asciidoc/images/complexity.png` for metrics showing decreasing complexity over time

## Essential Commands

### Building
```bash
./gradlew build                    # Full build
./gradlew clean build             # Clean build
./gradlew build -x test           # Build without tests
```

### Testing
```bash
./gradlew test                    # Run all tests
./gradlew :module-name:test       # Test specific module (e.g., :codion-common-core:test)
./gradlew test --tests "ClassName"              # Run specific test class
./gradlew test --tests "ClassName.methodName"   # Run specific test method
./gradlew test --debug-jvm                      # Debug tests on port 5005
```

### Code Quality
```bash
./gradlew spotlessCheck          # Check code formatting
./gradlew spotlessApply          # Auto-fix formatting issues
./gradlew sonar                  # Run SonarQube analysis
./gradlew jacocoTestReport       # Generate coverage report
./gradlew check                  # Run all verification tasks
```

### Running Demos
```bash
./gradlew demo-chinook:runClientLocal    # Run the Chinook demo with a local connection
# Other demos available in demos/ directory
```

## High-Level Architecture

### Module Structure
```
codion/
├── common/          # Foundation modules
│   ├── core/        # Observable pattern, Configuration, Events
│   ├── db/          # Database abstractions
│   ├── model/       # Base model interfaces
│   └── rmi/         # Remote communication
├── framework/       # Core framework
│   ├── domain/      # Entity definitions and domain modeling
│   ├── db-core/     # EntityConnection interface
│   ├── db-local/    # Local JDBC connections
│   ├── db-rmi/      # Remote connections
│   └── model/       # UI framework agnostic business logic models
├── swing/           # UI layer
│   ├── common-ui/   # UI utilities and components
│   ├── framework-model/  # Swing entity models
│   └── framework-ui/     # Entity-based UI components
└── dbms/           # Database-specific implementations
```

### Core Architectural Patterns

1. **Observable/Reactive Pattern**: The framework extensively uses `Observable<T>`, `Value<T>`, and `State` for reactive programming. UI components automatically update when underlying data changes.

2. **Entity System**: Type-safe entity modeling with:
   - `Entity`: Represents a database row
   - `EntityDefinition`: Defines entity structure and metadata
   - `Attribute<T>`: Type-safe attribute references
   - `AttributeDefinition<T>`: Manages attribute metadata
   - `Column<T>`: Database column definitions
   - `ColumnDefinition<T>`: Manages column metadata
   - `ForeignKey`: Foreign key definitions
   - `ForeignKeyDefinition`: Manages foreign key metadata

3. **Domain Modeling**: Entities are defined using a fluent builder pattern:
   ```java
   interface RecordLabel { 	
       EntityType TYPE = DOMAIN.entityType("chinook.record_label");
   
       Column<Integer> ID = TYPE.integerColumn("id");
       Column<Integer> NAME = TYPE.integerColumn("name");
   }

   interface Artist { 	
       EntityType TYPE = DOMAIN.entityType("chinook.artist");
   
       Column<Integer> ID = TYPE.integerColumn("id");
       Column<Integer> NAME = TYPE.integerColumn("name");
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

4. **Connection Abstraction**: `EntityConnection` provides a unified interface for database operations, supporting local JDBC, RMI, and HTTP connections.

5. **MVC in Swing Layer**:
   - `SwingEntityModel`: Coordinates edit and table models
   - `SwingEntityEditModel`: Single entity editing
   - `SwingEntityTableModel`: Entity collection management
   - `EntityPanel`: Master UI panel
   - `EntityEditPanel`: Form-based editing
   - `EntityTablePanel`: Tabular display

### Important Conventions

1. **Module System**: Uses Java Platform Module System (JPMS). Each module has a `module-info.java`.

2. **Service Discovery**: Domains are discovered via `ServiceLoader`. Domain implementations must be registered in `META-INF/services/is.codion.framework.domain.Domain`. Domains can also be kept in a local scope and instantiated directly, see ChinookAuthenticator.

3. **Testing**: Tests use H2 in-memory database with initialization scripts in `src/test/sql/create_h2_db.sql`.

4. **Configuration**: System properties prefixed with `codion.` control core framework behavior. Component specific properties use the component class name as prefix: `is.codion.swing.common.ui.component.text.NumberField.convertGroupingToDecimalSeparator`

5. **Internationalization**: Resource bundles follow the pattern `Messages_<locale>.properties`.

6. **License**: GPL-3.0 license. Note: This is "open-source, not open-contribution" - code contributions are not accepted.

7. **Static Factory Methods**: Factory methods are named after the type they return (e.g., `ReportType.reportType()`, `State.state()`) to enable clean static imports: `import static is.codion.common.db.report.ReportType.reportType;` then `ReportType report = reportType("name");`. This pattern is used consistently throughout the framework.

### Development Notes

- Always check existing patterns in similar modules before implementing new features
- The framework emphasizes keyboard navigation over mouse usage
- Entity definitions should include proper validations and constraints
- Use the Observable pattern for any state that needs to trigger UI updates
- Foreign key relationships should be properly defined in entity definitions
- Test with multiple database types when modifying database-related code

## Key Architectural Insights

### Foreign Key Reference Patterns

Foreign keys in Codion are first-class citizens with automatic entity loading:

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

The framework uses a fractal master-detail pattern throughout:

```java
// Models contain detail models
SwingEntityModel invoiceModel = new SwingEntityModel(invoiceEditModel);
SwingEntityModel invoiceLineModel = new SwingEntityModel(invoiceLineEditModel);
invoiceModel.detailModels().add(invoiceModel);

// Panels contain detail panels  
EntityPanel invoicePanel = new EntityPanel(invoiceModel);
EntityPanel invoiceLinePanel = new EntityPanel(invoiceLineModel);
invoicePanel.detailPanels().add(invoiceLinePanel);
```

### Observable State Management

Everything is observable, enabling reactive UI updates:

```java
// Entity values are observable
editModel.editor().value(Track.RATING).addConsumer(rating -> 
    System.out.println("Rating changed to: " + rating));

// Model state is observable
editModel.editor().modified().addConsumer(modified -> 
    saveButton.setEnabled(modified));

// Combine states
State canSave = State.and(
    hasChanges,
    isValid,
    notUpdating
);
```

### Testing Capabilities

The framework includes a comprehensive load testing framework:

```java
LoadTest<EmployeesAppModel> loadTest = LoadTest.builder(applicationFactory, application -> application.close())
    .user(UNIT_TEST_USER)
    .scenarios(List.of(
        scenario(new InsertDepartment(), 1),    // Weight: 1
        scenario(new SelectDepartment(), 10)))  // Weight: 10  
    .build();
```

See `documentation/src/docs/asciidoc/images/monitoring` directory for screenshots showing the framework handling 7000+ concurrent users.

### Security Model

Codion delegates authentication and authorization to the underlying database by default, typically with simple role-based access:
- `schema_read` role for read-only access
- `schema_write` role (includes read) for full access
- RMI server should run behind VPN for internal applications
- SSL and deserialization filtering for additional security

## Non-Entity UI Patterns

The SDKBOY demo showcases Codion's UI capabilities without the Entity framework. These patterns form the foundation of all Codion UI components:

### Observable State Management

1. **State** - Mutable boolean state with change notifications:
   ```java
   State downloading = State.state();
   State confirmExit = State.state(true); // with initial value
   State readOnly = State.and(installing.not(), refreshing.not()); // composite states
   ```

2. **Value<T>** - Mutable value holder for nullable or non-nullable types:
   ```java
   Value<String> filter = Value.builder()
       .<String>nullable()
       .listener(this::onFilterChanged)
       .build();
   
   Value<String> value = Value.builder()
          .nonNull("none")                 // null replacement, used when set() receives null
          .value("hello")                  // the initial value
          .notify(Notify.WHEN_SET)         // notifies listeners when set
          .validator(this::validateString) // using a validator
          .listener(this::onStringSet)     // and a listener
          .build();
   ```

3. **Observer Pattern** - Components automatically update when state changes:
   ```java
   installTask.active.addConsumer(this::onInstallActiveChanged);
   model.selection().item().addListener(this::onSelectionChanged);
   ```

### FilterTableModel Pattern

The table model provides sophisticated data management without entities:

```java
FilterTableModel<RowType, ColumnEnum> tableModel = 
    FilterTableModel.builder(new TableColumns())
        .items(new ItemSupplier())       // Provides that data when refreshed
        .visible(new VisibilityPredicate()) // Row filtering
        .build();
```

Key features:
- Type-safe column definitions via enums
- Built-in sorting and filtering
- Selection management with single/multi selection modes
- Refresh capability with progress indication
- Observable selection state

### UI Component Builders

All components use fluent builders with consistent patterns:

```java
// Text field with reactive binding
JTextField filter = stringField(model.filterValue())
    .hint("Filter...")
    .lowerCase(true)
    .selectAllOnFocusGained(true)
    .transferFocusOnEnter(true)
    .keyEvent(KeyEvents.builder(VK_UP)
        .action(command(selection::decrement)))
    .enabled(model.editEnabled())
    .build();

// Table with full configuration
FilterTable<Row, Column> table = FilterTable.builder(model)
    .columns(columns)
    .sortable(false)
    .selectionMode(SINGLE_SELECTION)
    .doubleClick(command(this::onDoubleClick))
    .enabled(taskActive.not())
    .cellRenderer(Column.STATUS, customRenderer)
    .build();
```

### Async Task Handling

ProgressWorker provides elegant async execution with progress:

```java
ProgressWorker.builder()
    .task(task)
    .onStarted(this::taskStarted)
    .onProgress(this::updateProgress)
    .onPublish(this::showStatus)
    .onDone(this::taskCompleted)
    .onResult(() -> model.refresh())
    .execute();
```

### Control Abstraction

Controls separate action logic from UI components:

```java
Control installControl = Control.builder()
    .command(this::install)
    .enabled(and(
        versionSelected,
        versionInstalled.not(),
        taskActive.not()))
    .build();

// Can be used with buttons, menu items, key bindings
button(installControl).build();
keyEvent.action(installControl).enable(panel);
```

### Keyboard-First Design

Systematic keyboard support throughout:

```java
KeyEvents.Builder keyEvent = KeyEvents.builder()
    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    .modifiers(ALT_DOWN_MASK);

keyEvent.keyCode(VK_I).action(installControl).enable(this);
keyEvent.keyCode(VK_D).action(uninstallControl).enable(this);
```

### Key Design Principles

1. **Everything is Observable** - State changes automatically propagate to UI
2. **Builders with Parameters** - All configuration methods take parameters (enabling mechanical/LLM generation)
3. **Type Safety** - Column enums, generic row types, no string keys
4. **Composition over Inheritance** - Combine states, nest panels, reuse controls
5. **Separation of Concerns** - Models handle data, panels handle layout, controls handle actions

## Hybrid Entity-UI Patterns

The Llemmy demo demonstrates a pragmatic middle ground - using Entity framework minimally for database operations while building the UI primarily with common-ui components.

### When to Use This Approach

Consider the hybrid pattern when:
- You need database persistence but not complex domain logic
- Your UI requirements don't fit standard CRUD patterns
- You want Entity framework benefits (connection management, transactions) without the constraints
- You're building specialized applications (chat interfaces, dashboards, tools)

### Minimal Entity Usage

Define entities purely for data storage:

```java
interface Chat {
    EntityType TYPE = DOMAIN.entityType("llemmy.chat");
    
    Column<Integer> ID = TYPE.integerColumn("id");
    Column<LocalDateTime> TIMESTAMP = TYPE.localDateTimeColumn("timestamp");
    Column<String> MESSAGE = TYPE.stringColumn("message");
    Column<String> JSON = TYPE.stringColumn("json");
    // ... other columns
}
```

Use SwingEntityModel as a thin wrapper:
```java
public final class ChatModel extends SwingEntityModel {
    ChatModel(EntityConnectionProvider connectionProvider) {
        super(new ChatTableModel(connectionProvider));
    }
}
```

### Custom Model Logic

Implement business logic in edit models, not domain definitions:

```java
public final class ChatEditModel extends AbstractEntityEditModel {
    private final Event<List<Document>> documentsSelected = Event.event();
    private final State processing = State.state();
    private final Value<ChatLanguageModel> model = Value.nullable();
    
    void submitPrompt() {
        ProgressWorker.builder(this::callLLM)
            .onStarted(this::onStarted)
            .onResult(this::onResult)
            .onException(this::onException)
            .execute();
    }
    
    private ChatResponse callLLM(ProgressReporter<String> progressReporter) {
        // Prepare attachments, call LLM, track tokens
        return model.get().call(messages);
    }
}
```

### Async Operations with Progress

Handle long-running operations elegantly:

```java
ProgressWorker.builder(longRunningTask)
    .onStarted(() -> {
        processing.set(true);
        startTime = currentTimeMillis();
    })
    .onProgress(progress -> elapsed.set(formatElapsed(startTime)))
    .onResult(result -> {
        // Update UI with result
        processing.set(false);
    })
    .onException(exception -> {
        // Store error in database
        insertError(exception);
        processing.set(false);
    })
    .execute();
```

### Override Default Behaviors

Customize Entity framework behaviors:

```java
// Soft delete instead of hard delete, AbstractEntityEditModel
@Override
protected void delete(Collection<Entity> entities, EntityConnection connection) {
  connection.update(entities.stream()
                  .map(this::setDeleted)
                  .filter(Entity::modified)
                  .toList());
}

// Custom UI layout, EntityPanel, EntityEditPanel, EntityTablePanel
@Override
protected void initializeUI() {
    setLayout(gridBagLayout());
    // Custom layout ignoring standard panel patterns
}
```

### State-Based UI Control

Coordinate complex UI states:

```java
State ready = State.and(
    processing.not(),
    hasPrompt,
    modelSelected
);

Control submitControl = Control.builder()
    .command(this::submitPrompt)
    .enabled(ready)
    .build();
```

### Benefits of This Approach

1. **Database abstraction** - EntityConnection handles connections, transactions, and pooling
2. **Type safety** - Entity definitions provide compile-time column checking
3. **Flexibility** - Use as much or as little of the framework as needed
4. **Clean architecture** - Clear separation between data persistence and UI logic
5. **Rapid development** - Leverage Entity infrastructure without the constraints

This hybrid approach is ideal for applications that need database persistence but have unique UI requirements that don't fit traditional CRUD patterns.

## Complex Domain Patterns

The World and Chinook demos showcase advanced domain modeling capabilities that go beyond basic CRUD operations.

### Custom Types and Converters

Define custom value types with database converters:

```java
// World demo - Geographic location
record Location(double latitude, double longitude) implements Serializable {
    @Override
    public String toString() {
        return "[" + latitude + "," + longitude + "]";
    }
}

// Define column with custom type
Column<Location> LOCATION = TYPE.column("location", Location.class);

// LocationConverter handles database ↔ Java conversion
LOCATION.define()
    .column()
    .columnClass(String.class, new LocationConverter());
```

### Array/Collection Columns

Support for database arrays:

```java
// Chinook demo - Tags array
Column<List<String>> TAGS = TYPE.column("tags", new TypeReference<>() {});

// TagsConverter handles SQL Array ↔ List<String>
TAGS.define()
    .column()
    .columnClass(Array.class, new TagsConverter());
```

### Cross-Entity Validation

Validate data across entity relationships:

```java
class CityValidator extends DefaultEntityValidator {
    @Override
    public <T> void validate(Entity city, Attribute<T> attribute) {
        super.validate(city, attribute);
        if (attribute.equals(City.POPULATION)) {
            Integer cityPopulation = city.get(City.POPULATION);
            Entity country = city.get(City.COUNTRY_FK);
            Integer countryPopulation = country.get(Country.POPULATION);
            if (countryPopulation != null && cityPopulation > countryPopulation) {
                throw new ValidationException(City.POPULATION, cityPopulation, 
                    "City population can not exceed country population");
            }
        }
    }
}
```

### Derived Attributes

Calculate values dynamically from other attributes:

```java
// World demo - Calculate speakers from percentage and population
Attribute<Integer> NO_OF_SPEAKERS = TYPE.integerAttribute("noOfSpeakers");

CountryLanguage.NO_OF_SPEAKERS.define()
     .derived(CountryLanguage.COUNTRY_FK, CountryLanguage.PERCENTAGE)
     .provider(new NoOfSpeakersProvider())
     .caption("No. of speakers")
     .numberFormatGrouping(true)

class NoOfSpeakers implements DerivedValue<Integer> {
    @Override
    public Integer get(SourceValues source) {
        Double percentage = source.get(CountryLanguage.PERCENTAGE);
        Entity country = source.get(CountryLanguage.COUNTRY_FK);
        if (percentage != null && country != null) {
            Integer population = country.get(Country.POPULATION);
            return (int)(population * (percentage / 100));
        }
        return null;
    }
}

//Derived values do not need to have source values
SomeEntity.RANDOM.define()
     .derived()
     .value(source -> randomNumber())
```

### Database Functions and Procedures

Expose database logic through the domain:

```java
// Chinook demo - Batch operations
FunctionType<EntityConnection, BigDecimal, Integer> RAISE_PRICE = 
    functionType("chinook.raise_price");

// Complex function with custom parameters
FunctionType<EntityConnection, RandomPlaylistParameters, Entity> RANDOM_PLAYLIST = 
    functionType("chinook.random_playlist");

record RandomPlaylistParameters(String playlistName, Integer noOfTracks, 
    Collection<Entity> genres) implements Serializable {}
```

### Performance Optimizations

#### Denormalized Attributes
```java
// World demo - simply fetches the the denormalized value from the referenced entity, if available
Country.CAPITAL_POPULATION.define()
    .denormalized(Country.CAPITAL_FK, City.POPULATION)
```

#### Reference Depth Control
```java
// Chinook demo - Control how deep to fetch relationships
Track.ALBUM_FK.define()
    .foreignKey()
    .referenceDepth(1);  // Default, also fetches the Album's Artist

PlaylistTrack.TRACK_FK.define()
    .foreignKey()
    .referenceDepth(2);  // Fetch Track → Album → Artist
```

#### Lazy Loading
```java
// Don't load expensive columns by default
Country.FLAG.define()
    .column()
    .selected(false);  // Only loaded when explicitly requested
```

### Advanced Query Features

#### Custom Conditions
```java
// Chinook demo - Parameterized WHERE conditions
ConditionType NOT_IN_PLAYLIST = TYPE.conditionType("notInPlaylist");

// Usage with custom SQL
List<Long> classicalPlaylistIds = List.of(42L, 43L);

Condition noneClassical = Track.NOT_IN_PLAYLIST.get(
    Playlist.ID, classicalPlaylistIds);

List<Entity> tracks = connection.select(noneClassical);
```

#### Subquery Columns
```java
// World demo - Count related entities
Country.NO_OF_CITIES.define()
    .subquery("SELECT COUNT(*) FROM world.city WHERE countrycode = country.code")
```

### Authentication Integration

Implement custom authentication with the domain, used by the EntityServer when establishing a client connection, without such an authenticator to server simply delegates the authentication to the database.

```java
// Chinook demo - User authentication
public final class ChinookAuthenticator implements Authenticator { 
	@Override
	public RemoteClient login(RemoteClient remoteClient) throws LoginException {
		authenticateUser(remoteClient.user());
		//Create a new RemoteClient based on the one received
		//but with the actual database user
		return remoteClient.withDatabaseUser(databaseUser);
	}

	private void authenticateUser(User user) throws LoginException {
		try (EntityConnection connection = fetchConnectionFromPool()) {
			int rows = connection.count(where(and(
							Authentication.User.USERNAME
											.equalToIgnoreCase(user.username()),
							Authentication.User.PASSWORD_HASH
											.equalTo(valueOf(user.password()).hashCode()))));
			if (rows == 0) {
				throw new ServerAuthenticationException("Wrong username or password");
			}
		}
	}
}
```

### Reporting Integration

Embed reports in the domain model:

```java
// Chinook demo - JasperReports integration
JRReportType REPORT = JasperReports.reportType("customer_report");

// Domain model
public Employees() {
    super(DOMAIN);
    add(Employee.EMPLOYEE_REPORT, classPathReport(Employees.class, "employees.jasper"));
}

// Reports are loaded from classpath, filsystem or over http and can be filled via EntityConnection
JasperPrint employeeReport = tableModel().connection()
		.report(Employee.EMPLOYEE_REPORT, reportParameters);

Dialogs.componentDialog(new JRViewer(employeeReport)).show();
```

### Key Principles for Complex Domains

1. **Type Safety First** - Use custom types rather than primitive obsession
2. **Push Logic Down** - Database functions for complex operations
3. **Optimize Fetching** - Control reference depth and lazy loading
4. **Validate Early** - Domain validators catch errors before database
5. **Leverage Database Features** - Arrays, views, functions, procedures
6. **Performance by Design** - Denormalization, subqueries, smart fetching

## Understanding Codion Code

### Common Patterns You'll See

1. **Builder Pattern Everywhere**
   ```java
   // Nearly every component uses builders
   	EntityPanel.builder(Artist.TYPE, ChinookAppPanel::createArtistPanel)
		.caption("Artist")
		.description("Manage artists");
   ```

2. **Observable Values Replace Direct Fields**
   ```java
   // Instead of: private String name;
   private final Value<String> name = Value.nullable();
   
   // Usage
   name.set("John");
   name.addListener(this::updateUI);
   name.addConsumer(newName -> updateUI(newName));
   ```

3. **Foreign Key References**
   ```java
   // Access foreign key entity
   Entity invoice = ...;
   Entity customer = invoice.get(Invoice.CUSTOMER_FK);
   
   // Access foreign key attribute directly, be mindful of referenceDepth  
   String customerName = invoice.get(Invoice.CUSTOMER_FK).get(Customer.NAME);
   ```

4. **Condition Building**
   ```java
   // Type-safe query conditions
   Condition condition = and(
       Track.ARTIST_FK.equalTo(artist),
       Track.GENRE_FK.in(genres),
       Track.RATING.greaterThanOrEqualTo(4)
   );
   ```

### Performance and Scale

- The framework can handle 7000+ concurrent users (see `documentation/src/docs/asciidoc/images/monitoring` screenshots)
- Complex domains with 50+ entities work smoothly
- Check `documentation/src/docs/asciidoc/images/complexity.png` to see how code complexity has decreased over time despite growth

### Documentation and Learning Resources

**Start Here:**
0. **The Readme** - `readme.adoc`- Contains screenshots of all five demos (three of the demos can be found in this repo)
1. **The Manual** - `documentation/src/docs/asciidoc/manual/` - Comprehensive guide organized by topic
   - `framework-conditions.adoc` - Deep dive into the condition framework
   - `framework-domain-model.adoc` - Understanding entity modeling
   - `framework-entity-connection.adoc` - Database operations guide
   - Start with `manual.adoc` for the table of contents

2. **Demo Applications** - Working examples showing real-world usage
   - **Petclinic** (`demos/petclinic/`) - Classic example, bare-bones, good starting point
   - **World** (`demos/world/`) - Shows geographic data, custom types, complex queries
   - **Chinook** (`demos/chinook/`) - Music store database, most comprehensive example, kitchen sink

3. **When Working on Javadocs:**
   - Look for `*Demo.java` classes in demos for usage examples
   - Check the manual's corresponding `.adoc` file for conceptual understanding
   - Cross-reference with actual usage in demo applications
   - Remember that demos use `{url-javadoc}` links extensively

4. **Code Navigation Tips:**
   - Entity definitions are in `domain/api/` packages
   - Entity implementations are in `domain/` or `domain/impl/` packages
   - Look for `TYPE.define()` to see how entities are configured
   - Search for `.conditionType()` to find custom condition examples

### Best Practices

1. **Use the Observable Pattern** - Don't fight it, embrace it
2. **Let the Database Work** - Use views, functions, procedures
3. **Builder Pattern** - Use it for consistency and discoverability
4. **Type Safety** - Leverage the type system, avoid string keys
5. **Test at the Right Level** - Unit test domains, integration test with H2
6. **Read the Manual** - Seriously, it's excellent and will save you time

## Documentation Code Examples

Codion documentation follows a strict pattern for code examples to ensure they stay synchronized with the actual codebase and are automatically refactored along with the code.

### The Golden Rule: Never Embed Java Code Directly

**❌ Wrong - Embedded code in AsciiDoc:**
```asciidoc
[source,java]
----
SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);
SwingEntityModel orderModel = new SwingEntityModel(Order.TYPE, connectionProvider);
customerModel.detailModels().add(orderModel);
----
```

**✅ Correct - Tagged includes from source files:**
```asciidoc
:dir-chinook-source: ../../../../../demos/chinook/src/main/java

[source,java]
----
include::{dir-chinook-source}/is/codion/demos/chinook/manual/FrameworkModelDemo.java[tags=entityModel]
----
```

### Code Example Process

1. **Create demo classes** in appropriate demo projects (e.g., `demos/chinook/src/main/java/is/codion/demos/chinook/manual/`)

2. **Use tags to mark sections:**
```java
void entityModel(EntityConnectionProvider connectionProvider) {
    // tag::entityModel[]
    SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);
    SwingEntityModel invoiceModel = new SwingEntityModel(Invoice.TYPE, connectionProvider);
    
    // Establish master-detail relationship
    customerModel.detailModels().add(invoiceModel);
    // end::entityModel[]
}
```

3. **Use the parameter trick** for dependencies:
   - Methods take `EntityConnectionProvider connectionProvider` as parameter
   - Avoids complex setup code in examples
   - Keeps examples focused on the actual API usage

4. **Include in documentation:**
```asciidoc
:dir-chinook-source: ../../../../../demos/chinook/src/main/java

include::{dir-chinook-source}/is/codion/demos/chinook/manual/FrameworkModelDemo.java[tags=entityModel]
```

### Benefits of This Approach

- **Automatic Refactoring**: Code examples are updated when APIs change
- **Compilation Guaranteed**: Examples must compile or the build fails
- **IDE Support**: Full IntelliJ/IDE support for code examples
- **Consistency**: All examples follow the same patterns
- **Maintenance**: No manual synchronization between docs and code

### Demo Class Organization

Each major documentation section has a corresponding demo class:
- `FrameworkModelDemo.java` - Model layer examples
- `DomainDemo.java` - Domain modeling examples
- `UIDemo.java` - UI component examples

### Existing Demo Projects

Use these demo projects for realistic examples:
- **Chinook** - Most comprehensive, kitchen sink examples
- **Petclinic** - Simple CRUD patterns
- **World** - Advanced domain modeling, custom types
- **Employees** - Business logic examples

Never create artificial examples when real demo entities are available!

### Documentation URL Pattern

Codion uses a special URL pattern for Javadoc links to maintain compatibility across different JDK versions:

```asciidoc
:url-javadoc: link:../api

// Then use with module name placeholders:
{url-javadoc}{framework-model}/is/codion/framework/model/EntityModel.html
{url-javadoc}{swing-framework-ui}/is/codion/swing/framework/ui/component/EntitySearchField.html
```

The module name placeholders (e.g., `{framework-model}`, `{swing-framework-ui}`) are defined in `documentation/build.gradle.kts` in the `tasks.asciidoctor` section. This pattern allows:
- JDK 8 branch to have empty placeholders (no module system)
- JDK 9+ branches to insert the proper module path
- Single documentation source that works across all branches

## API Refinement Window

**IMPORTANT**: Codion is in its final API refinement phase before promotion. The change window is **closing in the next few months** (open until promotion or when someone provably starts using it). After that, backward compatibility will be maintained.

### What This Means

- **Now is the time** for breaking changes and API improvements
- Every suboptimal name fixed now is one we won't be stuck with forever
- After 20+ years of development and 3 years of intensive polishing, we're in the final sprint

### How You Can Help

If you spot any of these while working with Codion, **please speak up immediately**:

1. **Redundant context** - Like `modifiesEntity()` in an attribute class → `modifies()`
2. **Unnecessary prefixes/suffixes** - Like `WHEN_SET` → `SET`
3. **Verbose names** where shorter would be clearer
4. **Inconsistencies** with established patterns
5. **Confusing method names** that could be more intuitive
6. **Parameter names** that could be clearer

### Examples of Recent Refinements

- `Notify.WHEN_SET` → `Notify.SET` (removed redundant prefix)
- `TransientAttributeDefinition.modifiesEntity()` → `modifies()` (removed redundant context)
- 360+ renames and 237+ removals over the past 3 years (see `changelog.md`)

Remember: Once the API freezes, these names are forever. Help make Codion something we'll all be happy using and maintaining for the next 20 years!