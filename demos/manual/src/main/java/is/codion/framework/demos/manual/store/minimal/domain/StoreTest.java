/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.domain;

import is.codion.framework.demos.manual.store.minimal.domain.Store.Address;
import is.codion.framework.demos.manual.store.minimal.domain.Store.Customer;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public class StoreTest extends EntityTestUnit {

  public StoreTest() {
    super(new Store());
  }

  @Test
  void customer() throws Exception {
    test(Customer.TYPE);
  }

  @Test
  void address() throws Exception {
    test(Address.TYPE);
  }
}
