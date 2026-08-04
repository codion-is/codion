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

			connection.close();

			//the same instance, re-establishing itself on the next operation
			connection.transactionOpen();
			assertEquals(2, connection.connections());
		}

		@Test
		@DisplayName("closing the connection does not invalidate it")
		void connection_closed_survives() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.entities();
			connection.close();
			assertFalse(connection.connected());

			//the closed connection re-establishes itself rather than having to be discarded
			connection.transactionOpen();
			assertTrue(connection.connected());
			assertEquals(2, connection.connections());
		}

		@Test
		@DisplayName("an invalid connection triggers a reconnect")
		void connection_invalid_triggersReconnect() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.transactionOpen();

			//simulate a connection failure
			connection.close();
			assertFalse(connection.connected());

			//the next operation re-establishes it, through the same instance
			connection.transactionOpen();
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
		void connected_afterClose_doesNotHeal() {
			TestConnection connection = new TestConnectionBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			connection.close();
			assertFalse(connection.connected());
			assertEquals(1, connection.connections());
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

			connection.transactionOpen();
			int afterFirstOperation = checks.get();
			connection.transactionOpen();
			connection.transactionOpen();

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

			connection.transactionOpen();
			int afterFirstOperation = checks.get();
			Thread.sleep(100);
			connection.transactionOpen();

			assertTrue(checks.get() > afterFirstOperation, "the connection must be rechecked once the interval has elapsed");
		}

		@Test
		@DisplayName("continuous use does not postpone the check indefinitely")
		void connection_continuousUse_stillRechecked() throws InterruptedException {
			//the interval is measured from the last check, not the last use, otherwise a connection
			//dying during continuous use would never be rechecked and every operation would fail
			EntityConnection.VALIDITY_CHECK_INTERVAL.set(50L);

			AtomicInteger checks = new AtomicInteger();
			EntityConnection connection = new CountingConnectionBuilder(checks)
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			//the connection dies, unnoticed, and is then used continuously
			connection.transactionOpen();
			connection.close();

			long deadline = System.currentTimeMillis() + 5_000;
			while (!connection.connected() && System.currentTimeMillis() < deadline) {
				Thread.sleep(10);
				connection.transactionOpen();
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
				threads[i] = new Thread(connection::transactionOpen);
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
			connection.transactionOpen();

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
			Thread caller = new Thread(connection::transactionOpen);
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

		private CountingConnection(CountingConnectionBuilder builder) {
			super(builder);
			this.checks = builder.checks;
		}

		@Override
		protected EntityConnection connect() {
			State connected = State.state(true);

			return ProxyBuilder.of(EntityConnection.class)
							.method("entities", parameters -> ENTITIES)
							//stands in for any operation, which is what drives validation
							.method("transactionOpen", parameters -> false)
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
							.method("transactionOpen", parameters -> false)
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

	private static final class TestConnection extends AbstractEntityConnection {

		private final AtomicInteger connections = new AtomicInteger();

		private TestConnection(AbstractBuilder<?, ?> builder) {
			super(builder);
		}

		/**
		 * @return the number of underlying connections established so far
		 */
		private int connections() {
			return connections.get();
		}

		@Override
		protected EntityConnection connect() {
			State connected = State.state(true);
			connections.incrementAndGet();

			return ProxyBuilder.of(EntityConnection.class)
							.method("entities", parameters -> ENTITIES)
							//stands in for any operation, which is what drives validation
							.method("transactionOpen", parameters -> false)
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
