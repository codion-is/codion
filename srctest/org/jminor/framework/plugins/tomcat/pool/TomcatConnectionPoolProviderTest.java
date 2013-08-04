/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.tomcat.pool;

import org.jminor.common.db.Databases;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.model.User;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TomcatConnectionPoolProviderTest {

  @Test
  public void test() throws Exception {
    final long startTime = System.currentTimeMillis();
    final TomcatConnectionPoolProvider provider = new TomcatConnectionPoolProvider();
    final ConnectionPool pool = provider.createConnectionPool(User.UNIT_TEST_USER, Databases.createInstance());
    pool.setCollectFineGrainedStatistics(true);
    assertTrue(pool.isCollectFineGrainedStatistics());
    pool.returnConnection(pool.getConnection());
    pool.returnConnection(pool.getConnection());
    pool.returnConnection(pool.getConnection());
    assertFalse(pool.getStatistics(startTime).getFineGrainedStatistics().isEmpty());
    pool.close();
  }
}
