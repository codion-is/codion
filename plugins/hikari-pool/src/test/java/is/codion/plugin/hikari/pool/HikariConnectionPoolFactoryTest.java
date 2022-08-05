/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.hikari.pool;

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HikariConnectionPoolFactoryTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() throws Exception {
    long startTime = System.currentTimeMillis();
    HikariConnectionPoolFactory provider = new HikariConnectionPoolFactory();
    ConnectionPoolWrapper pool = provider.createConnectionPoolWrapper(new H2DatabaseFactory().createDatabase("jdbc:h2:mem:HikariConnectionPoolProviderTest",
            Database.DATABASE_INIT_SCRIPTS.get()), UNIT_TEST_USER);
    pool.setCollectSnapshotStatistics(true);
    assertTrue(pool.isCollectSnapshotStatistics());
    pool.getConnection(UNIT_TEST_USER).close();
    pool.getConnection(UNIT_TEST_USER).close();
    pool.getConnection(UNIT_TEST_USER).close();
    pool.getStatistics(startTime).snapshot();
    pool.close();
  }
}
