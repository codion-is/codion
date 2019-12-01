/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.domain;

import org.jminor.common.db.DatabaseConnection;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.KeyGenerator;
import org.jminor.framework.domain.StringProvider;

import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import static org.jminor.framework.domain.property.Properties.*;

public final class Store extends Domain {

  public static final String T_ADDRESS = "store.address";
  public static final String ADDRESS_ID = "id";
  public static final String ADDRESS_STREET = "street";
  public static final String ADDRESS_CITY = "city";

  public static final String T_CUSTOMER = "store.customer";
  public static final String CUSTOMER_ID = "id";
  public static final String CUSTOMER_FIRST_NAME = "first_name";
  public static final String CUSTOMER_LAST_NAME = "last_name";
  public static final String CUSTOMER_EMAIL = "email";
  public static final String CUSTOMER_ADDRESS_ID = "address_id";
  public static final String CUSTOMER_ADDRESS_FK = "address_fk";
  public static final String CUSTOMER_IS_ACTIVE = "is_active";

  public Store() {
    address();
    customer();
  }

  private void address() {
    // tag::address[]
    define(T_ADDRESS,
            primaryKeyProperty(ADDRESS_ID),
            columnProperty(ADDRESS_STREET, Types.VARCHAR, "Street")
                    .setNullable(false).setMaxLength(120),
            columnProperty(ADDRESS_CITY, Types.VARCHAR, "City")
                    .setNullable(false).setMaxLength(50))
            .setStringProvider(new StringProvider(ADDRESS_STREET)
                    .addText(", ").addValue(ADDRESS_CITY));
    // end::address[]
  }

  private void customer() {
    // tag::customer[]
    define(T_CUSTOMER,
            primaryKeyProperty(CUSTOMER_ID, Types.VARCHAR),
            columnProperty(CUSTOMER_FIRST_NAME, Types.VARCHAR, "First name")
                    .setNullable(false).setMaxLength(40),
            columnProperty(CUSTOMER_LAST_NAME, Types.VARCHAR, "Last name")
                    .setNullable(false).setMaxLength(40),
            columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email"),
            foreignKeyProperty(CUSTOMER_ADDRESS_FK, "Address", T_ADDRESS,
                    columnProperty(CUSTOMER_ADDRESS_ID)),
            columnProperty(CUSTOMER_IS_ACTIVE, Types.BOOLEAN, "Is active")
                    .setColumnHasDefaultValue(true).setDefaultValue(true))
            .setKeyGenerator(new UUIDKeyGenerator())
            .setStringProvider(new CustomerToString());
    // end::customer[]
  }

  // tag::toString[]
  private static final class CustomerToString implements Entity.ToString {

    @Override
    public String toString(final Entity customer) {
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
    public void beforeInsert(final Entity entity, final DatabaseConnection connection)
            throws SQLException {
      entity.put(CUSTOMER_ID, UUID.randomUUID().toString());
    }

    @Override
    public boolean isInserted() {
      return true;
    }
  }
  // end::keyGenerator[]
}
