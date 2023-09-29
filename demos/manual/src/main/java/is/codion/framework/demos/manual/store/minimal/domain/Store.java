/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import java.util.function.Function;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;

// Extend the DefaultDomain class.
public class Store extends DefaultDomain {

  // Create a DomainType constant identifying the domain model.
  public static final DomainType DOMAIN = domainType(Store.class);

  // Create a namespace interface for the Customer entity.
  public interface Customer {
    // Use the DomainType and the table name to create an
    // EntityType constant identifying the entity.
    EntityType TYPE = DOMAIN.entityType("store.customer");

    // Use the EntityType to create typed Column constants for each column.
    Column<Long> ID = TYPE.longColumn("id");
    Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
    Column<String> LAST_NAME = TYPE.stringColumn("last_name");
    Column<String> EMAIL = TYPE.stringColumn("email");
    Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
  }

  // Create a namespace interface for the Address entity.
  public interface Address {
    EntityType TYPE = DOMAIN.entityType("store.address");

    Column<Long> ID = TYPE.longColumn("id");
    Column<Long> CUSTOMER_ID = TYPE.longColumn("customer_id");
    Column<String> STREET = TYPE.stringColumn("street");
    Column<String> CITY = TYPE.stringColumn("city");

    // Use the EntityType to create a ForeignKey
    // constant for the foreign key relationship.
    ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
  }

  public Store() {
    super(DOMAIN);
    // Use the Customer.TYPE constant to define a new entity,
    // based on attributes defined using the Column constants.
    // This entity definition is then added to the domain model.
    add(Customer.TYPE.define(           // returns EntityDefinition.Builder
            Customer.ID.define()
                    .primaryKey(),      // returns ColumnDefinition.Builder
            Customer.FIRST_NAME.define()
                    .column()           // returns ColumnDefinition.Builder
                    .caption("First name")
                    .nullable(false)
                    .maximumLength(40),
            Customer.LAST_NAME.define()
                    .column()
                    .caption("Last name")
                    .nullable(false)
                    .maximumLength(40),
            Customer.EMAIL.define()
                    .column()
                    .caption("Email")
                    .maximumLength(100),
            Customer.ACTIVE.define()
                    .column()
                    .caption("Active")
                    .nullable(false)
                    .defaultValue(true))
            .keyGenerator(identity())
            .stringFactory(StringFactory.builder()
                    .value(Customer.LAST_NAME)
                    .text(", ")
                    .value(Customer.FIRST_NAME)
                    .build())
            .caption("Customer"));

    // Use the Address.TYPE constant to define a new entity,
    // based on attributes defined using the Column and ForeignKey constants.
    // This entity definition is then added to the domain model.
    add(Address.TYPE.define(
            Address.ID.define()
                    .primaryKey(),
            Address.CUSTOMER_ID.define()
                    .column()
                    .nullable(false),
            Address.CUSTOMER_FK.define()
                    .foreignKey()       // returns ForeignKeyDefinition.Builder
                    .caption("Customer"),
            Address.STREET.define()
                    .column()
                    .caption("Street")
                    .nullable(false)
                    .maximumLength(100),
            Address.CITY.define()
                    .column()
                    .caption("City")
                    .nullable(false)
                    .maximumLength(50))
            .keyGenerator(identity())
            .stringFactory(StringFactory.builder()
                    .value(Address.STREET)
                    .text(", ")
                    .value(Address.CITY)
                    .build())
            .caption("Address"));
  }

  void addressExpanded() {
    ColumnDefinition.Builder<Long, ?> id =
            Address.ID.define()
                    .primaryKey();

    ColumnDefinition.Builder<Long, ?> customerId =
            Address.CUSTOMER_ID.define()
                    .column()
                    .nullable(false);

    ForeignKeyDefinition.Builder customerFk =
            Address.CUSTOMER_FK.define()
                    .foreignKey()
                    .caption("Customer");

    ColumnDefinition.Builder<String, ?> street =
            Address.STREET.define()
                    .column()
                    .caption("Street")
                    .nullable(false)
                    .maximumLength(100);

    ColumnDefinition.Builder<String, ?> city =
            Address.CITY.define()
                    .column()
                    .caption("City")
                    .nullable(false)
                    .maximumLength(50);

    KeyGenerator keyGenerator = KeyGenerator.identity();

    Function<Entity, String> stringFactory = StringFactory.builder()
            .value(Address.STREET)
            .text(", ")
            .value(Address.CITY)
            .build();

    EntityDefinition.Builder address =
            Customer.TYPE.define(id, customerId, customerFk, street, city)
                    .keyGenerator(keyGenerator)
                    .stringFactory(stringFactory)
                    .caption("Address");

    add(address);
  }
}
