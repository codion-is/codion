/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.plugin.jasperreports.model.JasperReportWrapper;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static is.codion.framework.domain.entity.EntityType.entityType;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.plugin.jasperreports.model.JasperReports.fileReport;

public final class Store extends Domain {

  public interface Address {
    EntityType TYPE = entityType("store.address");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> STREET = TYPE.stringAttribute("street");
    Attribute<String> CITY = TYPE.stringAttribute("city");
    Attribute<Boolean> VALID = TYPE.booleanAttribute("valid");
  }

  public interface Customer {
    EntityType TYPE = entityType("store.customer");
    Attribute<String> ID = TYPE.stringAttribute("id");
    Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
    Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
    Attribute<String> EMAIL = TYPE.stringAttribute("email");
    Attribute<Boolean> IS_ACTIVE = TYPE.booleanAttribute("is_active");
  }

  public interface CustomerAddress {
    EntityType TYPE = entityType("store.customer_address");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> CUSTOMER_ID = TYPE.stringAttribute("customer_id");
    Attribute<Entity> CUSTOMER_FK = TYPE.entityAttribute("customer_fk");
    Attribute<Integer> ADDRESS_ID = TYPE.integerAttribute("address_id");
    Attribute<Entity> ADDRESS_FK = TYPE.entityAttribute("address_fk");
  }

  public static final JasperReportWrapper CUSTOMER_REPORT =
            fileReport("reports/customer.jasper");

  public Store() {
    customer();
    address();
    customerAddress();
  }

  private void customer() {
    // tag::customer[]
    define(Customer.TYPE,
            primaryKeyProperty(Customer.ID),
            columnProperty(Customer.FIRST_NAME, "First name")
                    .nullable(false).maximumLength(40),
            columnProperty(Customer.LAST_NAME, "Last name")
                    .nullable(false).maximumLength(40),
            columnProperty(Customer.EMAIL, "Email"),
            columnProperty(Customer.IS_ACTIVE, "Is active")
                    .columnHasDefaultValue(true).defaultValue(true))
            .keyGenerator(new UUIDKeyGenerator())
            .stringProvider(new CustomerToString())
            .caption("Customer");
    // end::customer[]
  }

  private void address() {
    // tag::address[]
    define(Address.TYPE,
            primaryKeyProperty(Address.ID),
            columnProperty(Address.STREET, "Street")
                    .nullable(false).maximumLength(120),
            columnProperty(Address.CITY, "City")
                    .nullable(false).maximumLength(50),
            columnProperty(Address.VALID, "Valid")
                    .columnHasDefaultValue(true).nullable(false))
            .stringProvider(new StringProvider(Address.STREET)
                    .addText(", ").addValue(Address.CITY))
            .keyGenerator(automatic("store.address"))
            .smallDataset(true)
            .caption("Address");
    // end::address[]
  }

  private void customerAddress() {
    // tag::customerAddress[]
    define(CustomerAddress.TYPE,
            primaryKeyProperty(CustomerAddress.ID),
            foreignKeyProperty(CustomerAddress.CUSTOMER_FK, "Customer", Customer.TYPE,
                    columnProperty(CustomerAddress.CUSTOMER_ID))
                    .nullable(false),
            foreignKeyProperty(CustomerAddress.ADDRESS_FK, "Address", Address.TYPE,
                    columnProperty(CustomerAddress.ADDRESS_ID))
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
              new StringBuilder(customer.get(Customer.LAST_NAME))
                      .append(", ")
                      .append(customer.get(Customer.FIRST_NAME));
      if (customer.isNotNull(Customer.EMAIL)) {
        builder.append(" <")
                .append(customer.get(Customer.EMAIL))
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
      entity.put(Customer.ID, UUID.randomUUID().toString());
    }
  }
  // end::keyGenerator[]
}
