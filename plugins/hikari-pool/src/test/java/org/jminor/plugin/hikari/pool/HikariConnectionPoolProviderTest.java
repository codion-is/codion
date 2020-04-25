/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.hikari.pool;

import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.dbms.h2database.H2Database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HikariConnectionPoolProviderTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  @Test
  public void test() throws Exception {
    final long startTime = System.currentTimeMillis();
    final HikariConnectionPoolProvider provider = new HikariConnectionPoolProvider();
    final ConnectionPool pool = provider.createConnectionPool(H2Database.h2MemoryDatabase("HikariConnectionPoolProviderTest.test",
            System.getProperty("jminor.db.initScript")), UNIT_TEST_USER);
    pool.setCollectSnapshotStatistics(true);
    assertTrue(pool.isCollectSnapshotStatistics());
    pool.getConnection().close();
    pool.getConnection().close();
    pool.getConnection().close();
    pool.getStatistics(startTime).getSnapshot();
    pool.close();
  }
}
