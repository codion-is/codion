/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.tomcat.pool;

import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.dbms.h2database.H2Database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TomcatConnectionPoolProviderTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  @Test
  public void test() throws Exception {
    final TomcatConnectionPoolProvider provider = new TomcatConnectionPoolProvider();
    final ConnectionPool pool = provider.createConnectionPool(H2Database.h2MemoryDatabase("TomcatConnectionPoolProviderTest.test",
            System.getProperty("jminor.db.initScript")), UNIT_TEST_USER);
    pool.setCollectSnapshotStatistics(true);
    assertTrue(pool.isCollectSnapshotStatistics());
    pool.getConnection().close();
    pool.getConnection().close();
    pool.getConnection().close();
    pool.close();
  }
}
