/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

// tag::storeTest[]
public class StoreTest extends EntityTestUnit {

  public StoreTest() {
    super(Store.class.getName());
  }

  @Test
  public void customer() throws Exception {
    test(Customer.TYPE);
  }

  @Test
  public void address() throws Exception {
    test(Address.TYPE);
  }

  @Test
  public void customerAddress() throws Exception {
    test(CustomerAddress.TYPE);
  }

  @Override
  protected Entity initializeReferenceEntity(EntityType<? extends Entity> entityType,
                                             Map<EntityType<? extends Entity>, Entity> foreignKeyEntities) {
    //see if the currently running test requires an ADDRESS entity
    if (entityType.equals(Address.TYPE)) {
      Entity address = getEntities().entity(Address.TYPE);
      address.put(Address.ID, 21);
      address.put(Address.STREET, "One Way");
      address.put(Address.CITY, "Sin City");

      return address;
    }

    return super.initializeReferenceEntity(entityType, foreignKeyEntities);
  }

  @Override
  protected Entity initializeTestEntity(EntityType<? extends Entity> entityType,
                                        Map<EntityType<? extends Entity>, Entity> foreignKeyEntities) {
    if (entityType.equals(Address.TYPE)) {
      //Initialize a entity representing the table STORE.ADDRESS,
      //which can be used for the testing
      Entity address = getEntities().entity(Address.TYPE);
      address.put(Address.ID, 42);
      address.put(Address.STREET, "Street");
      address.put(Address.CITY, "City");

      return address;
    }
    else if (entityType.equals(Customer.TYPE)) {
      //Initialize a entity representing the table STORE.CUSTOMER,
      //which can be used for the testing
      Entity customer = getEntities().entity(Customer.TYPE);
      customer.put(Customer.ID, UUID.randomUUID().toString());
      customer.put(Customer.FIRST_NAME, "Robert");
      customer.put(Customer.LAST_NAME, "Ford");
      customer.put(Customer.IS_ACTIVE, true);

      return customer;
    }
    else if (entityType.equals(CustomerAddress.TYPE)) {
      Entity customerAddress = getEntities().entity(CustomerAddress.TYPE);
      customerAddress.put(CustomerAddress.CUSTOMER_FK, foreignKeyEntities.get(Customer.TYPE));
      customerAddress.put(CustomerAddress.ADDRESS_FK, foreignKeyEntities.get(Address.TYPE));

      return customerAddress;
    }

    return super.initializeTestEntity(entityType, foreignKeyEntities);
  }

  @Override
  protected void modifyEntity(Entity testEntity,
                              Map<EntityType<? extends Entity>, Entity> foreignKeyEntities) {
    if (testEntity.is(Address.TYPE)) {
      testEntity.put(Address.STREET, "New Street");
      testEntity.put(Address.CITY, "New City");
    }
    else if (testEntity.is(Customer.TYPE)) {
      //It is sufficient to change the value of a single property, but the more the merrier
      testEntity.put(Customer.FIRST_NAME, "Jesse");
      testEntity.put(Customer.LAST_NAME, "James");
      testEntity.put(Customer.IS_ACTIVE, false);
    }
  }
}
// end::storeTest[]