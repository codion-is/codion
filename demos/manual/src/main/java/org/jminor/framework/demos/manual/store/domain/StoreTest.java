/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.domain;

import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.Map;

// tag::storeTest[]
public class StoreTest extends EntityTestUnit {

  public StoreTest() {
    super(Store.class.getName());
  }

  @Test
  public void customer() throws Exception {
    test(Store.T_CUSTOMER);
  }

  @Test
  public void address() throws Exception {
    test(Store.T_ADDRESS);
  }

  @Test
  public void customerAddress() throws Exception {
    test(Store.T_CUSTOMER_ADDRESS);
  }

  @Override
  protected Entity initializeReferenceEntity(String entityId,
                                             Map<String, Entity> foreignKeyEntities) {
    //see if the currently running test requires an ADDRESS entity
    if (entityId.equals(Store.T_ADDRESS)) {
      Entity address = getDomain().entity(Store.T_ADDRESS);
      address.put(Store.ADDRESS_ID, 21);
      address.put(Store.ADDRESS_STREET, "One Way");
      address.put(Store.ADDRESS_CITY, "Sin City");

      return address;
    }

    return super.initializeReferenceEntity(entityId, foreignKeyEntities);
  }

  @Override
  protected Entity initializeTestEntity(String entityId,
                                        Map<String, Entity> foreignKeyEntities) {
    if (entityId.equals(Store.T_ADDRESS)) {
      //Initialize a entity representing the table STORE.ADDRESS,
      //which can be used for the testing
      Entity address = getDomain().entity(Store.T_ADDRESS);
      address.put(Store.ADDRESS_ID, 42);
      address.put(Store.ADDRESS_STREET, "Street");
      address.put(Store.ADDRESS_CITY, "City");

      return address;
    }
    else if (entityId.equals(Store.T_CUSTOMER)) {
      //Initialize a entity representing the table STORE.CUSTOMER,
      //which can be used for the testing
      Entity customer = getDomain().entity(Store.T_CUSTOMER);
      customer.put(Store.CUSTOMER_ID, 42);
      customer.put(Store.CUSTOMER_FIRST_NAME, "Robert");
      customer.put(Store.CUSTOMER_LAST_NAME, "Ford");
      customer.put(Store.CUSTOMER_IS_ACTIVE, true);

      return customer;
    }
    else if (entityId.equals(Store.T_CUSTOMER_ADDRESS)) {
      Entity customerAddress = getDomain().entity(Store.T_CUSTOMER_ADDRESS);
      customerAddress.put(Store.CUSTOMER_ADDRESS_CUSTOMER_FK, foreignKeyEntities.get(Store.T_CUSTOMER));
      customerAddress.put(Store.CUSTOMER_ADDRESS_ADDRESS_FK, foreignKeyEntities.get(Store.T_ADDRESS));

      return customerAddress;
    }

    return super.initializeTestEntity(entityId, foreignKeyEntities);
  }

  @Override
  protected void modifyEntity(Entity testEntity,
                              Map<String, Entity> foreignKeyEntities) {
    if (testEntity.is(Store.T_ADDRESS)) {
      testEntity.put(Store.ADDRESS_STREET, "New Street");
      testEntity.put(Store.ADDRESS_CITY, "New City");
    }
    else if (testEntity.is(Store.T_CUSTOMER)) {
      //It is sufficient to change the value of a single property, but the more the merrier
      testEntity.put(Store.CUSTOMER_FIRST_NAME, "Jesse");
      testEntity.put(Store.CUSTOMER_LAST_NAME, "James");
      testEntity.put(Store.CUSTOMER_IS_ACTIVE, false);
    }
  }
}
// end::storeTest[]