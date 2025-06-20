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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ConnectionRequest builder and properties.
 * Expanded to cover edge cases, validation, and builder patterns.
 */
public final class ClientsTest {

	private static final String TEST_USER_SPEC = "scott:tiger";
	private static final String TEST_CLIENT_TYPE = "TestClient";
	private static final String EMPTY_CLIENT_TYPE = "";
	private static final String LONG_CLIENT_TYPE = "A".repeat(1000);

	@Nested
	@DisplayName("ConnectionRequest builder tests")
	class ConnectionRequestBuilderTests {

		@Test
		@DisplayName("Basic builder with required fields creates valid request")
		void builder_withRequiredFields_createsValidRequest() {
			User user = User.parse(TEST_USER_SPEC);
			UUID clientId = UUID.randomUUID();

			ConnectionRequest request = ConnectionRequest.builder()
							.user(user)
							.clientId(clientId)
							.clientType(TEST_CLIENT_TYPE)
							.build();

			assertEquals(user, request.user());
			assertEquals(clientId, request.clientId());
			assertEquals(TEST_CLIENT_TYPE, request.clientType());
			assertFalse(request.version().isPresent());
			assertEquals(Version.version(), request.frameworkVersion());
		}

		@Test
		@DisplayName("Builder with version creates request with version")
		void builder_withVersion_createsRequestWithVersion() {
			User user = User.parse(TEST_USER_SPEC);
			UUID clientId = UUID.randomUUID();
			Version clientVersion = Version.parse("1.2.3");

			ConnectionRequest request = ConnectionRequest.builder()
							.user(user)
							.clientId(clientId)
							.clientType(TEST_CLIENT_TYPE)
							.version(clientVersion)
							.build();

			assertTrue(request.version().isPresent());
			assertEquals(clientVersion, request.version().get());
		}

		@Test
		@DisplayName("Builder with null user throws exception")
		void builder_withNullUser_throwsException() {
			assertThrows(NullPointerException.class, () ->
							ConnectionRequest.builder()
											.user(null)
											.clientId(UUID.randomUUID())
											.clientType(TEST_CLIENT_TYPE)
											.build());
		}

		@Test
		@DisplayName("Builder with null client ID throws exception")
		void builder_withNullClientId_throwsException() {
			assertThrows(NullPointerException.class, () ->
							ConnectionRequest.builder()
											.user(User.parse(TEST_USER_SPEC))
											.clientId(null)
											.clientType(TEST_CLIENT_TYPE)
											.build());
		}

		@Test
		@DisplayName("Builder with null client type throws exception")
		void builder_withNullClientType_throwsException() {
			assertThrows(NullPointerException.class, () ->
							ConnectionRequest.builder()
											.user(User.parse(TEST_USER_SPEC))
											.clientId(UUID.randomUUID())
											.clientType(null)
											.build());
		}
	}

	@Nested
	@DisplayName("ConnectionRequest property tests")
	class ConnectionRequestPropertyTests {

		@Test
		@DisplayName("Hash code is based on client ID")
		void hashCode_basedOnClientId() {
			UUID clientId = UUID.randomUUID();
			ConnectionRequest request = createTestRequest(clientId);

			assertEquals(clientId.hashCode(), request.hashCode());
		}

		@Test
		@DisplayName("Framework version returns current version")
		void frameworkVersion_returnsCurrentVersion() {
			ConnectionRequest request = createTestRequest();

			assertEquals(Version.version(), request.frameworkVersion());
		}

		@Test
		@DisplayName("String representation contains username")
		void toString_containsUsername() {
			User user = User.parse(TEST_USER_SPEC);
			ConnectionRequest request = ConnectionRequest.builder()
							.user(user)
							.clientId(UUID.randomUUID())
							.clientType(TEST_CLIENT_TYPE)
							.build();

			String stringRep = request.toString();
			assertTrue(stringRep.contains(user.username()));
		}
	}

	@Nested
	@DisplayName("ConnectionRequest edge cases")
	class ConnectionRequestEdgeCaseTests {

		@Test
		@DisplayName("Empty client type is allowed")
		void clientType_empty_isAllowed() {
			ConnectionRequest request = ConnectionRequest.builder()
							.user(User.parse(TEST_USER_SPEC))
							.clientId(UUID.randomUUID())
							.clientType(EMPTY_CLIENT_TYPE)
							.build();

			assertEquals(EMPTY_CLIENT_TYPE, request.clientType());
		}

