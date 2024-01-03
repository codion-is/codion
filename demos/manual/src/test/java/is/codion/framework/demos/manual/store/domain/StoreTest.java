/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public final class StoreTest extends EntityTestUnit {

  public StoreTest() {
    super(new Store());
  }

  @Test
  void customer() throws DatabaseException {
    test(Customer.TYPE);
  }

  @Test
  void address() throws DatabaseException {
    test(Address.TYPE);
  }

  @Test
  void customerAddress() throws Exception {
    test(CustomerAddress.TYPE);
  }
}
