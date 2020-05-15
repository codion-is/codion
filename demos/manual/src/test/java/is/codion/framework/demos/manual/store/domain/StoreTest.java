/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.domain;

import dev.codion.common.db.exception.DatabaseException;
import dev.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public final class StoreTest extends EntityTestUnit {

  public StoreTest() {
    super(Store.class.getName());
  }

  @Test
  void customer() throws DatabaseException {
    test(Store.T_CUSTOMER);
  }

  @Test
  void address() throws DatabaseException {
    test(Store.T_ADDRESS);
  }

  @Test
  public void customerAddress() throws Exception {
    test(Store.T_CUSTOMER_ADDRESS);
  }
}
