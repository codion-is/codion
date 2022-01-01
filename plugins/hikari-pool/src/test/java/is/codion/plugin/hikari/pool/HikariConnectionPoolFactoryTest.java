/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() throws Exception {
    final long startTime = System.currentTimeMillis();
    final HikariConnectionPoolFactory provider = new HikariConnectionPoolFactory();
    final ConnectionPoolWrapper pool = provider.createConnectionPoolWrapper(new H2DatabaseFactory().createDatabase("jdbc:h2:mem:HikariConnectionPoolProviderTest",
            Database.DATABASE_INIT_SCRIPTS.get()), UNIT_TEST_USER);
    pool.setCollectSnapshotStatistics(true);
    assertTrue(pool.isCollectSnapshotStatistics());
    pool.getConnection(UNIT_TEST_USER).close();
    pool.getConnection(UNIT_TEST_USER).close();
    pool.getConnection(UNIT_TEST_USER).close();
    pool.getStatistics(startTime).getSnapshot();
    pool.close();
  }
}
