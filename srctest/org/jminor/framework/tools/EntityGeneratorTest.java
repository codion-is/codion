/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools;

import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.db.dbms.H2Database;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class EntityGeneratorTest {

  @Test
  public void test() throws Exception {
    final H2Database database = (H2Database) DatabaseProvider.createInstance();
    final String domainClass = EntityGenerator.getDomainClass(database,
            "Test", "CHINOOK", "org.test", "scott", "tiger", null);
    System.out.println(domainClass);
    assertNotNull(domainClass);
  }
}
