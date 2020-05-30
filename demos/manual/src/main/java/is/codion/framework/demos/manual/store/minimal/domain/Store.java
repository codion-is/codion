/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;

import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Attributes.*;
import static is.codion.framework.domain.property.Properties.*;

public class Store extends Domain {

  public static final String T_CUSTOMER = "store.customer";
  public static final Attribute<Integer> CUSTOMER_ID = integerAttribute("id");
  public static final Attribute<String> CUSTOMER_FIRST_NAME = stringAttribute("first_name");
  public static final Attribute<String> CUSTOMER_LAST_NAME = stringAttribute("last_name");
  public static final Attribute<String> CUSTOMER_EMAIL = stringAttribute("email");
  public static final Attribute<Boolean> CUSTOMER_IS_ACTIVE = booleanAttribute("is_active");

  public static final String T_ADDRESS = "store.address";
  public static final Attribute<Integer> ADDRESS_ID = integerAttribute("id");
  public static final EntityAttribute ADDRESS_CUSTOMER_FK = entityAttribute("customer_fk");
  public static final Attribute<Integer> ADDRESS_CUSTOMER_ID = integerAttribute("customer_id");
  public static final Attribute<String> ADDRESS_STREET = stringAttribute("street");
  public static final Attribute<String> ADDRESS_CITY = stringAttribute("city");

  public Store() {
    define(T_CUSTOMER,
            primaryKeyProperty(CUSTOMER_ID),
            columnProperty(CUSTOMER_FIRST_NAME, "First name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_LAST_NAME, "Last name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_EMAIL, "Email")
                    .maximumLength(100),
            columnProperty(CUSTOMER_IS_ACTIVE, "Is active")
                    .defaultValue(true))
            .keyGenerator(automatic("store.customer"))
            .stringProvider(new StringProvider(CUSTOMER_LAST_NAME)
                    .addText(", ").addValue(CUSTOMER_FIRST_NAME))
            .caption("Customer");

    define(T_ADDRESS,
            primaryKeyProperty(ADDRESS_ID),
            foreignKeyProperty(ADDRESS_CUSTOMER_FK, "Customer", T_CUSTOMER,
                    columnProperty(ADDRESS_CUSTOMER_ID))
                    .nullable(false),
            columnProperty(ADDRESS_STREET, "Street")
                    .nullable(false).maximumLength(100),
            columnProperty(ADDRESS_CITY, "City")
                    .nullable(false).maximumLength(50))
            .keyGenerator(automatic("store.address"))
            .stringProvider(new StringProvider(ADDRESS_STREET)
                    .addText(", ").addValue(ADDRESS_CITY))
            .caption("Address");
  }
}
