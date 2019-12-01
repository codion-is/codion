/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.domain;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.framework.domain.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public final class StoreTest extends EntityTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public StoreTest() {
    super(Store.class.getName());
  }

  @Test
  void address() throws DatabaseException {
    testEntity(Store.T_ADDRESS);
  }

  @Test
  void customer() throws DatabaseException {
    testEntity(Store.T_CUSTOMER);
  }

  @Override
  protected User getTestUser() throws CancelException {
    return UNIT_TEST_USER;
  }
}
