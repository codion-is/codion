/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.plugin.jasperreports.model.JasperReports;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.property.Properties.*;

public final class Store extends DefaultDomain {

  static final DomainType STORE = domainType(Store.class);

  public interface Address {
    EntityType TYPE = STORE.entityType("store.address");

    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<String> STREET = TYPE.stringAttribute("street");
    Attribute<String> CITY = TYPE.stringAttribute("city");
    Attribute<Boolean> VALID = TYPE.booleanAttribute("valid");
  }

  public interface Customer {
    EntityType TYPE = STORE.entityType("store.customer");

    Attribute<String> ID = TYPE.stringAttribute("id");
    Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
    Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
    Attribute<String> EMAIL = TYPE.stringAttribute("email");
    Attribute<Boolean> IS_ACTIVE = TYPE.booleanAttribute("is_active");
  }

  public interface CustomerAddress {
    EntityType TYPE = STORE.entityType("store.customer_address");

    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<String> CUSTOMER_ID = TYPE.stringAttribute("customer_id");
    Attribute<Long> ADDRESS_ID = TYPE.longAttribute("address_id");

    ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
    ForeignKey ADDRESS_FK = TYPE.foreignKey("address_fk", ADDRESS_ID, Address.ID);
  }

  public static final ReportType<JasperReport, JasperPrint, Map<String, Object>> CUSTOMER_REPORT =
          JasperReports.reportType("customer_report");

  public Store() {
    super(STORE);
    customer();
    address();
    customerAddress();
  }

  private void customer() {
    // tag::customer[]
    add(definition(
            primaryKeyProperty(Customer.ID),
            columnProperty(Customer.FIRST_NAME, "First name")
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(Customer.LAST_NAME, "Last name")
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(Customer.EMAIL, "Email"),
            columnProperty(Customer.IS_ACTIVE, "Is active")
                    .columnHasDefaultValue(true)
                    .defaultValue(true))
            .keyGenerator(new UUIDKeyGenerator())
            // tag::customerStringFactory[]
            .stringFactory(new CustomerToString())
            // end::customerStringFactory[]
            .caption("Customer"));
    // end::customer[]
  }

  private void address() {
    // tag::address[]
    add(definition(
            primaryKeyProperty(Address.ID),
            columnProperty(Address.STREET, "Street")
                    .nullable(false)
                    .maximumLength(120),
            columnProperty(Address.CITY, "City")
                    .nullable(false)
                    .maximumLength(50),
            columnProperty(Address.VALID, "Valid")
                    .columnHasDefaultValue(true)
                    .nullable(false))
            .stringFactory(StringFactory.builder()
                    .value(Address.STREET)
                    .text(", ")
                    .value(Address.CITY)
                    .build())
            .keyGenerator(identity())
            .smallDataset(true)
            .caption("Address"));
    // end::address[]
  }

  private void customerAddress() {
    // tag::customerAddress[]
    add(definition(
            primaryKeyProperty(CustomerAddress.ID),
            columnProperty(CustomerAddress.CUSTOMER_ID)
                    .nullable(false),
            foreignKeyProperty(CustomerAddress.CUSTOMER_FK, "Customer"),
            columnProperty(CustomerAddress.ADDRESS_ID)
                    .nullable(false),
            foreignKeyProperty(CustomerAddress.ADDRESS_FK, "Address"))
            .keyGenerator(identity())
            .caption("Customer address"));
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
