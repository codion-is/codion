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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.version.Version;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for DefaultServerInformation.
 * Tests constructor, getter methods, and serialization behavior.
 */
public class DefaultServerInformationTest {

	private static final UUID TEST_SERVER_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");
	private static final String TEST_SERVER_NAME = "TestServer";
	private static final int TEST_SERVER_PORT = 12345;
	private static final ZonedDateTime TEST_START_TIME = ZonedDateTime.parse("2025-01-15T10:30:00Z");
	private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");
	private static final ZonedDateTime TEST_START_TIME_WITH_ZONE = TEST_START_TIME.withZoneSameInstant(TEST_ZONE);

	@Nested
	@DisplayName("Constructor and basic properties")
	class ConstructorTest {

		@Test
		@DisplayName("Constructor stores all parameters correctly")
		void constructor_withAllParameters_storesCorrectly() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			assertEquals(TEST_SERVER_ID, info.id());
			assertEquals(TEST_SERVER_NAME, info.name());
			assertEquals(TEST_SERVER_PORT, info.port());
			assertEquals(TEST_START_TIME, info.startTime());
		}

		@Test
		@DisplayName("Constructor with different time zone preserves zone")
		void constructor_withTimeZone_preservesZone() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME_WITH_ZONE);

			assertEquals(TEST_START_TIME_WITH_ZONE, info.startTime());
			assertEquals(TEST_ZONE, info.timeZone());
		}

		@Test
		@DisplayName("Constructor allows null server ID")
		void constructor_withNullServerId_allowed() {
			DefaultServerInformation info = new DefaultServerInformation(
							null, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			assertNull(info.id());
		}

		@Test
		@DisplayName("Constructor allows null server name")
		void constructor_withNullServerName_allowed() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, null, TEST_SERVER_PORT, TEST_START_TIME);

			assertNull(info.name());
		}

		@Test
		@DisplayName("Constructor allows zero port")
		void constructor_withZeroPort_allowed() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, 0, TEST_START_TIME);

			assertEquals(0, info.port());
		}

		@Test
		@DisplayName("Constructor allows negative port")
		void constructor_withNegativePort_allowed() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, -1, TEST_START_TIME);

			assertEquals(-1, info.port());
		}
	}

	@Nested
	@DisplayName("System properties")
	class SystemPropertiesTest {

		@Test
		@DisplayName("Version returns current Codion version")
		void version_returnsCurrentVersion() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			assertEquals(Version.version(), info.version());
		}

		@Test
		@DisplayName("Locale returns default system locale")
		void locale_returnsDefaultLocale() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			assertEquals(Locale.getDefault(), info.locale());
		}

		@Test
		@DisplayName("Time zone extracted from start time")
		void timeZone_extractedFromStartTime() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME_WITH_ZONE);

			assertEquals(TEST_ZONE, info.timeZone());
		}

		@Test
		@DisplayName("Time zone matches start time zone")
		void timeZone_matchesStartTimeZone() {
			ZoneId zone = ZoneId.of("Europe/London");
			ZonedDateTime startTime = ZonedDateTime.now(zone);

			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, startTime);

			assertEquals(zone, info.timeZone());
			assertEquals(startTime.getZone(), info.timeZone());
		}
	}

	@Nested
	@DisplayName("Edge cases")
	class EdgeCasesTest {

		@Test
		@DisplayName("Empty string server name")
		void constructor_withEmptyServerName_allowed() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, "", TEST_SERVER_PORT, TEST_START_TIME);

			assertEquals("", info.name());
		}

		@Test
		@DisplayName("Maximum port value")
		void constructor_withMaxPort_allowed() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, 65535, TEST_START_TIME);

			assertEquals(65535, info.port());
		}

		@Test
		@DisplayName("Very long server name")
		void constructor_withLongServerName_allowed() {
			String longName = "A".repeat(1000);
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, longName, TEST_SERVER_PORT, TEST_START_TIME);

			assertEquals(longName, info.name());
		}

		@Test
		@DisplayName("Different UUID instances with same values are equal")
		void constructor_withEquivalentUUID_maintainsEquality() {
			UUID uuid1 = UUID.fromString("12345678-1234-1234-1234-123456789012");
			UUID uuid2 = UUID.fromString("12345678-1234-1234-1234-123456789012");

			DefaultServerInformation info1 = new DefaultServerInformation(
							uuid1, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);
			DefaultServerInformation info2 = new DefaultServerInformation(
							uuid2, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			assertEquals(uuid1, uuid2);
			assertEquals(info1.id(), info2.id());
		}
	}

	@Nested
	@DisplayName("Serialization")
	class SerializationTest {

		@Test
		@DisplayName("Serialization preserves all properties")
		void serialization_preservesAllProperties() throws IOException, ClassNotFoundException {
			DefaultServerInformation original = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME_WITH_ZONE);

			DefaultServerInformation deserialized = serializeAndDeserialize(original);

			assertEquals(original.id(), deserialized.id());
			assertEquals(original.name(), deserialized.name());
			assertEquals(original.port(), deserialized.port());
			assertEquals(original.startTime(), deserialized.startTime());
			assertEquals(original.version(), deserialized.version());
			assertEquals(original.locale(), deserialized.locale());
			assertEquals(original.timeZone(), deserialized.timeZone());
		}

		@Test
		@DisplayName("Serialization with null values")
		void serialization_withNullValues_works() throws IOException, ClassNotFoundException {
			DefaultServerInformation original = new DefaultServerInformation(
							null, null, TEST_SERVER_PORT, TEST_START_TIME);

			DefaultServerInformation deserialized = serializeAndDeserialize(original);

			assertNull(deserialized.id());
			assertNull(deserialized.name());
			assertEquals(original.port(), deserialized.port());
			assertEquals(original.startTime(), deserialized.startTime());
		}

		@Test
		@DisplayName("Serialization maintains version consistency")
		void serialization_maintainsVersionConsistency() throws IOException, ClassNotFoundException {
			DefaultServerInformation original = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			DefaultServerInformation deserialized = serializeAndDeserialize(original);

			// Version should be the same as it's determined at runtime
			assertEquals(Version.version(), deserialized.version());
		}

		@Test
		@DisplayName("Serialization maintains locale consistency")
		void serialization_maintainsLocaleConsistency() throws IOException, ClassNotFoundException {
			DefaultServerInformation original = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			DefaultServerInformation deserialized = serializeAndDeserialize(original);

			// Locale should be the same as it's determined at runtime
			assertEquals(Locale.getDefault(), deserialized.locale());
		}

		private DefaultServerInformation serializeAndDeserialize(DefaultServerInformation original)
						throws IOException, ClassNotFoundException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
				oos.writeObject(original);
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			try (ObjectInputStream ois = new ObjectInputStream(bais)) {
				return (DefaultServerInformation) ois.readObject();
			}
		}
	}

	@Nested
	@DisplayName("Interface compliance")
	class InterfaceComplianceTest {

		@Test
		@DisplayName("Implements ServerInformation interface")
		void implementsServerInformationInterface() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			assertInstanceOf(ServerInformation.class, info);
		}

		@Test
		@DisplayName("All interface methods return non-null or expected null")
		void allInterfaceMethods_returnAppropriateValues() {
			DefaultServerInformation info = new DefaultServerInformation(
							TEST_SERVER_ID, TEST_SERVER_NAME, TEST_SERVER_PORT, TEST_START_TIME);

			// These should never be null
			assertNotNull(info.version());
			assertNotNull(info.locale());
			assertNotNull(info.startTime());
			assertNotNull(info.timeZone());

			// These can be null based on constructor parameters
			assertNotNull(info.id()); // Not null in this test case
			assertNotNull(info.name()); // Not null in this test case
		}

		@Test
		@DisplayName("Interface methods handle null constructor parameters appropriately")
		void interfaceMethods_withNullConstructorParams_handleGracefully() {
			DefaultServerInformation info = new DefaultServerInformation(
							null, null, TEST_SERVER_PORT, TEST_START_TIME);

			// These should still work even with null constructor params
			assertNotNull(info.version());
			assertNotNull(info.locale());
			assertNotNull(info.startTime());
			assertNotNull(info.timeZone());
			assertEquals(TEST_SERVER_PORT, info.port());

			// These will be null due to constructor
			assertNull(info.id());
			assertNull(info.name());
		}
	}
}