/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringProvider;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.property.Properties.*;

public class Store extends Domain {

  static final DomainType DOMAIN = domainType(Store.class);

  public interface Customer {
    EntityType<Entity> TYPE = DOMAIN.entityType("store.customer");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> FIRST_NAME = TYPE.stringAttribute("first_name");
    Attribute<String> LAST_NAME = TYPE.stringAttribute("last_name");
    Attribute<String> EMAIL = TYPE.stringAttribute("email");
    Attribute<Boolean> IS_ACTIVE = TYPE.booleanAttribute("is_active");
  }

  public interface Address {
    EntityType<Entity> TYPE = DOMAIN.entityType("store.address");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<Integer> CUSTOMER_ID = TYPE.integerAttribute("customer_id");
    Attribute<Entity> CUSTOMER_FK = TYPE.entityAttribute("customer_fk");
    Attribute<String> STREET = TYPE.stringAttribute("street");
    Attribute<String> CITY = TYPE.stringAttribute("city");
  }

  public Store() {
    super(DOMAIN);

    define(Customer.TYPE,
            primaryKeyProperty(Customer.ID),
            columnProperty(Customer.FIRST_NAME, "First name")
                    .nullable(false).maximumLength(40),
            columnProperty(Customer.LAST_NAME, "Last name")
                    .nullable(false).maximumLength(40),
            columnProperty(Customer.EMAIL, "Email")
                    .maximumLength(100),
            columnProperty(Customer.IS_ACTIVE, "Is active")
                    .defaultValue(true))
            .keyGenerator(automatic("store.customer"))
            .stringProvider(new StringProvider(Customer.LAST_NAME)
                    .addText(", ").addValue(Customer.FIRST_NAME))
            .caption("Customer");

    define(Address.TYPE,
            primaryKeyProperty(Address.ID),
            foreignKeyProperty(Address.CUSTOMER_FK, "Customer", Customer.TYPE,
                    columnProperty(Address.CUSTOMER_ID))
                    .nullable(false),
            columnProperty(Address.STREET, "Street")
                    .nullable(false).maximumLength(100),
            columnProperty(Address.CITY, "City")
                    .nullable(false).maximumLength(50))
            .keyGenerator(automatic("store.address"))
            .stringProvider(new StringProvider(Address.STREET)
                    .addText(", ").addValue(Address.CITY))
            .caption("Address");
  }
}
