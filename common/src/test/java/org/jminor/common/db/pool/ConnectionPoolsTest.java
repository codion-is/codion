package org.jminor.common.db.pool;

import org.jminor.common.User;
import org.jminor.common.db.DatabaseConnectionsTest;
import org.jminor.common.db.DatabasesTest;
import org.jminor.common.db.exception.DatabaseException;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public final class ConnectionPoolsTest {

  @Test
  public void initializeConnectionPools() throws DatabaseException, ClassNotFoundException {
    ConnectionPools.initializeConnectionPools(null, DatabasesTest.createTestDatabaseInstance(),
            Collections.singletonList(User.UNIT_TEST_USER), 1000);
    assertNotNull(ConnectionPools.getConnectionPool(User.UNIT_TEST_USER));
    ConnectionPools.closeConnectionPools();
    assertNull(ConnectionPools.getConnectionPool(User.UNIT_TEST_USER));
  }

  @Test
  public void createDefaultConnectionPool() throws DatabaseException {
    final ConnectionPool pool = ConnectionPools.createDefaultConnectionPool(DatabaseConnectionsTest.createTestDatabaseConnectionProvider());
    assertNotNull(ConnectionPools.getConnectionPool(User.UNIT_TEST_USER));
    ConnectionPools.closeConnectionPools();
    assertNull(ConnectionPools.getConnectionPool(User.UNIT_TEST_USER));
  }
}
