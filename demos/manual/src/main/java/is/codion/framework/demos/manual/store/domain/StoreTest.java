/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.common.db.exception.DatabaseException;
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
  protected Entity initializeReferenceEntity(EntityType entityType,
                                             Map<EntityType, Entity> foreignKeyEntities)
          throws DatabaseException {
    //see if the currently running test requires an ADDRESS entity
    if (entityType.equals(Address.TYPE)) {
      return getEntities().builder(Address.TYPE)
              .with(Address.ID, 21L)
              .with(Address.STREET, "One Way")
              .with(Address.CITY, "Sin City")
              .build();
    }

    return super.initializeReferenceEntity(entityType, foreignKeyEntities);
  }

  @Override
  protected Entity initializeTestEntity(EntityType entityType,
                                        Map<EntityType, Entity> foreignKeyEntities) {
    if (entityType.equals(Address.TYPE)) {
      //Initialize an entity representing the table STORE.ADDRESS,
      //which can be used for the testing
      return getEntities().builder(Address.TYPE)
              .with(Address.ID, 42L)
              .with(Address.STREET, "Street")
              .with(Address.CITY, "City")
              .build();
    }
    else if (entityType.equals(Customer.TYPE)) {
      //Initialize an entity representing the table STORE.CUSTOMER,
      //which can be used for the testing
      return getEntities().builder(Customer.TYPE)
              .with(Customer.ID, UUID.randomUUID().toString())
              .with(Customer.FIRST_NAME, "Robert")
              .with(Customer.LAST_NAME, "Ford")
              .with(Customer.IS_ACTIVE, true)
              .build();
    }
    else if (entityType.equals(CustomerAddress.TYPE)) {
      return getEntities().builder(CustomerAddress.TYPE)
              .with(CustomerAddress.CUSTOMER_FK, foreignKeyEntities.get(Customer.TYPE))
              .with(CustomerAddress.ADDRESS_FK, foreignKeyEntities.get(Address.TYPE))
              .build();
    }

    return super.initializeTestEntity(entityType, foreignKeyEntities);
  }

  @Override
  protected void modifyEntity(Entity testEntity, Map<EntityType, Entity> foreignKeyEntities) {
    if (testEntity.getEntityType().equals(Address.TYPE)) {
      testEntity.put(Address.STREET, "New Street");
      testEntity.put(Address.CITY, "New City");
    }
    else if (testEntity.getEntityType().equals(Customer.TYPE)) {
      //It is sufficient to change the value of a single property, but the more, the merrier
      testEntity.put(Customer.FIRST_NAME, "Jesse");
      testEntity.put(Customer.LAST_NAME, "James");
      testEntity.put(Customer.IS_ACTIVE, false);
    }
  }
}
// end::storeTest[]