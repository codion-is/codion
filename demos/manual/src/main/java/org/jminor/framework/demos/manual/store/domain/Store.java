/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.domain;

import org.jminor.framework.domain.Domain;

import java.sql.Types;

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
            columnProperty(ADDRESS_STREET, Types.VARCHAR, "Street"),
            columnProperty(ADDRESS_CITY, Types.VARCHAR, "City"))
            .setStringProvider(new StringProvider(ADDRESS_STREET)
                    .addText(", ").addValue(ADDRESS_CITY));
    // end::address[]
  }

  private void customer() {
    // tag::customer[]
    define(T_CUSTOMER,
            primaryKeyProperty(CUSTOMER_ID),
            columnProperty(CUSTOMER_FIRST_NAME, Types.VARCHAR, "First name"),
            columnProperty(CUSTOMER_LAST_NAME, Types.VARCHAR, "Last name"),
            columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email"),
            foreignKeyProperty(CUSTOMER_ADDRESS_FK, "Address", T_ADDRESS,
                    columnProperty(CUSTOMER_ADDRESS_ID)),
            columnProperty(CUSTOMER_IS_ACTIVE, Types.BOOLEAN, "Is active"))
            .setStringProvider(customer -> {
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
            });
    // end::customer[]
  }
}
