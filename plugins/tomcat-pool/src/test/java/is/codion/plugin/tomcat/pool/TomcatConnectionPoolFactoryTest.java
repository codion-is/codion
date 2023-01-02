/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.tomcat.pool;

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TomcatConnectionPoolFactoryTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void test() throws Exception {
    TomcatConnectionPoolFactory provider = new TomcatConnectionPoolFactory();
    ConnectionPoolWrapper pool = provider.createConnectionPoolWrapper(new H2DatabaseFactory().createDatabase("jdbc:h2:mem:TomcatConnectionPoolProviderTest",
            Database.DATABASE_INIT_SCRIPTS.get()), UNIT_TEST_USER);
    pool.setCollectSnapshotStatistics(true);
    assertTrue(pool.isCollectSnapshotStatistics());
    pool.connection(UNIT_TEST_USER).close();
    pool.connection(UNIT_TEST_USER).close();
    pool.connection(UNIT_TEST_USER).close();
    pool.close();
  }
}
