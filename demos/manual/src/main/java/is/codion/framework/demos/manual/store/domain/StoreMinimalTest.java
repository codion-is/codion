/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.domain;

import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

// tag::storeTest[]
public class StoreMinimalTest extends EntityTestUnit {

  public StoreMinimalTest() {
    super(StoreMinimal.class.getName());
  }

  @Test
  public void customer() throws Exception {
    test(Store.T_CUSTOMER);
  }
}
// end::storeTest[]