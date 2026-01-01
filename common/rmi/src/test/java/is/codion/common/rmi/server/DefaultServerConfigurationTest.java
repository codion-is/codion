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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for DefaultServerConfiguration.
 * Tests builder pattern, property validation, and SSL configuration.
 */
public class DefaultServerConfigurationTest {

	private static final int TEST_PORT = 12345;
	private static final int TEST_REGISTRY_PORT = 1234;
	private static final int TEST_ADMIN_PORT = 5678;
	private static final String TEST_SERVER_NAME = "TestServer";
	private static final String TEST_AUXILIARY_CLASS = "com.example.AuxiliaryServer";
	private static final String TEST_FILTER_CLASS = "com.example.FilterFactory";
	private static final int TEST_MAINTENANCE_INTERVAL = 60000;
	private static final int TEST_CONNECTION_LIMIT = 100;

	@Nested
	@DisplayName("Builder pattern tests")
	class BuilderPatternTest {

		@Test
		@DisplayName("Basic builder with port creates configuration")
		void builder_withPort_createsConfiguration() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			assertEquals(TEST_PORT, config.port());
			assertEquals(Registry.REGISTRY_PORT, config.registryPort());
			assertNotNull(config.serverName());
			assertTrue(config.sslEnabled());
		}

		@Test
		@DisplayName("Builder with port and registry port creates configuration")
		void builder_withPortAndRegistryPort_createsConfiguration() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT, TEST_REGISTRY_PORT).build();

			assertEquals(TEST_PORT, config.port());
			assertEquals(TEST_REGISTRY_PORT, config.registryPort());
		}

		@Test
		@DisplayName("Builder fluent interface works correctly")
		void builder_fluentInterface_worksCorrectly() {
			Collection<String> auxiliaryClasses = Arrays.asList(TEST_AUXILIARY_CLASS);

			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.adminPort(TEST_ADMIN_PORT)
							.serverName(TEST_SERVER_NAME)
							.auxiliaryServerFactory(auxiliaryClasses)
							.sslEnabled(false)
							.objectInputFilterFactory(TEST_FILTER_CLASS)
							.connectionMaintenanceInterval(TEST_MAINTENANCE_INTERVAL)
							.connectionLimit(TEST_CONNECTION_LIMIT)
							.build();

			assertEquals(TEST_PORT, config.port());
			assertEquals(TEST_ADMIN_PORT, config.adminPort());
			assertEquals(TEST_SERVER_NAME, config.serverName());
			assertTrue(config.auxiliaryServerFactory().containsAll(auxiliaryClasses));
			assertEquals(auxiliaryClasses.size(), config.auxiliaryServerFactory().size());
			assertFalse(config.sslEnabled());
			assertEquals(Optional.of(TEST_FILTER_CLASS), config.objectInputFilterFactory());
			assertEquals(TEST_MAINTENANCE_INTERVAL, config.connectionMaintenanceInterval());
			assertEquals(TEST_CONNECTION_LIMIT, config.connectionLimit());
		}
	}

	@Nested
	@DisplayName("Server name configuration")
	class ServerNameConfigurationTest {

		@Test
		@DisplayName("String server name is stored correctly")
		void serverName_withString_isStoredCorrectly() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.serverName(TEST_SERVER_NAME)
							.build();

			assertEquals(TEST_SERVER_NAME, config.serverName());
		}

		@Test
		@DisplayName("Supplier server name is stored correctly")
		void serverName_withSupplier_isStoredCorrectly() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.serverName(() -> TEST_SERVER_NAME)
							.build();

			assertEquals(TEST_SERVER_NAME, config.serverName());
		}

		@Test
		@DisplayName("Null string server name throws exception")
		void serverName_withNullString_throwsException() {
			assertThrows(IllegalArgumentException.class, () ->
							ServerConfiguration.builder(TEST_PORT).serverName((String) null));
		}

		@Test
		@DisplayName("Empty string server name throws exception")
		void serverName_withEmptyString_throwsException() {
			assertThrows(IllegalArgumentException.class, () ->
							ServerConfiguration.builder(TEST_PORT).serverName(""));
		}

		@Test
		@DisplayName("Null supplier throws NPE")
		void serverName_withNullSupplier_throwsNPE() {
			assertThrows(NullPointerException.class, () ->
							ServerConfiguration.builder(TEST_PORT).serverName((java.util.function.Supplier<String>) null));
		}

		@Test
		@DisplayName("Supplier returning null throws exception on access")
		void serverName_supplierReturningNull_throwsExceptionOnAccess() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.serverName(() -> null)
							.build();

			assertThrows(IllegalArgumentException.class, config::serverName);
		}

		@Test
		@DisplayName("Supplier returning empty string throws exception on access")
		void serverName_supplierReturningEmpty_throwsExceptionOnAccess() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.serverName(() -> "")
							.build();

			assertThrows(IllegalArgumentException.class, config::serverName);
		}
	}

	@Nested
	@DisplayName("SSL configuration")
	class SSLConfigurationTest {

		@Test
		@DisplayName("SSL enabled by default")
		void ssl_enabledByDefault() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			assertTrue(config.sslEnabled());
			assertTrue(config.rmiClientSocketFactory().isPresent());
			assertTrue(config.rmiServerSocketFactory().isPresent());
			assertInstanceOf(SslRMIClientSocketFactory.class, config.rmiClientSocketFactory().get());
			assertInstanceOf(SslRMIServerSocketFactory.class, config.rmiServerSocketFactory().get());
		}

		@Test
		@DisplayName("SSL can be disabled")
		void ssl_canBeDisabled() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.sslEnabled(false)
							.build();

			assertFalse(config.sslEnabled());
			assertFalse(config.rmiClientSocketFactory().isPresent());
			assertFalse(config.rmiServerSocketFactory().isPresent());
		}

		@Test
		@DisplayName("SSL re-enabled after being disabled")
		void ssl_reEnabledAfterDisabling() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.sslEnabled(false)
							.sslEnabled(true)
							.build();

			assertTrue(config.sslEnabled());
			assertTrue(config.rmiClientSocketFactory().isPresent());
			assertTrue(config.rmiServerSocketFactory().isPresent());
		}
	}

	@Nested
	@DisplayName("Auxiliary server configuration")
	class AuxiliaryServerConfigurationTest {

		@Test
		@DisplayName("Empty auxiliary server list by default")
		void auxiliaryServers_emptyByDefault() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			assertTrue(config.auxiliaryServerFactory().isEmpty());
		}

		@Test
		@DisplayName("Auxiliary server class names are stored")
		void auxiliaryServers_classNamesStored() {
			Collection<String> classNames = Arrays.asList("com.example.Server1", "com.example.Server2");

			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.auxiliaryServerFactory(classNames)
							.build();

			assertTrue(config.auxiliaryServerFactory().containsAll(classNames));
			assertEquals(classNames.size(), config.auxiliaryServerFactory().size());
		}

		@Test
		@DisplayName("Auxiliary server collection is immutable")
		void auxiliaryServers_collectionIsImmutable() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.auxiliaryServerFactory(Arrays.asList(TEST_AUXILIARY_CLASS))
							.build();

			assertThrows(UnsupportedOperationException.class, () ->
							config.auxiliaryServerFactory().add("new.class"));
		}

		@Test
		@DisplayName("Null auxiliary server collection throws NPE")
		void auxiliaryServers_nullCollection_throwsNPE() {
			assertThrows(NullPointerException.class, () ->
							ServerConfiguration.builder(TEST_PORT).auxiliaryServerFactory(null));
		}
	}

	@Nested
	@DisplayName("Port configuration")
	class PortConfigurationTest {

		@Test
		@DisplayName("Server port is stored correctly")
		void port_serverPortStored() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			assertEquals(TEST_PORT, config.port());
		}

		@Test
		@DisplayName("Registry port defaults to standard port")
		void port_registryPortDefaultsToStandard() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			assertEquals(Registry.REGISTRY_PORT, config.registryPort());
		}

		@Test
		@DisplayName("Custom registry port is stored")
		void port_customRegistryPortStored() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT, TEST_REGISTRY_PORT).build();

			assertEquals(TEST_REGISTRY_PORT, config.registryPort());
		}

		@Test
		@DisplayName("Admin port defaults to 0")
		void port_adminPortDefaultsToZero() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			assertEquals(0, config.adminPort());
		}

		@Test
		@DisplayName("Custom admin port is stored")
		void port_customAdminPortStored() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.adminPort(TEST_ADMIN_PORT)
							.build();

			assertEquals(TEST_ADMIN_PORT, config.adminPort());
		}
	}

	@Nested
	@DisplayName("Filter and maintenance configuration")
	class FilterAndMaintenanceConfigurationTest {

		@Test
		@DisplayName("Object input filter factory class name is optional")
		void filter_objectInputFilterOptional() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			// May be present due to system property, but test that it's at least accessible
			assertNotNull(config.objectInputFilterFactory());
		}

		@Test
		@DisplayName("Custom object input filter factory class name is stored")
		void filter_customObjectInputFilterStored() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.objectInputFilterFactory(TEST_FILTER_CLASS)
							.build();

			assertEquals(Optional.of(TEST_FILTER_CLASS), config.objectInputFilterFactory());
		}

		@Test
		@DisplayName("Null object input filter factory class name clears value")
		void filter_nullObjectInputFilterClears() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.objectInputFilterFactory(null)
							.build();

			assertEquals(Optional.empty(), config.objectInputFilterFactory());
		}

		@Test
		@DisplayName("Connection maintenance interval has default")
		void maintenance_intervalHasDefault() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			assertEquals(ServerConfiguration.DEFAULT_CONNECTION_MAINTENANCE_INTERVAL,
							config.connectionMaintenanceInterval());
		}

		@Test
		@DisplayName("Custom connection maintenance interval is stored")
		void maintenance_customIntervalStored() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.connectionMaintenanceInterval(TEST_MAINTENANCE_INTERVAL)
							.build();

			assertEquals(TEST_MAINTENANCE_INTERVAL, config.connectionMaintenanceInterval());
		}

		@Test
		@DisplayName("Connection limit defaults to -1")
		void limit_connectionLimitDefaultsToNegativeOne() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT).build();

			assertEquals(-1, config.connectionLimit());
		}

		@Test
		@DisplayName("Custom connection limit is stored")
		void limit_customConnectionLimitStored() {
			ServerConfiguration config = ServerConfiguration.builder(TEST_PORT)
							.connectionLimit(TEST_CONNECTION_LIMIT)
							.build();

			assertEquals(TEST_CONNECTION_LIMIT, config.connectionLimit());
		}
	}

	@Nested
	@DisplayName("System properties builder")
	class SystemPropertiesBuilderTest {

		@Test
		@DisplayName("Builder from system properties works")
		void systemProperties_builderWorks() {
			// This test verifies the method exists and creates a builder
			// The actual system property values are environment-dependent
			ServerConfiguration.Builder<?> builder = ServerConfiguration.builderFromSystemProperties();

			assertNotNull(builder);

			// Build and verify it creates a valid configuration
			ServerConfiguration config = builder.build();
			assertNotNull(config);
		}
	}
}