/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.hikari.pool;

import org.jminor.common.User;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.dbms.h2database.H2Database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HikariConnectionPoolProviderTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  @Test
  public void test() throws Exception {
    final long startTime = System.currentTimeMillis();
    final HikariConnectionPoolProvider provider = new HikariConnectionPoolProvider();
    final ConnectionPool pool = provider.createConnectionPool(UNIT_TEST_USER,
            new H2Database("HikariConnectionPoolProviderTest.test", System.getProperty("jminor.db.initScript")));
    pool.setCollectFineGrainedStatistics(true);
    assertTrue(pool.isCollectFineGrainedStatistics());
    pool.getConnection().close();
    pool.getConnection().close();
    pool.getConnection().close();
    pool.getStatistics(startTime).getFineGrainedStatistics();
    pool.close();
  }
}
