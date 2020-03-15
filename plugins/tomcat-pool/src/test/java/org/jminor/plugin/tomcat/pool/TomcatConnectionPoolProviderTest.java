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
    final ConnectionPool pool = provider.createConnectionPool(UNIT_TEST_USER,
            new H2Database("TomcatConnectionPoolProviderTest.test", System.getProperty("jminor.db.initScript")));
    pool.setCollectFineGrainedStatistics(true);
    assertTrue(pool.isCollectFineGrainedStatistics());
    pool.getConnection().close();
    pool.getConnection().close();
    pool.getConnection().close();
    pool.close();
  }
}
