/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.domain;

import dev.codion.common.db.connection.DatabaseConnection;
import dev.codion.framework.domain.Domain;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.entity.KeyGenerator;
import dev.codion.framework.domain.entity.StringProvider;
import dev.codion.framework.domain.property.ColumnProperty;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static dev.codion.framework.domain.entity.KeyGenerators.automatic;
import static dev.codion.framework.domain.property.Properties.*;

public final class Store extends Domain {

  public static final String T_ADDRESS = "store.address";
  public static final String ADDRESS_ID = "id";
  public static final String ADDRESS_STREET = "street";
  public static final String ADDRESS_CITY = "city";
  public static final String ADDRESS_VALID = "valid";

  public static final String T_CUSTOMER = "store.customer";
  public static final String CUSTOMER_ID = "id";
  public static final String CUSTOMER_FIRST_NAME = "first_name";
  public static final String CUSTOMER_LAST_NAME = "last_name";
  public static final String CUSTOMER_EMAIL = "email";
  public static final String CUSTOMER_IS_ACTIVE = "is_active";

  public static final String T_CUSTOMER_ADDRESS = "store.customer_address";
  public static final String CUSTOMER_ADDRESS_ID = "id";
  public static final String CUSTOMER_ADDRESS_CUSTOMER_ID = "customer_id";
  public static final String CUSTOMER_ADDRESS_CUSTOMER_FK = "customer_fk";
  public static final String CUSTOMER_ADDRESS_ADDRESS_ID = "address_id";
  public static final String CUSTOMER_ADDRESS_ADDRESS_FK = "address_fk";

  public Store() {
    customer();
    address();
    customerAddress();
  }

  private void customer() {
    // tag::customer[]
    define(T_CUSTOMER,
            primaryKeyProperty(CUSTOMER_ID, Types.VARCHAR),
            columnProperty(CUSTOMER_FIRST_NAME, Types.VARCHAR, "First name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_LAST_NAME, Types.VARCHAR, "Last name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email"),
            columnProperty(CUSTOMER_IS_ACTIVE, Types.BOOLEAN, "Is active")
                    .columnHasDefaultValue(true).defaultValue(true))
            .keyGenerator(new UUIDKeyGenerator())
            .stringProvider(new CustomerToString())
            .caption("Customer");
    // end::customer[]
  }

  private void address() {
    // tag::address[]
    define(T_ADDRESS,
            primaryKeyProperty(ADDRESS_ID, Types.INTEGER),
            columnProperty(ADDRESS_STREET, Types.VARCHAR, "Street")
                    .nullable(false).maximumLength(120),
            columnProperty(ADDRESS_CITY, Types.VARCHAR, "City")
                    .nullable(false).maximumLength(50),
            columnProperty(ADDRESS_VALID, Types.BOOLEAN, "Valid")
                    .columnHasDefaultValue(true).nullable(false))
            .stringProvider(new StringProvider(ADDRESS_STREET)
                    .addText(", ").addValue(ADDRESS_CITY))
            .keyGenerator(automatic(T_ADDRESS))
            .smallDataset(true)
            .caption("Address");
    // end::address[]
  }

  private void customerAddress() {
    // tag::customerAddress[]
    define(T_CUSTOMER_ADDRESS,
            primaryKeyProperty(CUSTOMER_ADDRESS_ID),
            foreignKeyProperty(CUSTOMER_ADDRESS_CUSTOMER_FK, "Customer", T_CUSTOMER,
                    columnProperty(CUSTOMER_ADDRESS_CUSTOMER_ID, Types.VARCHAR))
                    .nullable(false),
            foreignKeyProperty(CUSTOMER_ADDRESS_ADDRESS_FK, "Address", T_ADDRESS,
                    columnProperty(CUSTOMER_ADDRESS_ADDRESS_ID))
                    .nullable(false))
            .keyGenerator(automatic(T_CUSTOMER_ADDRESS))
            .caption("Customer address");
    // end::customerAddress[]
  }

  // tag::toString[]
  private static final class CustomerToString implements Function<Entity, String>, Serializable {

    @Override
    public String apply(final Entity customer) {
      StringBuilder builder =
              new StringBuilder(customer.getString(CUSTOMER_LAST_NAME))
                      .append(", ")
                      .append(customer.getString(CUSTOMER_FIRST_NAME));
      if (customer.isNotNull(CUSTOMER_EMAIL)) {
        builder.append(" <")
                .append(customer.getString(CUSTOMER_EMAIL))
                .append(">");
      }

      return builder.toString();
    }
  }
  // end::toString[]

  // tag::keyGenerator[]
  private static final class UUIDKeyGenerator implements KeyGenerator {

    @Override
    public void beforeInsert(final Entity entity, final List<ColumnProperty> primaryKeyProperties,
                             final DatabaseConnection connection) throws SQLException {
      entity.put(CUSTOMER_ID, UUID.randomUUID().toString());
    }
  }
  // end::keyGenerator[]
}
