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
 * Copyright (c) 2018 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.common.reactive.state.State;
import is.codion.common.utilities.proxy.ProxyBuilder;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.AbstractEntityConnection.AbstractBuilder;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AbstractEntityConnectionTest")
public final class AbstractEntityConnectionTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Entities ENTITIES = new TestDomain().entities();

	@BeforeEach
	void setUp() {
		//check on every call, these tests assert reconnection the moment a connection goes bad
		EntityConnection.VALIDITY_CHECK_INTERVAL.set(0L);
	}

	@AfterEach
	void tearDown() {
		EntityConnection.VALIDITY_CHECK_INTERVAL.remove();
	}

	@Nested
	@DisplayName("Connection Lifecycle")
	class ConnectionLifecycle {

		@Test
		@DisplayName("the connection establishes and re-establishes the underlying one")
		void connection_lifecycle_worksCorrectly() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			assertEquals("description", connection.description().orElseThrow());
			assertEquals(ENTITIES, connection.entities());
			assertEquals(UNIT_TEST_USER, connection.user());
			assertEquals(TestDomain.DOMAIN, connection.domainType());
			assertEquals(1, connection.connections());

			connection.killUnderlying();

			//the same instance, re-establishing itself on the next operation
			connection.cacheQueries();
			assertEquals(2, connection.connections());
		}

		@Test
		@DisplayName("closing the connection is terminal")
		void close_isTerminal() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.entities();
			connection.close();
			assertFalse(connection.connected());

			//it is a connection going bad that is healed, an explicit close is honored
			assertThrows(IllegalStateException.class, connection::cacheQueries);
			assertEquals(1, connection.connections());
			//the domain metadata remains available
			assertEquals(ENTITIES, connection.entities());
			//closing an already closed connection has no effect
			assertDoesNotThrow(() -> connection.close());
		}

		@Test
		@DisplayName("an invalid connection triggers a reconnect")
		void connection_invalid_triggersReconnect() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.cacheQueries();

			//the underlying connection dies from an external cause
			connection.killUnderlying();
			assertFalse(connection.connected());

			//the next operation re-establishes it, through the same instance
			connection.cacheQueries();
			assertTrue(connection.connected());
			assertEquals(2, connection.connections());
		}

		@Test
		@DisplayName("the connection is established when built")
		void connection_built_isConnected() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			//built connected, so that a database or server which can not be
			//reached is reported here rather than on first use
			assertTrue(connection.connected());
			assertEquals(1, connection.connections());
		}

		@Test
		@DisplayName("connected() reports without re-establishing")
		void connected_afterUnderlyingDeath_doesNotHeal() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.killUnderlying();
			assertFalse(connection.connected());
			assertEquals(1, connection.connections());
		}

		@Test
		@DisplayName("a build failing after the connect closes the established connection")
		void build_entitiesFails_underlyingClosed() {
			FailingEntitiesConnectionBuilder builder = new FailingEntitiesConnectionBuilder();
			builder.failEntities = true;

			//the underlying connection is established, then fetching the entities fails - the build
			//fails, and the connection it established must be closed, not left dangling on the server
			assertThrows(RuntimeException.class, () -> builder
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build());
			assertEquals(1, builder.connections.get());
			assertEquals(1, builder.closed.get(), "the established connection must be closed when the build fails");
		}

		@Test
		@DisplayName("a re-establishment failing after the connect closes the established connection")
		void reconnect_entitiesFails_underlyingClosedAndRetried() {
			FailingEntitiesConnectionBuilder builder = new FailingEntitiesConnectionBuilder();
			FailingEntitiesConnection connection = builder
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.killUnderlying();
			connection.failEntities = true;

			//the re-establishment creates a live underlying connection, whose entities() then fails -
			//that connection must be closed rather than left dangling
			assertThrows(RuntimeException.class, connection::cacheQueries);
			assertEquals(2, builder.connections.get());
			assertEquals(2, builder.closed.get(), "the dead original and the half established replacement");

			//and the failure is not sticky, the next operation tries again
			connection.failEntities = false;
			connection.cacheQueries();
			assertEquals(3, builder.connections.get());
		}
	}

	@Nested
	@DisplayName("Transactions")
	class Transactions {

		@Test
		@DisplayName("the transaction state is answered from this instance, without a connection")
		void transactionOpen_answersWithoutTouchingTheConnection() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.killUnderlying();
			//a connection which has gone bad can not be asked, so the answer must come from this
			//instance, and answering a question must not establish a connection
			assertFalse(connection.transactionOpen());
			assertEquals(1, connection.connections());
		}

		@Test
		@DisplayName("operations within a transaction go to the transaction connection, unvalidated")
		void transaction_operations_skipValidation() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.startTransaction();
			assertTrue(connection.transactionOpen());
			int checks = connection.checks();
			connection.cacheQueries();
			connection.cacheQueries();
			assertEquals(checks, connection.checks(), "operations within a transaction must not validate the connection");
			connection.commitTransaction();
			assertFalse(connection.transactionOpen());
			assertEquals(1, connection.connections());
		}

		@Test
		@DisplayName("a connection dying within a transaction fails loudly, it is not replaced")
		void transaction_connectionDies_operationsFailWithoutReconnecting() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.startTransaction();
			connection.killUnderlying();

			//were the connection replaced here, this operation would succeed on a fresh connection,
			//outside the transaction the caller believes to be open, and be committed by itself
			assertThrows(RuntimeException.class, connection::cacheQueries);
			assertTrue(connection.transactionOpen());
			assertEquals(1, connection.connections(), "a connection must never be replaced during a transaction");
		}

		@Test
		@DisplayName("a failed rollback discards the connection and the next operation heals")
		void rollbackTransaction_connectionDead_discardsAndHeals() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.startTransaction();
			connection.killUnderlying();

			//the rollback call fails, but the disconnect rolls the transaction back, so the caller's
			//intent is fulfilled and the exception which triggered the rollback remains the one reported
			assertDoesNotThrow(connection::rollbackTransaction);
			assertFalse(connection.transactionOpen());

			connection.cacheQueries();
			assertEquals(2, connection.connections(), "the operation following a failed rollback must find a fresh connection");
		}

		@Test
		@DisplayName("a commit failing on a dead connection throws and discards")
		void commitTransaction_connectionDead_throwsAndDiscards() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.startTransaction();
			connection.killUnderlying();

			assertThrows(RuntimeException.class, connection::commitTransaction);
			assertFalse(connection.transactionOpen(), "the transaction died with the connection");

			connection.cacheQueries();
			assertEquals(2, connection.connections());
		}

		@Test
		@DisplayName("a commit failing on a live connection leaves the transaction open")
		void commitTransaction_commitFails_transactionStaysOpen() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.startTransaction();
			connection.failCommit = true;

			//a deferred constraint failing on commit for example - the connection is fine and
			//the transaction still open, the caller decides whether to retry or roll back
			assertThrows(RuntimeException.class, connection::commitTransaction);
			assertTrue(connection.transactionOpen());
			assertEquals(1, connection.connections());

			connection.rollbackTransaction();
			assertFalse(connection.transactionOpen());
		}

		@Test
		@DisplayName("closing the connection rolls back the open transaction")
		void close_transactionOpen_clearsTransactionState() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.startTransaction();
			connection.close();

			assertFalse(connection.transactionOpen());
			assertThrows(IllegalStateException.class, connection::cacheQueries);
		}

		@Test
		@DisplayName("a transaction can not be started while one is open")
		void startTransaction_transactionOpen_throws() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.startTransaction();
			assertThrows(IllegalStateException.class, connection::startTransaction);
			assertTrue(connection.transactionOpen(), "the open transaction must survive the failed start");
		}

		@Test
		@DisplayName("ending a transaction which is not open throws")
		void endTransaction_noTransactionOpen_throws() {
			TransactionConnection connection = new TransactionConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			assertThrows(IllegalStateException.class, connection::commitTransaction);
			assertThrows(IllegalStateException.class, connection::rollbackTransaction);
		}
	}

	@Nested
	@DisplayName("Builder Configuration")
	class BuilderConfiguration {

		@Test
		@DisplayName("builder with minimal configuration")
		void builder_minimalConfiguration_buildsCorrectly() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			assertNotNull(connection);
			assertEquals(UNIT_TEST_USER, connection.user());
			assertEquals(TestDomain.DOMAIN, connection.domainType());
		}

		@Test
		@DisplayName("builder validates required parameters")
		void builder_missingRequiredParameters_throwsException() {
			// Missing user
			assertThrows(NullPointerException.class, () ->
							new TestConnectionBuilder()
											.domain(TestDomain.DOMAIN)
											.build()
			);

			// Missing domain
			assertThrows(NullPointerException.class, () ->
							new TestConnectionBuilder()
											.user(UNIT_TEST_USER)
											.build()
			);
		}

		@Test
		@DisplayName("builder with domain type")
		void builder_domainType_buildsCorrectly() {
			DomainType domainType = TestDomain.DOMAIN;
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(domainType)
							.build();

			assertEquals(domainType, connection.domainType());
		}
	}

	@Nested
	@DisplayName("Validity Check Interval")
	class ValidityCheckInterval {

		@Test
		@DisplayName("a connection validated within the interval is not rechecked")
		void connection_withinInterval_notRechecked() {
			EntityConnection.VALIDITY_CHECK_INTERVAL.set(10_000L);

			AtomicInteger checks = new AtomicInteger();
			EntityConnection connection = new CountingConnectionBuilder(checks)
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.cacheQueries();
			int afterFirstOperation = checks.get();
			connection.cacheQueries();
			connection.cacheQueries();

			assertEquals(afterFirstOperation, checks.get(), "the connection must not be rechecked within the interval");
		}

		@Test
		@DisplayName("a connection is rechecked once the interval has elapsed")
		void connection_intervalElapsed_rechecked() throws InterruptedException {
			EntityConnection.VALIDITY_CHECK_INTERVAL.set(50L);

			AtomicInteger checks = new AtomicInteger();
			EntityConnection connection = new CountingConnectionBuilder(checks)
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.cacheQueries();
			int afterFirstOperation = checks.get();
			Thread.sleep(100);
			connection.cacheQueries();

			assertTrue(checks.get() > afterFirstOperation, "the connection must be rechecked once the interval has elapsed");
		}

		@Test
		@DisplayName("continuous use does not postpone the check indefinitely")
		void connection_continuousUse_stillRechecked() throws InterruptedException {
			//the interval is measured from the last check, not the last use, otherwise a connection
			//dying during continuous use would never be rechecked and every operation would fail
			EntityConnection.VALIDITY_CHECK_INTERVAL.set(50L);

			AtomicInteger checks = new AtomicInteger();
			CountingConnection connection = new CountingConnectionBuilder(checks)
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			//the connection dies, unnoticed, and is then used continuously
			connection.cacheQueries();
			connection.killUnderlying();

			long deadline = System.currentTimeMillis() + 5_000;
			while (!connection.connected() && System.currentTimeMillis() < deadline) {
				Thread.sleep(10);
				connection.cacheQueries();
			}

			assertTrue(connection.connected(), "continuous use must not postpone the check past the interval");
		}
	}

	@Nested
	@DisplayName("Thread Safety")
	class ThreadSafety {

		@Test
		@DisplayName("concurrent first use establishes a single underlying connection")
		void connection_concurrentFirstUse_connectsOnce() throws InterruptedException {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			Thread[] threads = new Thread[10];
			for (int i = 0; i < threads.length; i++) {
				threads[i] = new Thread(connection::cacheQueries);
			}
			for (Thread thread : threads) {
				thread.start();
			}
			for (Thread thread : threads) {
				thread.join();
			}

			assertEquals(1, connection.connections());
		}

		@Test
		@DisplayName("a slow operation does not block entities()")
		void connection_operationInProgress_doesNotBlockEntities() throws InterruptedException {
			//connections serialize every operation on a single lock, which connected() shares,
			//so validating a connection blocks for the duration of any operation in progress.
			//That check must not be performed while holding this connection's lock, doing so
			//stalls every caller, the UI thread included
			Object connectionLock = new Object();
			CountDownLatch operationRunning = new CountDownLatch(1);
			CountDownLatch releaseOperation = new CountDownLatch(1);

			EntityConnection connection = new LockingConnectionBuilder(connectionLock)
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();
			connection.cacheQueries();

			//an operation in progress, holding the connection lock
			Thread operation = new Thread(() -> {
				synchronized (connectionLock) {
					operationRunning.countDown();
					await(releaseOperation);
				}
			});
			operation.start();
			assertTrue(operationRunning.await(10, SECONDS));

			//a second caller, blocked while validating the connection ahead of its operation
			Thread caller = new Thread(connection::cacheQueries);
			caller.start();
			assertTrue(blocked(caller));

			CountDownLatch done = new CountDownLatch(1);
			Thread unrelated = new Thread(() -> {
				connection.entities();
				done.countDown();
			});
			unrelated.start();

			assertTrue(done.await(10, SECONDS), "entities() must not queue behind an operation in progress");

			releaseOperation.countDown();
			operation.join();
			caller.join();
			unrelated.join();
		}

		private boolean blocked(Thread thread) throws InterruptedException {
			for (int i = 0; i < 500; i++) {
				if (thread.getState() == Thread.State.BLOCKED) {
					return true;
				}
				Thread.sleep(10);
			}

			return false;
		}

		private void await(CountDownLatch latch) {
			try {
				latch.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Nested
	@DisplayName("Connection Properties")
	class ConnectionProperties {

		@Test
		@DisplayName("toString includes key information")
		void connection_toString_includesKeyInfo() {
			EntityConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			String toString = connection.toString();
			assertNotNull(toString);
			assertTrue(toString.contains(UNIT_TEST_USER.username()) ||
							toString.contains(connection.getClass().getSimpleName()));
		}
	}

	private static final class CountingConnection extends AbstractEntityConnection {

		private final AtomicInteger checks;

		private volatile State alive = State.state(true);

		private CountingConnection(CountingConnectionBuilder builder) {
			super(builder);
			this.checks = builder.checks;
		}

		/**
		 * Simulates the current underlying connection dying from an external cause,
		 * without this instance being told.
		 */
		private void killUnderlying() {
			alive.set(false);
		}

		@Override
		protected EntityConnection connect() {
			State connected = State.state(true);
			alive = connected;

			return ProxyBuilder.of(EntityConnection.class)
							.method("entities", parameters -> ENTITIES)
							//stands in for any operation, which is what drives validation
							.method("cacheQueries", parameters -> (QueryCache) () -> {})
							.method("connected", parameters -> {
								checks.incrementAndGet();

								return connected.is();
							})
							.method("close", parameters -> {
								connected.set(false);

								return null;
							})
							.build();
		}
	}

	private static final class CountingConnectionBuilder extends AbstractBuilder<CountingConnection, CountingConnectionBuilder> {

		private final AtomicInteger checks;

		private CountingConnectionBuilder(AtomicInteger checks) {
			super(EntityConnection.CONNECTION_TYPE_LOCAL);
			this.checks = checks;
		}

		@Override
		protected CountingConnection createConnection() {
			return new CountingConnection(this);
		}
	}

	private static final class LockingConnection extends AbstractEntityConnection {

		private final Object connectionLock;

		private LockingConnection(LockingConnectionBuilder builder) {
			super(builder);
			this.connectionLock = builder.connectionLock;
		}

		@Override
		protected EntityConnection connect() {
			return ProxyBuilder.of(EntityConnection.class)
							.method("entities", parameters -> ENTITIES)
							//stands in for any operation, which is what drives validation
							.method("cacheQueries", parameters -> (QueryCache) () -> {})
							.method("connected", parameters -> {
								synchronized (connectionLock) {
									return true;
								}
							})
							.method("close", parameters -> null)
							.build();
		}

		@Override
		protected void close(EntityConnection connection) {}
	}

	private static final class LockingConnectionBuilder extends AbstractBuilder<LockingConnection, LockingConnectionBuilder> {

		private final Object connectionLock;

		private LockingConnectionBuilder(Object connectionLock) {
			super(EntityConnection.CONNECTION_TYPE_LOCAL);
			this.connectionLock = connectionLock;
		}

		@Override
		protected LockingConnection createConnection() {
			return new LockingConnection(this);
		}
	}

	private static final class TransactionConnection extends AbstractEntityConnection {

		private final AtomicInteger connections = new AtomicInteger();
		private final AtomicInteger checks = new AtomicInteger();

		private volatile State alive = State.state(true);
		private volatile boolean failCommit = false;

		private TransactionConnection(TransactionConnectionBuilder builder) {
			super(builder);
		}

		/**
		 * @return the number of underlying connections established so far
		 */
		private int connections() {
			return connections.get();
		}

		/**
		 * @return the number of validity checks performed so far
		 */
		private int checks() {
			return checks.get();
		}

		/**
		 * Simulates the current underlying connection dying from an external cause,
		 * without this instance being told.
		 */
		private void killUnderlying() {
			alive.set(false);
		}

		@Override
		protected EntityConnection connect() {
			connections.incrementAndGet();
			State connected = State.state(true);
			alive = connected;
			AtomicBoolean transactionOpen = new AtomicBoolean();

			return ProxyBuilder.of(EntityConnection.class)
							.method("entities", parameters -> ENTITIES)
							.method("connected", parameters -> {
								checks.incrementAndGet();

								return connected.is();
							})
							//stands in for any operation
							.method("cacheQueries", parameters -> {
								verifyConnected(connected);

								return (QueryCache) () -> {};
							})
							.method("transactionOpen", parameters -> transactionOpen.get())
							.method("startTransaction", parameters -> {
								verifyConnected(connected);
								if (!transactionOpen.compareAndSet(false, true)) {
									throw new IllegalStateException("Transaction already open");
								}

								return null;
							})
							//like the real connections, these clear the transaction flag only after
							//the underlying call succeeds, which on a dead connection it never does
							.method("commitTransaction", parameters -> {
								verifyConnected(connected);
								if (failCommit) {
									throw new RuntimeException("Commit failed");
								}
								transactionOpen.set(false);

								return null;
							})
							.method("rollbackTransaction", parameters -> {
								verifyConnected(connected);
								transactionOpen.set(false);

								return null;
							})
							.method("close", parameters -> {
								connected.set(false);

								return null;
							})
							.build();
		}

		private static void verifyConnected(State connected) {
			if (!connected.is()) {
				throw new RuntimeException("Connection has gone bad");
			}
		}
	}

	private static final class TransactionConnectionBuilder extends AbstractBuilder<TransactionConnection, TransactionConnectionBuilder> {

		private TransactionConnectionBuilder() {
			super(EntityConnection.CONNECTION_TYPE_LOCAL);
		}

		@Override
		protected TransactionConnection createConnection() {
			return new TransactionConnection(this);
		}
	}

	private static final class FailingEntitiesConnection extends AbstractEntityConnection {

		private final AtomicInteger connections;
		private final AtomicInteger closed;

		private volatile State alive = State.state(true);
		private volatile boolean failEntities;

		private FailingEntitiesConnection(FailingEntitiesConnectionBuilder builder) {
			super(builder);
			this.connections = builder.connections;
			this.closed = builder.closed;
			this.failEntities = builder.failEntities;
		}

		/**
		 * Simulates the current underlying connection dying from an external cause,
		 * without this instance being told.
		 */
		private void killUnderlying() {
			alive.set(false);
		}

		@Override
		protected EntityConnection connect() {
			connections.incrementAndGet();
			State connected = State.state(true);
			alive = connected;

			return ProxyBuilder.of(EntityConnection.class)
							.method("entities", parameters -> {
								if (failEntities) {
									throw new RuntimeException("Fetching the entities failed");
								}

								return ENTITIES;
							})
							.method("connected", parameters -> connected.is())
							//stands in for any operation
							.method("cacheQueries", parameters -> {
								if (!connected.is()) {
									throw new RuntimeException("Connection has gone bad");
								}

								return (QueryCache) () -> {};
							})
							.method("close", parameters -> {
								closed.incrementAndGet();
								connected.set(false);

								return null;
							})
							.build();
		}
	}

	private static final class FailingEntitiesConnectionBuilder extends AbstractBuilder<FailingEntitiesConnection, FailingEntitiesConnectionBuilder> {

		//held here, shared with the connection, since the connection instance
		//never escapes a failing build for the test to inspect
		private final AtomicInteger connections = new AtomicInteger();
		private final AtomicInteger closed = new AtomicInteger();

		private boolean failEntities = false;

		private FailingEntitiesConnectionBuilder() {
			super(EntityConnection.CONNECTION_TYPE_LOCAL);
		}

		@Override
		protected FailingEntitiesConnection createConnection() {
			return new FailingEntitiesConnection(this);
		}
	}

	private static final class TestConnection extends AbstractEntityConnection {

		private final AtomicInteger connections = new AtomicInteger();

		private volatile State alive = State.state(true);

		private TestConnection(AbstractBuilder<?, ?> builder) {
			super(builder);
		}

		/**
		 * @return the number of underlying connections established so far
		 */
		private int connections() {
			return connections.get();
		}

		/**
		 * Simulates the current underlying connection dying from an external cause,
		 * without this instance being told.
		 */
		private void killUnderlying() {
			alive.set(false);
		}

		@Override
		protected EntityConnection connect() {
			State connected = State.state(true);
			alive = connected;
			connections.incrementAndGet();

			return ProxyBuilder.of(EntityConnection.class)
							.method("entities", parameters -> ENTITIES)
							//stands in for any operation, which is what drives validation
							.method("cacheQueries", parameters -> (QueryCache) () -> {})
							.method("connected", parameters -> connected.is())
							.method("close", parameters -> {
								connected.set(false);
								return null;
							})
							.build();
		}

		@Override
		public Optional<String> description() {
			return Optional.of("description");
		}
	}

	private static final class TestConnectionBuilder extends AbstractBuilder<TestConnection, TestConnectionBuilder> {

		private TestConnectionBuilder() {
			super(EntityConnection.CONNECTION_TYPE_LOCAL);
		}

		@Override
		protected TestConnection createConnection() {
			return new TestConnection(this);
		}
	}
}
