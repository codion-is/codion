/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringProvider;

import static is.codion.framework.domain.entity.Entities.type;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.*;

public class Store extends Domain {

  public static final EntityType T_CUSTOMER = type("store.customer");
  public static final Attribute<Integer> CUSTOMER_ID = T_CUSTOMER.integerAttribute("id");
  public static final Attribute<String> CUSTOMER_FIRST_NAME = T_CUSTOMER.stringAttribute("first_name");
  public static final Attribute<String> CUSTOMER_LAST_NAME = T_CUSTOMER.stringAttribute("last_name");
  public static final Attribute<String> CUSTOMER_EMAIL = T_CUSTOMER.stringAttribute("email");
  public static final Attribute<Boolean> CUSTOMER_IS_ACTIVE = T_CUSTOMER.booleanAttribute("is_active");

  public static final EntityType T_ADDRESS = type("store.address");
  public static final Attribute<Integer> ADDRESS_ID = T_ADDRESS.integerAttribute("id");
  public static final Attribute<Entity> ADDRESS_CUSTOMER_FK = T_ADDRESS.entityAttribute("customer_fk");
  public static final Attribute<Integer> ADDRESS_CUSTOMER_ID = T_ADDRESS.integerAttribute("customer_id");
  public static final Attribute<String> ADDRESS_STREET = T_ADDRESS.stringAttribute("street");
  public static final Attribute<String> ADDRESS_CITY = T_ADDRESS.stringAttribute("city");

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
