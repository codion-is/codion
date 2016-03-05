/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.tomcat.pool;

import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.model.User;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TomcatConnectionPoolProviderTest {

  @Test
  public void test() throws Exception {
    final long startTime = System.currentTimeMillis();
    final TomcatConnectionPoolProvider provider = new TomcatConnectionPoolProvider();
    final ConnectionPool pool = provider.createConnectionPool(User.UNIT_TEST_USER,
            new H2Database("TomcatConnectionPoolProviderTest.test", System.getProperty("jminor.db.initScript")));
    pool.setCollectFineGrainedStatistics(true);
    assertTrue(pool.isCollectFineGrainedStatistics());
    pool.returnConnection(pool.getConnection());
    pool.returnConnection(pool.getConnection());
    pool.returnConnection(pool.getConnection());
    pool.close();
  }
}
