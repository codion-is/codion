/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.domain;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.domain.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public final class StoreTest extends EntityTestUnit {

  public StoreTest() {
    super(Store.class.getName());
  }

  @Test
  void address() throws DatabaseException {
    test(Store.T_ADDRESS);
  }

  @Test
  void customer() throws DatabaseException {
    test(Store.T_CUSTOMER);
  }
}
