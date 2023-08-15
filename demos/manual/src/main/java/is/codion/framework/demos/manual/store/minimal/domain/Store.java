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

    add(Customer.TYPE.define(
            Customer.ID
                    .primaryKey(),
            Customer.FIRST_NAME
                    .column()
                    .caption("First name")
                    .nullable(false)
                    .maximumLength(40),
            Customer.LAST_NAME
                    .column()
                    .caption("Last name")
                    .nullable(false)
                    .maximumLength(40),
            Customer.EMAIL
                    .column()
                    .caption("Email")
                    .maximumLength(100),
            Customer.IS_ACTIVE
                    .column()
                    .caption("Is active")
                    .nullable(false)
                    .defaultValue(true))
            .keyGenerator(identity())
            .stringFactory(StringFactory.builder()
                    .value(Customer.LAST_NAME)
                    .text(", ")
                    .value(Customer.FIRST_NAME)
                    .build())
            .caption("Customer"));

    add(Address.TYPE.define(
            Address.ID
                    .primaryKey(),
            Address.CUSTOMER_ID
                    .column()
                    .nullable(false),
            Address.CUSTOMER_FK
                    .foreignKey()
                    .caption("Customer"),
            Address.STREET
                    .column()
                    .caption("Street")
                    .nullable(false)
                    .maximumLength(100),
            Address.CITY
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
            Address.ID
                    .primaryKey();

    ColumnDefinition.Builder<Long, ?> customerId =
            Address.CUSTOMER_ID
                    .column()
                    .nullable(false);

    ForeignKeyDefinition.Builder customerFk =
            Address.CUSTOMER_FK
                    .foreignKey()
                    .caption("Customer");

    ColumnDefinition.Builder<String, ?> street =
            Address.STREET
                    .column()
                    .caption("Street")
                    .nullable(false)
                    .maximumLength(100);

    ColumnDefinition.Builder<String, ?> city =
            Address.CITY
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
