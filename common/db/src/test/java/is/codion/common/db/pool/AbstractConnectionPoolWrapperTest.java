/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
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
    Database database = H2DatabaseFactory.createDatabase("jdbc:h2:mem:h2db", "src/test/sql/create_h2_db.sql");

    assertThrows(IllegalStateException.class, () -> ConnectionPoolFactory.instance("is.codion.none.existing.Factory"));

    ConnectionPoolFactory poolFactory = ConnectionPoolFactory.instance();
    assertNotNull(ConnectionPoolFactory.instance("is.codion.plugin.hikari.pool.HikariConnectionPoolFactory"));

    User user = User.parse("scott:tiger");
    long startTime = System.currentTimeMillis();
    ConnectionPoolWrapper poolWrapper = poolFactory.createConnectionPoolWrapper(database, user);
    poolWrapper.user();
    poolWrapper.setCollectSnapshotStatistics(true);
    assertTrue(poolWrapper.isCollectSnapshotStatistics());
    for (int i = 0; i < 100; i++) {
      poolWrapper.connection(user).close();
    }
    //just wait a bit for statistics to be collected
    Thread.sleep(100);
    ConnectionPoolStatistics statistics = poolWrapper.statistics(startTime);
    statistics.available();
    statistics.inUse();
    statistics.created();
    statistics.creationDate();
    statistics.destroyed();
    statistics.username();
    statistics.averageGetTime();
    statistics.requests();
    statistics.requestsPerSecond();
    statistics.failedRequests();
    statistics.failedRequestsPerSecond();
    statistics.maximumCheckOutTime();
    statistics.minimumCheckOutTime();
    statistics.resetTime();
    statistics.size();
    statistics.timestamp();
    List<ConnectionPoolState> snapshot = statistics.snapshot();
    assertFalse(snapshot.isEmpty());
    ConnectionPoolState state = snapshot.get(0);
    state.size();
    state.inUse();
    state.waiting();
    state.timestamp();
    poolWrapper.resetStatistics();
  }
}
