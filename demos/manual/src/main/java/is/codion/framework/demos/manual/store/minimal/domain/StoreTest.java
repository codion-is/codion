/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public class StoreTest extends EntityTestUnit {

  public StoreTest() {
    super(Store.class.getName());
  }

  @Test
  void customer() throws Exception {
    test(Store.Customer.TYPE);
  }

  @Test
  void address() throws Exception {
    test(Store.Address.TYPE);
  }
}
