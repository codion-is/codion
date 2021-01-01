/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.tomcat.pool;

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPool;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.dbms.h2database.H2DatabaseFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TomcatConnectionPoolFactoryTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  public void test() throws Exception {
    final TomcatConnectionPoolFactory provider = new TomcatConnectionPoolFactory();
    final ConnectionPool pool = provider.createConnectionPool(new H2DatabaseFactory().createDatabase("jdbc:h2:mem:TomcatConnectionPoolProviderTest",
            Database.DATABASE_INIT_SCRIPTS.get()), UNIT_TEST_USER);
    pool.setCollectSnapshotStatistics(true);
    assertTrue(pool.isCollectSnapshotStatistics());
    pool.getConnection(UNIT_TEST_USER).close();
    pool.getConnection(UNIT_TEST_USER).close();
    pool.getConnection(UNIT_TEST_USER).close();
    pool.close();
  }
}
