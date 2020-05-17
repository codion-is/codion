/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;

import java.sql.Types;

import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.*;

public class Store extends Domain {

  public static final String T_CUSTOMER = "store.customer";
  public static final String CUSTOMER_ID = "id";
  public static final String CUSTOMER_FIRST_NAME = "first_name";
  public static final String CUSTOMER_LAST_NAME = "last_name";
  public static final String CUSTOMER_EMAIL = "email";
  public static final String CUSTOMER_IS_ACTIVE = "is_active";

  public static final String T_ADDRESS = "store.address";
  public static final String ADDRESS_ID = "id";
  public static final String ADDRESS_CUSTOMER_FK = "customer_fk";
  public static final String ADDRESS_CUSTOMER_ID = "customer_id";
  public static final String ADDRESS_STREET = "street";
  public static final String ADDRESS_CITY = "city";

  public Store() {
    define(T_CUSTOMER,
            primaryKeyProperty(CUSTOMER_ID, Types.INTEGER),
            columnProperty(CUSTOMER_FIRST_NAME, Types.VARCHAR, "First name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_LAST_NAME, Types.VARCHAR, "Last name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email")
                    .maximumLength(100),
            columnProperty(CUSTOMER_IS_ACTIVE, Types.BOOLEAN, "Is active")
                    .defaultValue(true))
            .keyGenerator(automatic("store.customer"))
            .stringProvider(new StringProvider(CUSTOMER_LAST_NAME)
                    .addText(", ").addValue(CUSTOMER_FIRST_NAME))
            .caption("Customer");

    define(T_ADDRESS,
            primaryKeyProperty(ADDRESS_ID, Types.INTEGER),
            foreignKeyProperty(ADDRESS_CUSTOMER_FK, "Customer", T_CUSTOMER,
                    columnProperty(ADDRESS_CUSTOMER_ID))
                    .nullable(false),
            columnProperty(ADDRESS_STREET, Types.VARCHAR, "Street")
                    .nullable(false).maximumLength(100),
            columnProperty(ADDRESS_CITY, Types.VARCHAR, "City")
                    .nullable(false).maximumLength(50))
            .keyGenerator(automatic("store.address"))
            .stringProvider(new StringProvider(ADDRESS_STREET)
                    .addText(", ").addValue(ADDRESS_CITY))
            .caption("Address");
  }
}
