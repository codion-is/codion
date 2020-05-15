/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain.minimal;

import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

// tag::storeTest[]
public class StoreTest extends EntityTestUnit {

  public StoreTest() {
    super(Store.class.getName());
  }

  @Test
  void customer() throws Exception {
    test(is.codion.framework.demos.manual.store.domain.Store.T_CUSTOMER);
  }
}
// end::storeTest[]