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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.user.User;
import is.codion.dbms.h2.H2DatabaseFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class AbstractConnectionPoolWrapperTest {

	private static final String H2_MEMORY_URL = "jdbc:h2:mem:h2db";
	private static final String H2_INIT_SCRIPT = "src/test/sql/create_h2_db.sql";
	private static final String TEST_USER_CREDENTIALS = "scott:tiger";
	private static final String NON_EXISTENT_FACTORY = "is.codion.none.existing.Factory";
	private static final String HIKARI_FACTORY = "is.codion.plugin.hikari.pool.HikariConnectionPoolFactory";

	private static final int CONNECTION_TEST_COUNT = 100;
	private static final int STATISTICS_WAIT_TIME_MS = 100;

	private Database database;
	private User testUser;
	private ConnectionPoolWrapper poolWrapper;

	@BeforeEach
	void setup() {
		database = H2DatabaseFactory.create(H2_MEMORY_URL, H2_INIT_SCRIPT);
		testUser = User.parse(TEST_USER_CREDENTIALS);
	}

	@AfterEach
	void tearDown() {
		if (poolWrapper != null) {
			poolWrapper.close();
		}
	}

	@Nested
	@DisplayName("ConnectionPoolFactory tests")
	class ConnectionPoolFactoryTests {

		@Test
		@DisplayName("Factory instantiation with non-existent class throws exception")
		void factory_nonExistentClass_shouldThrowException() {
			assertThrows(IllegalStateException.class,
							() -> ConnectionPoolFactory.instance(NON_EXISTENT_FACTORY));
		}

		@Test
		@DisplayName("Factory instantiation with valid class succeeds")
		void factory_validClass_shouldInstantiate() {
			ConnectionPoolFactory poolFactory = ConnectionPoolFactory.instance();
			assertNotNull(poolFactory);

			// Test specific implementation if available
			assertNotNull(ConnectionPoolFactory.instance(HIKARI_FACTORY));
		}
	}

	@Test
	@DisplayName("ConnectionPoolState copy is independent of the source")
	void connectionPoolStateCopyIsIndependent() {
		DefaultConnectionPoolState state = new DefaultConnectionPoolState();
		state.set(1000L, 5, 3, 1);
		DefaultConnectionPoolState copy = new DefaultConnectionPoolState(state);
		assertEquals(1000L, copy.timestamp());
		assertEquals(5, copy.size());
		assertEquals(3, copy.inUse());
		assertEquals(1, copy.waiting());
		//mutating the live source (as the collector thread does) must not change the copied snapshot
		state.set(2000L, 9, 9, 9);
		assertEquals(1000L, copy.timestamp());
		assertEquals(5, copy.size());
	}

	@Nested
	@DisplayName("ConnectionPoolWrapper tests")
	class ConnectionPoolWrapperTests {

		@BeforeEach
		void createPool() {
			ConnectionPoolFactory poolFactory = ConnectionPoolFactory.instance();
			poolWrapper = poolFactory.createConnectionPool(database, testUser);
		}

		@Test
		@DisplayName("Pool wrapper basic operations work correctly")
		void poolWrapper_basicOperations_shouldWork() {
			// Verify user
			assertEquals(testUser, poolWrapper.user());

			// Test snapshot statistics configuration
			poolWrapper.collectSnapshotStatistics(true);
			assertTrue(poolWrapper.collectSnapshotStatistics());

			poolWrapper.collectSnapshotStatistics(false);
			assertFalse(poolWrapper.collectSnapshotStatistics());
		}

		@Test
		@DisplayName("Pool wrapper handles multiple connections correctly")
		void poolWrapper_multipleConnections_shouldHandleCorrectly() throws SQLException {
			// Enable statistics collection
			poolWrapper.collectSnapshotStatistics(true);

			// Get and close multiple connections
			for (int i = 0; i < CONNECTION_TEST_COUNT; i++) {
				try (Connection conn = poolWrapper.connection(testUser)) {
					assertNotNull(conn);
					assertTrue(conn.isValid(1));
				}
			}
		}

		@Test
		@DisplayName("Pool statistics are collected correctly")
		void poolWrapper_statistics_shouldBeCollected() throws SQLException, InterruptedException {
			long startTime = System.currentTimeMillis();
			poolWrapper.collectSnapshotStatistics(true);

			// Perform some operations
			for (int i = 0; i < 10; i++) {
				poolWrapper.connection(testUser).close();
			}

			// Wait for statistics to be collected
			Thread.sleep(STATISTICS_WAIT_TIME_MS);

			// Verify statistics
			ConnectionPoolStatistics statistics = poolWrapper.statistics(startTime);
			assertNotNull(statistics);

			// Verify basic statistics values
			assertTrue(statistics.requests() >= 10);
			assertTrue(statistics.timestamp() >= startTime);
		}

		@Test
		@DisplayName("Pool statistics accessors work correctly")
		void poolStatistics_accessors_shouldWork() throws SQLException, InterruptedException {
			long startTime = System.currentTimeMillis();
			poolWrapper.collectSnapshotStatistics(true);

			// Generate some activity
			poolWrapper.connection(testUser).close();
			Thread.sleep(STATISTICS_WAIT_TIME_MS);

			ConnectionPoolStatistics statistics = poolWrapper.statistics(startTime);

			// Test all accessor methods
			assertDoesNotThrow(() -> {
				statistics.available();
				statistics.inUse();
				statistics.created();
				statistics.destroyed();
				statistics.averageTime();
				statistics.requestsPerSecond();
				statistics.failedRequests();
				statistics.failedRequestsPerSecond();
				statistics.maximumTime();
				statistics.minimumTime();
				statistics.resetTime();
				statistics.size();
			});
		}

		@Test
		@DisplayName("Pool snapshot statistics are collected when enabled")
		void poolWrapper_snapshotStatistics_shouldBeCollected() throws SQLException, InterruptedException {
			poolWrapper.collectSnapshotStatistics(true);
			long startTime = System.currentTimeMillis();

			// Generate activity
			for (int i = 0; i < 5; i++) {
				poolWrapper.connection(testUser).close();
				Thread.sleep(20);
			}

			ConnectionPoolStatistics statistics = poolWrapper.statistics(startTime);
			List<ConnectionPoolState> snapshot = statistics.snapshot();

			assertNotNull(snapshot);
			assertFalse(snapshot.isEmpty());

			// Verify snapshot state
			ConnectionPoolState state = snapshot.get(0);
			assertNotNull(state);
			assertTrue(state.size() >= 0);
			assertTrue(state.inUse() >= 0);
			assertTrue(state.waiting() >= 0);
			assertTrue(state.timestamp() > 0);
		}

		@Test
		@DisplayName("statistics(0) excludes pre-allocated empty snapshot states")
		void statisticsSinceEpochExcludesEmptyStates() throws SQLException, InterruptedException {
			poolWrapper.collectSnapshotStatistics(true);
			for (int i = 0; i < 3; i++) {
				poolWrapper.connection(testUser).close();
				Thread.sleep(20);
			}
			ConnectionPoolStatistics statistics = poolWrapper.statistics(0);
			//a since-epoch snapshot must not leak the pre-allocated empty (timestamp 0) ring states
			for (ConnectionPoolState state : statistics.snapshot()) {
				assertTrue(state.timestamp() > 0);
			}
		}

		@Test
		@DisplayName("Statistics reset works correctly")
		void poolWrapper_resetStatistics_shouldWork() throws SQLException {
			poolWrapper.collectSnapshotStatistics(true);

			// Generate some statistics
			for (int i = 0; i < 5; i++) {
				poolWrapper.connection(testUser).close();
			}

			// Reset statistics
			assertDoesNotThrow(() -> poolWrapper.resetStatistics());

			// Statistics should be reset (new start time)
			ConnectionPoolStatistics newStats = poolWrapper.statistics(System.currentTimeMillis());
			assertNotNull(newStats);
		}
	}
}