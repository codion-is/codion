/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.framework.db.EntityConnectionProviders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 14:30:18
 */
public class EntityConnectionProvidersTest {

  @Test
  public void test() {
    assertNotNull(EntityConnectionProviders.connectionProvider().setDomainClassName(TestDomain.class.getName()).setClientTypeId("test"));
  }
}
