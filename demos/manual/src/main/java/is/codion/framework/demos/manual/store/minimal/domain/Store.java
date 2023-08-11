/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.util.function.Function;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.property.Property.*;

public class Store extends DefaultDomain {

  static final DomainType DOMAIN = domainType(Store.class);

  public interface Customer {
    EntityType TYPE = DOMAIN.entityType("store.customer");

    Column<Long> ID = TYPE.longColumn("id");
    Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
    Column<String> LAST_NAME = TYPE.stringColumn("last_name");
    Column<String> EMAIL = TYPE.stringColumn("email");
    Column<Boolean> IS_ACTIVE = TYPE.booleanColumn("is_active");
  }

  public interface Address {
    EntityType TYPE = DOMAIN.entityType("store.address");

    Column<Long> ID = TYPE.longColumn("id");
    Column<Long> CUSTOMER_ID = TYPE.longColumn("customer_id");
    Column<String> STREET = TYPE.stringColumn("street");
    Column<String> CITY = TYPE.stringColumn("city");

    ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
  }

  public Store() {
    super(DOMAIN);

    add(definition(
            primaryKeyProperty(Customer.ID),
            columnProperty(Customer.FIRST_NAME, "First name")
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(Customer.LAST_NAME, "Last name")
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(Customer.EMAIL, "Email")
                    .maximumLength(100),
            columnProperty(Customer.IS_ACTIVE, "Is active")
                    .nullable(false)
                    .defaultValue(true))
            .keyGenerator(identity())
            .stringFactory(StringFactory.builder()
                    .value(Customer.LAST_NAME)
                    .text(", ")
                    .value(Customer.FIRST_NAME)
                    .build())
            .caption("Customer"));

    add(definition(
            primaryKeyProperty(Address.ID),
            columnProperty(Address.CUSTOMER_ID)
                    .nullable(false),
            foreignKeyProperty(Address.CUSTOMER_FK, "Customer"),
            columnProperty(Address.STREET, "Street")
                    .nullable(false)
                    .maximumLength(100),
            columnProperty(Address.CITY, "City")
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
    ColumnProperty.Builder<Long, ?> id =
            Property.primaryKeyProperty(Address.ID);

    ColumnProperty.Builder<Long, ?> customerId =
            Property.columnProperty(Address.CUSTOMER_ID)
                    .nullable(false);

    ForeignKeyProperty.Builder customerFk =
            Property.foreignKeyProperty(Address.CUSTOMER_FK, "Customer");

    ColumnProperty.Builder<String, ?> street =
            Property.columnProperty(Address.STREET, "Street")
                    .nullable(false)
                    .maximumLength(100);

    ColumnProperty.Builder<String, ?> city =
            Property.columnProperty(Address.CITY, "City")
                    .nullable(false)
                    .maximumLength(50);

    KeyGenerator keyGenerator = KeyGenerator.identity();

    Function<Entity, String> stringFactory = StringFactory.builder()
            .value(Address.STREET)
            .text(", ")
            .value(Address.CITY)
            .build();

    EntityDefinition.Builder address =
            EntityDefinition.definition(id, customerId, customerFk, street, city)
                    .keyGenerator(keyGenerator)
                    .stringFactory(stringFactory)
                    .caption("Address");

    add(address);
  }
}
