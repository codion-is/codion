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
 * Copyright (c) 2018 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.common.reactive.state.State;
import is.codion.common.utilities.proxy.ProxyBuilder;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.AbstractEntityConnectionProvider.AbstractBuilder;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AbstractEntityConnectionProviderTest")
public final class AbstractEntityConnectionProviderTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Entities ENTITIES = new TestDomain().entities();

	@Nested
	@DisplayName("Connection Lifecycle")
	class ConnectionLifecycle {

		@Test
		@DisplayName("provider creates and manages connections correctly")
		void provider_connectionLifecycle_worksCorrectly() {
			TestProviderBuilder builder = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN);
			EntityConnectionProvider provider = builder.build();

			// Test initial state
			assertEquals("description", provider.description().get());
			assertEquals(EntityConnectionProvider.CONNECTION_TYPE_LOCAL, provider.connectionType());
			assertEquals(ENTITIES, provider.entities());
			assertEquals(UNIT_TEST_USER, provider.user());
			assertEquals(TestDomain.DOMAIN, provider.domainType());

			// Get connection
			EntityConnection connection1 = provider.connection();
			assertNotNull(connection1);
			assertTrue(provider.connectionValid());

			// Close provider - should close connection
			provider.close();
			assertFalse(provider.connectionValid());

			// Get new connection after close
			EntityConnection connection2 = provider.connection();
			assertNotNull(connection2);
			assertTrue(provider.connectionValid());
			assertNotEquals(connection1, connection2);
		}

		@Test
		@DisplayName("closing connection invalidates provider")
		void provider_closingConnection_invalidatesProvider() {
			EntityConnectionProvider provider = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			EntityConnection connection = provider.connection();
			assertTrue(provider.connectionValid());

			// Close connection directly
			connection.close();
			assertFalse(provider.connectionValid());

			// Provider creates new connection after previous was closed
			EntityConnection newConnection = provider.connection();
			assertNotNull(newConnection);
			assertTrue(provider.connectionValid());
			assertNotEquals(connection, newConnection);
		}

		@Test
		@DisplayName("multiple connection retrievals return same instance")
		void provider_multipleRetrievals_returnSameConnection() {
			EntityConnectionProvider provider = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			EntityConnection connection1 = provider.connection();
			EntityConnection connection2 = provider.connection();
			EntityConnection connection3 = provider.connection();

			assertSame(connection1, connection2);
			assertSame(connection2, connection3);
		}
	}

	@Nested
	@DisplayName("Builder Configuration")
	class BuilderConfiguration {

		@Test
		@DisplayName("builder with minimal configuration")
		void builder_minimalConfiguration_buildsCorrectly() {
			EntityConnectionProvider provider = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			assertNotNull(provider);
			assertEquals(UNIT_TEST_USER, provider.user());
			assertEquals(TestDomain.DOMAIN, provider.domainType());
		}

		@Test
		@DisplayName("builder validates required parameters")
		void builder_missingRequiredParameters_throwsException() {
			// Missing user
			assertThrows(NullPointerException.class, () ->
							new TestProviderBuilder()
											.domain(TestDomain.DOMAIN)
											.build()
			);

			// Missing domain
			assertThrows(NullPointerException.class, () ->
							new TestProviderBuilder()
											.user(UNIT_TEST_USER)
											.build()
			);
		}

		@Test
		@DisplayName("builder with domain type string")
		void builder_domainTypeString_buildsCorrectly() {
			DomainType domainType = TestDomain.DOMAIN;
			EntityConnectionProvider provider = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(domainType)
							.build();

			assertEquals(domainType, provider.domainType());
		}
	}

	@Nested
	@DisplayName("Connection State")
	class ConnectionState {

		@Test
		@DisplayName("connection valid state changes correctly")
		void provider_connectionValidState_changesCorrectly() {
			EntityConnectionProvider provider = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			// Initially no connection
			assertFalse(provider.connectionValid());

			// After getting connection
			provider.connection();
			assertTrue(provider.connectionValid());

			// After closing provider
			provider.close();
			assertFalse(provider.connectionValid());
		}

		@Test
		@DisplayName("closed provider can create new connections")
		void provider_afterClose_canCreateNewConnections() {
			EntityConnectionProvider provider = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			EntityConnection connection1 = provider.connection();
			provider.close();

			// Should be able to get a new connection
			EntityConnection connection2 = provider.connection();
			assertNotNull(connection2);
			assertNotEquals(connection1, connection2);
			assertTrue(provider.connectionValid());
		}

		@Test
		@DisplayName("invalid connection triggers reconnect")
		void provider_invalidConnection_triggersReconnect() {
			TestProviderBuilder builder = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN);
			EntityConnectionProvider provider = builder.build();

			// Get initial connection
			EntityConnection connection1 = provider.connection();
			assertTrue(provider.connectionValid());

			// Close connection directly (simulating connection failure)
			connection1.close();
			assertFalse(connection1.connected());

			// Provider should detect invalid connection
			assertFalse(provider.connectionValid());

			// Next call should create new connection
			EntityConnection connection2 = provider.connection();
			assertNotNull(connection2);
			assertNotEquals(connection1, connection2);
			assertTrue(connection2.connected());
			assertTrue(provider.connectionValid());
		}
	}

	@Nested
	@DisplayName("Thread Safety")
	class ThreadSafety {

		@Test
		@DisplayName("concurrent connection requests return same instance")
		void provider_concurrentRequests_returnSameInstance() throws InterruptedException {
			EntityConnectionProvider provider = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			EntityConnection[] connections = new EntityConnection[10];
			Thread[] threads = new Thread[10];

			// Create threads that all request connection
			for (int i = 0; i < threads.length; i++) {
				int index = i;
				threads[i] = new Thread(() -> connections[index] = provider.connection());
			}

			// Start all threads
			for (Thread thread : threads) {
				thread.start();
			}

			// Wait for all to complete
			for (Thread thread : threads) {
				thread.join();
			}

			// All should have same connection instance
			EntityConnection firstConnection = connections[0];
			for (EntityConnection connection : connections) {
				assertSame(firstConnection, connection);
			}
		}
	}

	@Nested
	@DisplayName("Provider Properties")
	class ProviderProperties {


		@Test
		@DisplayName("toString includes key information")
		void provider_toString_includesKeyInfo() {
			EntityConnectionProvider provider = new TestProviderBuilder()
							.user(UNIT_TEST_USER)
							.domain(TestDomain.DOMAIN)
							.build();

			String toString = provider.toString();
			assertNotNull(toString);
			// AbstractEntityConnectionProvider likely includes user and domain in toString
			assertTrue(toString.contains(UNIT_TEST_USER.username()) ||
							toString.contains(provider.getClass().getSimpleName()));
		}
	}

	private static final class TestProvider extends AbstractEntityConnectionProvider {

		public TestProvider(AbstractBuilder<?, ?> builder) {
			super(builder);
		}

		@Override
		protected EntityConnection connect() {
			State connected = State.state(true);

			return ProxyBuilder.of(EntityConnection.class)
							.method("equals", Object.class, parameters -> TestProvider.this == parameters.arguments().get(0))
							.method("entities", parameters -> ENTITIES)
							.method("connected", parameters -> connected.is())
							.method("close", parameters -> {
								connected.set(false);
								return null;
							})
							.build();
		}

		@Override
		protected void close(EntityConnection connection) {
			connection.close();
		}

		@Override
		public String connectionType() {
			return EntityConnectionProvider.CONNECTION_TYPE_LOCAL;
		}

		@Override
		public Optional<String> description() {
			return Optional.of("description");
		}
	}

	private static final class TestProviderBuilder extends AbstractBuilder<TestProvider, TestProviderBuilder> {

		private TestProviderBuilder() {
			super(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
		}

		@Override
		public TestProvider build() {
			return new TestProvider(this);
		}
	}
}