		@Test
		@DisplayName("Very long client type is allowed")
		void clientType_veryLong_isAllowed() {
			ConnectionRequest request = ConnectionRequest.builder()
							.user(User.parse(TEST_USER_SPEC))
							.clientId(UUID.randomUUID())
							.clientType(LONG_CLIENT_TYPE)
							.build();

			assertEquals(LONG_CLIENT_TYPE, request.clientType());
		}

		@Test
		@DisplayName("User with empty password is allowed")
		void user_withEmptyPassword_isAllowed() {
			User user = User.user("testuser", new char[0]);
			ConnectionRequest request = ConnectionRequest.builder()
							.user(user)
							.clientId(UUID.randomUUID())
							.clientType(TEST_CLIENT_TYPE)
							.build();

			assertEquals(user, request.user());
		}

		@Test
		@DisplayName("User with special characters is allowed")
		void user_withSpecialCharacters_isAllowed() {
			User user = User.user("test@domain.com", "p@$$w0rd!".toCharArray());
			ConnectionRequest request = ConnectionRequest.builder()
							.user(user)
							.clientId(UUID.randomUUID())
							.clientType(TEST_CLIENT_TYPE)
							.build();

			assertEquals(user, request.user());
		}
	}

	@Nested
	@DisplayName("ConnectionRequest equality and identity")
	class ConnectionRequestEqualityTests {

		@Test
		@DisplayName("Requests with same client ID have same hash code")
		void requests_withSameClientId_haveSameHashCode() {
			UUID clientId = UUID.randomUUID();
			ConnectionRequest request1 = createTestRequest(clientId);
			ConnectionRequest request2 = createTestRequest(clientId);

			assertEquals(request1.hashCode(), request2.hashCode());
		}

		@Test
		@DisplayName("Requests with different client IDs have different hash codes")
		void requests_withDifferentClientIds_haveDifferentHashCodes() {
			ConnectionRequest request1 = createTestRequest(UUID.randomUUID());
			ConnectionRequest request2 = createTestRequest(UUID.randomUUID());

			assertNotEquals(request1.hashCode(), request2.hashCode());
		}

		@Test
		@DisplayName("Request properties are immutable after creation")
		void request_propertiesAreImmutable() {
			User user = User.parse(TEST_USER_SPEC);
			UUID clientId = UUID.randomUUID();
			ConnectionRequest request = createTestRequest(user, clientId);

			// Properties should be the same objects (immutable)
			assertSame(user, request.user());
			assertSame(clientId, request.clientId());
		}
	}

	@Nested
	@DisplayName("Version handling")
	class VersionHandlingTests {

		@Test
		@DisplayName("Version optional is empty by default")
		void version_emptyByDefault() {
			ConnectionRequest request = createTestRequest();

			assertFalse(request.version().isPresent());
		}

		@Test
		@DisplayName("Null version results in empty optional")
		void version_nullResultsInEmptyOptional() {
			ConnectionRequest request = ConnectionRequest.builder()
							.user(User.parse(TEST_USER_SPEC))
							.clientId(UUID.randomUUID())
							.clientType(TEST_CLIENT_TYPE)
							.version(null)
							.build();

			assertFalse(request.version().isPresent());
		}

		@Test
		@DisplayName("Valid version is preserved")
		void version_validVersionPreserved() {
			Version testVersion = Version.parse("2.1.0");
			ConnectionRequest request = ConnectionRequest.builder()
							.user(User.parse(TEST_USER_SPEC))
							.clientId(UUID.randomUUID())
							.clientType(TEST_CLIENT_TYPE)
							.version(testVersion)
							.build();

			assertTrue(request.version().isPresent());
			assertEquals(testVersion, request.version().get());
		}
	}

	// Helper methods
	private ConnectionRequest createTestRequest() {
		return createTestRequest(UUID.randomUUID());
	}

	private ConnectionRequest createTestRequest(UUID clientId) {
		return createTestRequest(User.parse(TEST_USER_SPEC), clientId);
	}

	private ConnectionRequest createTestRequest(User user, UUID clientId) {
		return ConnectionRequest.builder()
						.user(user)
						.clientId(clientId)
						.clientType(TEST_CLIENT_TYPE)
						.build();
	}
}
