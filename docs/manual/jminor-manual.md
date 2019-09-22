###### JMinor Application Framework

##### Architecture

A typical JMinor application has two layers, the model and the UI. The model encapsulates the application data, communicates with the underlying database and contains most of the business logic whereas the UI displays the data and provides ways to modify it. The model layer works with a domain model which defines the tables, columns, primary and foreign keys used by the application.

#### Domain Model

The JMinor framework is not an ORM (Object-relational mapping) framework, instead the database structure is modelled much like an E/R diagram, with each table represented by an Entity, each column by a Property and each foreign key relationship with a ForeignKeyProperty.

The database structure is defined in the [Domain](https://heima.hafro.is/~darri/jminor_wiki_data/project/docs/api/org/jminor/framework/domain/Domain.html) class. An instance of this class contains a [Entity.Definition](https://heima.hafro.is/~darri/jminor_wiki_data/project/docs/api/org/jminor/framework/domain/Entity.Definition.html) for each table, which in turn contains a [Property](https://heima.hafro.is/~darri/jminor_wiki_data/project/docs/api/org/jminor/framework/domain/Property.html) instance for each column in the table. Actual rows in a table are represented by the [Entity](https://heima.hafro.is/~darri/jminor_wiki_data/project/docs/api/org/jminor/framework/domain/Entity.html) class which contains the column values mapped to their respective Properties.

```java
import static org.jminor.framework.domain.Properties.*;

public class Store extends Domain {

  //String constants for the entity and it's properties,
  //the entityId and propertyIds respectively
  public static final String T_CUSTOMER = "store.customer";
  public static final String CUSTOMER_ID = "id";
  public static final String CUSTOMER_FIRST_NAME = "first_name";
  public static final String CUSTOMER_LAST_NAME = "last_name";
  public static final String CUSTOMER_ACTIVE = "active";
  
  public Store() {
    defineCustomer();
  }
  
  private void defineCustomer() {
    Property id = primaryKeyProperty(CUSTOMER_ID, Types.INTEGER);
    Property firstName = columnProperty(CUSTOMER_FIRST_NAME, Types.VARCHAR, "First name");
    Property lastName = columnProperty(CUSTOMER_LAST_NAME, Types.VARCHAR, "Last name");
    Property active = booleanProperty(CUSTOMER_ACTIVE, Types.BOOLEAN, "Active", 1, 0);

    define(T_CUSTOMER, id, firstName, lastName, active);
  }
}
```

As well as serving as a container for the domain model the Domain object serves as a factory for Entity and Entity.Key instances.

```java
Store store = new Store();

List<Property> customerProperties = store.getProperties(Store.T_CUSTOMER);

Entity john = store.entity(Store.T_CUSTOMER);

john.put(Store.CUSTOMER_ID, 1);
john.put(Store.CUSTOMER_FIRST_NAME, "John");
john.put(Store.CUSTOMER_LAST_NAME, "Doe");
john.put(Store.CUSTOMER_ACTIVE, true);

String firstName = john.getString(Store.CUSTOMER_FIRST_NAME);

Entity.Key johnKey = store.key(Store.T_CUSTOMER);
johnKey.put(Store.CUSTOMER_ID, 2);
```

Each table is identified by a String constant (which must be unique within the domain), called **entityId**, these should be defined in the Domain class. In the example below the **entityId** constants are prefixed by **T_**.

```java
public class Store extends Domain {
  
  /** entityId representing the "address" table in the "store" schema */
  public static final String T_ADDRESS = "store.address";
  
  /** entityId representing the "customer" table in the "store" schema */
  public static final String T_CUSTOMER = "store.customer";

}
```
Each column in a table is identified by a String constant (which must be unique within its Entity) called **propertyId** and is represented by the [Property](https://heima.hafro.is/~darri/jminor_wiki_data/project/docs/api/org/jminor/framework/domain/Property.html) class or one of its subclasses.

```java
public class Store extends Domain {
  
  /** entityId representing the "address" table in the "store" schema */
  public static final String T_ADDRESS = "store.address";

  /** propertIds for the columns in the "address" table */
  public static final String ADDRESS_ID = "id";
  public static final String ADDRESS_STREET = "street";
  public static final String ADDRESS_CITY = "city";
 
  /** entityId representing the "customer" table in the "store" schema */
  public static final String T_CUSTOMER = "store.customer";
 
  /** propertIds for the columns in the "customer" table */
  public static final String CUSTOMER_ID = "id";
  public static final String CUSTOMER_FIRST_NAME = "first_name";
  public static final String CUSTOMER_LAST_NAME = "last_name";
  public static final String CUSTOMER_ADDRESS_FK = "address_fk";
  public static final String CUSTOMER_ADDRESS_ID = "address_id";
  public static final String CUSTOMER_IS_ACTIVE = "is_active";
  public static final String CUSTOMER_CITY = "city";
  public static final String CUSTOMER_DERIVED = "derived";

}
```

### Entity

An Entity instance is a map-like structure, mapping column values to their respective properties via **put()** and **get()** methods with a few useful features added.

#### Model Layer

### EntityModel

The EntityModel class coordinates two other classes, the EntityEditModel and the EntityTableModel, which are concerned with editing a 

The EntityModel class is the base class for working with entities, it is comprised of a EntityEditModel, for editing a entity instance, and a EntityTableModel for working with multiple entities. The EntityModel coordinates between the edit model and the table model.