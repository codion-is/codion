/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.tomcat.pool;

import org.jminor.common.User;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.dbms.h2database.H2Database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TomcatConnectionPoolProviderTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  @Test
  public void test() throws Exception {
    final TomcatConnectionPoolProvider provider = new TomcatConnectionPoolProvider();
    final ConnectionPool pool = provider.createConnectionPool(UNIT_TEST_USER,
            new H2Database("TomcatConnectionPoolProviderTest.test", System.getProperty("jminor.db.initScript")));
    pool.setCollectFineGrainedStatistics(true);
    assertTrue(pool.isCollectFineGrainedStatistics());
    pool.returnConnection(pool.getConnection());
    pool.returnConnection(pool.getConnection());
    pool.returnConnection(pool.getConnection());
    pool.close();
  }
}
