/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.ColumnProperty;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static is.codion.framework.domain.entity.Entities.type;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.*;

public final class Store extends Domain {

  public static final EntityType T_ADDRESS = type("store.address");
  public static final Attribute<Integer> ADDRESS_ID = T_ADDRESS.integerAttribute("id");
  public static final Attribute<String> ADDRESS_STREET = T_ADDRESS.stringAttribute("street");
  public static final Attribute<String> ADDRESS_CITY = T_ADDRESS.stringAttribute("city");
  public static final Attribute<Boolean> ADDRESS_VALID = T_ADDRESS.booleanAttribute("valid");

  public static final EntityType T_CUSTOMER = type("store.customer");
  public static final Attribute<String> CUSTOMER_ID = T_CUSTOMER.stringAttribute("id");
  public static final Attribute<String> CUSTOMER_FIRST_NAME = T_CUSTOMER.stringAttribute("first_name");
  public static final Attribute<String> CUSTOMER_LAST_NAME = T_CUSTOMER.stringAttribute("last_name");
  public static final Attribute<String> CUSTOMER_EMAIL = T_CUSTOMER.stringAttribute("email");
  public static final Attribute<Boolean> CUSTOMER_IS_ACTIVE = T_CUSTOMER.booleanAttribute("is_active");

  public static final EntityType T_CUSTOMER_ADDRESS = type("store.customer_address");
  public static final Attribute<Integer> CUSTOMER_ADDRESS_ID = T_CUSTOMER_ADDRESS.integerAttribute("id");
  public static final Attribute<String> CUSTOMER_ADDRESS_CUSTOMER_ID = T_CUSTOMER_ADDRESS.stringAttribute("customer_id");
  public static final Attribute<Entity> CUSTOMER_ADDRESS_CUSTOMER_FK = T_CUSTOMER_ADDRESS.entityAttribute("customer_fk");
  public static final Attribute<Integer> CUSTOMER_ADDRESS_ADDRESS_ID = T_CUSTOMER_ADDRESS.integerAttribute("address_id");
  public static final Attribute<Entity> CUSTOMER_ADDRESS_ADDRESS_FK = T_CUSTOMER_ADDRESS.entityAttribute("address_fk");

  public Store() {
    customer();
    address();
    customerAddress();
  }

  private void customer() {
    // tag::customer[]
    define(T_CUSTOMER,
            primaryKeyProperty(CUSTOMER_ID),
            columnProperty(CUSTOMER_FIRST_NAME, "First name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_LAST_NAME, "Last name")
                    .nullable(false).maximumLength(40),
            columnProperty(CUSTOMER_EMAIL, "Email"),
            columnProperty(CUSTOMER_IS_ACTIVE, "Is active")
                    .columnHasDefaultValue(true).defaultValue(true))
            .keyGenerator(new UUIDKeyGenerator())
            .stringProvider(new CustomerToString())
            .caption("Customer");
    // end::customer[]
  }

  private void address() {
    // tag::address[]
    define(T_ADDRESS,
            primaryKeyProperty(ADDRESS_ID),
            columnProperty(ADDRESS_STREET, "Street")
                    .nullable(false).maximumLength(120),
            columnProperty(ADDRESS_CITY, "City")
                    .nullable(false).maximumLength(50),
            columnProperty(ADDRESS_VALID, "Valid")
                    .columnHasDefaultValue(true).nullable(false))
            .stringProvider(new StringProvider(ADDRESS_STREET)
                    .addText(", ").addValue(ADDRESS_CITY))
            .keyGenerator(automatic("store.address"))
            .smallDataset(true)
            .caption("Address");
    // end::address[]
  }

  private void customerAddress() {
    // tag::customerAddress[]
    define(T_CUSTOMER_ADDRESS,
            primaryKeyProperty(CUSTOMER_ADDRESS_ID),
            foreignKeyProperty(CUSTOMER_ADDRESS_CUSTOMER_FK, "Customer", T_CUSTOMER,
                    columnProperty(CUSTOMER_ADDRESS_CUSTOMER_ID))
                    .nullable(false),
            foreignKeyProperty(CUSTOMER_ADDRESS_ADDRESS_FK, "Address", T_ADDRESS,
                    columnProperty(CUSTOMER_ADDRESS_ADDRESS_ID))
                    .nullable(false))
            .keyGenerator(automatic("store.customer_address"))
            .caption("Customer address");
    // end::customerAddress[]
  }

  // tag::toString[]
  private static final class CustomerToString implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public String apply(Entity customer) {
      StringBuilder builder =
              new StringBuilder(customer.get(CUSTOMER_LAST_NAME))
                      .append(", ")
                      .append(customer.get(CUSTOMER_FIRST_NAME));
      if (customer.isNotNull(CUSTOMER_EMAIL)) {
        builder.append(" <")
                .append(customer.get(CUSTOMER_EMAIL))
                .append(">");
      }

      return builder.toString();
    }
  }
  // end::toString[]

  // tag::keyGenerator[]
  private static final class UUIDKeyGenerator implements KeyGenerator {

    @Override
    public void beforeInsert(Entity entity, List<ColumnProperty<?>> primaryKeyProperties,
                             DatabaseConnection connection) throws SQLException {
      entity.put(CUSTOMER_ID, UUID.randomUUID().toString());
    }
  }
  // end::keyGenerator[]
}
