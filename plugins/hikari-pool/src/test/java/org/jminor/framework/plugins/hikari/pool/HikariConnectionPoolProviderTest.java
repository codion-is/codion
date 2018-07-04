/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.hikari.pool;

import org.jminor.common.User;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.pool.ConnectionPool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HikariConnectionPoolProviderTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  @Test
  public void test() throws Exception {
    final long startTime = System.currentTimeMillis();
    final HikariConnectionPoolProvider provider = new HikariConnectionPoolProvider();
    final ConnectionPool pool = provider.createConnectionPool(UNIT_TEST_USER,
            new H2Database("HikariConnectionPoolProviderTest.test", System.getProperty("jminor.db.initScript")));
    pool.setCollectFineGrainedStatistics(true);
    assertTrue(pool.isCollectFineGrainedStatistics());
    pool.returnConnection(pool.getConnection());
    pool.returnConnection(pool.getConnection());
    pool.returnConnection(pool.getConnection());
    assertFalse(pool.getStatistics(startTime).getFineGrainedStatistics().isEmpty());
    pool.close();
  }
}
