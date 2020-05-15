/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;

import java.sql.Types;
// tag::staticImports[]
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.columnProperty;
import static is.codion.framework.domain.property.Properties.primaryKeyProperty;

public class StoreMinimal extends Domain {
// end::staticImports[]

  // tag::customerConstants[]
  public static final String T_CUSTOMER = "store.customer";
  public static final String CUSTOMER_ID = "id";
  public static final String CUSTOMER_FIRST_NAME = "first_name";
  public static final String CUSTOMER_LAST_NAME = "last_name";
  public static final String CUSTOMER_EMAIL = "email";
  public static final String CUSTOMER_IS_ACTIVE = "is_active";
  // end::customerConstants[]

  // tag::customer[]
  public StoreMinimal() {
    define(T_CUSTOMER,
            primaryKeyProperty(CUSTOMER_ID, Types.BIGINT),
            columnProperty(CUSTOMER_FIRST_NAME, Types.VARCHAR, "First name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_LAST_NAME, Types.VARCHAR, "Last name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email"),
            columnProperty(CUSTOMER_IS_ACTIVE, Types.BOOLEAN, "Is active")
                    .defaultValue(true))
            .keyGenerator(automatic("store.customer"))
            .stringProvider(new StringProvider(CUSTOMER_LAST_NAME)
                    .addText(", ").addValue(CUSTOMER_FIRST_NAME))
            .caption("Customer");
  }
}
// end::customer[]
