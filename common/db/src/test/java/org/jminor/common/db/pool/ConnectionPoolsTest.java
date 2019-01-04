package org.jminor.common.db.pool;

import org.jminor.common.User;
import org.jminor.common.db.DatabaseConnectionsTest;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class ConnectionPoolsTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  @Test
  public void initializeConnectionPools() throws DatabaseException, ClassNotFoundException {
    ConnectionPools.initializeConnectionPools(new DefaultConnectionPoolProvider(), Databases.getInstance(),
            Collections.singletonList(UNIT_TEST_USER));
    assertNotNull(ConnectionPools.getConnectionPool(UNIT_TEST_USER));
    ConnectionPools.closeConnectionPools();
    assertNull(ConnectionPools.getConnectionPool(UNIT_TEST_USER));
  }

  @Test
  public void createDefaultConnectionPool() throws DatabaseException {
    ConnectionPools.createDefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    assertNotNull(ConnectionPools.getConnectionPool(UNIT_TEST_USER));
    ConnectionPools.closeConnectionPools();
    assertNull(ConnectionPools.getConnectionPool(UNIT_TEST_USER));
  }
}
