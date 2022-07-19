/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractConnectionPoolWrapperTest {

  @Test
  void test() throws DatabaseException, SQLException, InterruptedException {
    Database database = new H2DatabaseFactory().createDatabase("jdbc:h2:mem:h2db", "src/test/sql/create_h2_db.sql");

    assertThrows(IllegalStateException.class, () -> ConnectionPoolFactory.connectionPoolFactory("is.codion.none.existing.Factory"));

    ConnectionPoolFactory poolFactory = ConnectionPoolFactory.connectionPoolFactory();
    assertNotNull(ConnectionPoolFactory.connectionPoolFactory("is.codion.plugin.hikari.pool.HikariConnectionPoolFactory"));

    User user = User.parse("scott:tiger");
    long startTime = System.currentTimeMillis();
    ConnectionPoolWrapper poolWrapper = poolFactory.createConnectionPoolWrapper(database, user);
    poolWrapper.getUser();
    poolWrapper.setCollectSnapshotStatistics(true);
    assertTrue(poolWrapper.isCollectSnapshotStatistics());
    for (int i = 0; i < 100; i++) {
      poolWrapper.getConnection(user).close();
    }
    //just wait a bit for statistics to be collected
    Thread.sleep(100);
    ConnectionPoolStatistics statistics = poolWrapper.getStatistics(startTime);
    statistics.getAvailable();
    statistics.getInUse();
    statistics.getCreated();
    statistics.getCreationDate();
    statistics.getDestroyed();
    statistics.getUsername();
    statistics.getAverageGetTime();
    statistics.getRequests();
    statistics.getRequestsPerSecond();
    statistics.getFailedRequests();
    statistics.getFailedRequestsPerSecond();
    statistics.getMaximumCheckOutTime();
    statistics.getMinimumCheckOutTime();
    statistics.getResetTime();
    statistics.getSize();
    statistics.getTimestamp();
    List<ConnectionPoolState> snapshot = statistics.getSnapshot();
    assertFalse(snapshot.isEmpty());
    ConnectionPoolState state = snapshot.get(0);
    state.getSize();
    state.getInUse();
    state.getWaiting();
    state.getTimestamp();
    poolWrapper.resetStatistics();
  }
}
