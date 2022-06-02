/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.StringFactory;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.property.Properties.*;

public class Store extends DefaultDomain {

  static final DomainType DOMAIN = domainType(Store.class);

  public interface Customer {
    EntityType TYPE = DOMAIN.entityType("store.customer");

    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
    Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
    Attribute<String> EMAIL = TYPE.stringAttribute("email");
    Attribute<Boolean> IS_ACTIVE = TYPE.booleanAttribute("is_active");
  }

  public interface Address {
    EntityType TYPE = DOMAIN.entityType("store.address");

    Attribute<Long> ID = TYPE.longAttribute("id");
    Attribute<Long> CUSTOMER_ID = TYPE.longAttribute("customer_id");
    Attribute<String> STREET = TYPE.stringAttribute("street");
    Attribute<String> CITY = TYPE.stringAttribute("city");

    ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);
  }

  public Store() {
    super(DOMAIN);

    define(Customer.TYPE,
            primaryKeyProperty(Customer.ID),
            columnProperty(Customer.FIRST_NAME, "First name")
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(Customer.LAST_NAME, "Last name")
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(Customer.EMAIL, "Email")
                    .maximumLength(100),
            columnProperty(Customer.IS_ACTIVE, "Is active")
                    .nullable(false)
                    .defaultValue(true))
            .keyGenerator(identity())
            .stringFactory(StringFactory.builder()
                    .value(Customer.LAST_NAME)
                    .text(", ")
                    .value(Customer.FIRST_NAME))
            .caption("Customer");

    define(Address.TYPE,
            primaryKeyProperty(Address.ID),
            columnProperty(Address.CUSTOMER_ID)
                    .nullable(false),
            foreignKeyProperty(Address.CUSTOMER_FK, "Customer"),
            columnProperty(Address.STREET, "Street")
                    .nullable(false)
                    .maximumLength(100),
            columnProperty(Address.CITY, "City")
                    .nullable(false)
                    .maximumLength(50))
            .keyGenerator(identity())
            .stringFactory(StringFactory.builder()
                    .value(Address.STREET)
                    .text(", ")
                    .value(Address.CITY))
            .caption("Address");
  }
}
