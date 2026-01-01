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
package is.codion.common.rmi.server.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for server exception classes.
 * Tests constructor behavior, inheritance hierarchy, and serialization.
 */
public class ServerExceptionTest {

	private static final String TEST_MESSAGE = "Test exception message";
	private static final String EMPTY_MESSAGE = "";
	private static final String NULL_MESSAGE = null;

	@Nested
	@DisplayName("ServerException tests")
	class ServerExceptionTests {

		@Test
		@DisplayName("Constructor handles various message types")
		void constructor_handlesVariousMessages() {
			assertEquals(TEST_MESSAGE, new TestableServerException(TEST_MESSAGE).getMessage());
			assertNull(new TestableServerException(NULL_MESSAGE).getMessage());
			assertEquals(EMPTY_MESSAGE, new TestableServerException(EMPTY_MESSAGE).getMessage());
		}

		@Test
		@DisplayName("Extends Exception class")
		void extendsException() {
			ServerException exception = new TestableServerException(TEST_MESSAGE);

			assertInstanceOf(Exception.class, exception);
		}

		@Test
		@DisplayName("Serialization preserves message")
		void serialization_preservesMessage() throws IOException, ClassNotFoundException {
			ServerException original = new TestableServerException(TEST_MESSAGE);

			ServerException deserialized = serializeAndDeserialize(original);

			assertEquals(original.getMessage(), deserialized.getMessage());
		}

		@Test
		@DisplayName("Serialization with null message")
		void serialization_withNullMessage_works() throws IOException, ClassNotFoundException {
			ServerException original = new TestableServerException(NULL_MESSAGE);

			ServerException deserialized = serializeAndDeserialize(original);

			assertEquals(original.getMessage(), deserialized.getMessage());
		}
	}

	@Nested
	@DisplayName("LoginException tests")
	class LoginExceptionTests {

		@Test
		@DisplayName("Constructor handles various message types")
		void constructor_handlesVariousMessages() {
			assertEquals(TEST_MESSAGE, new LoginException(TEST_MESSAGE).getMessage());
			assertNull(new LoginException(NULL_MESSAGE).getMessage());
		}

		@Test
		@DisplayName("Extends ServerException")
		void extendsServerException() {
			LoginException exception = new LoginException(TEST_MESSAGE);

			assertInstanceOf(ServerException.class, exception);
			assertInstanceOf(Exception.class, exception);
		}


		@Test
		@DisplayName("Serialization preserves message")
		void serialization_preservesMessage() throws IOException, ClassNotFoundException {
			LoginException original = new LoginException(TEST_MESSAGE);

			LoginException deserialized = (LoginException) serializeAndDeserialize(original);

			assertEquals(original.getMessage(), deserialized.getMessage());
			assertInstanceOf(LoginException.class, deserialized);
		}
	}

	@Nested
	@DisplayName("ServerAuthenticationException tests")
	class ServerAuthenticationExceptionTests {

		@Test
		@DisplayName("Constructor with message creates exception")
		void constructor_withMessage_createsException() {
			ServerAuthenticationException exception = new ServerAuthenticationException(TEST_MESSAGE);

			assertEquals(TEST_MESSAGE, exception.getMessage());
		}

		@Test
		@DisplayName("Constructor with null message creates exception")
		void constructor_withNullMessage_createsException() {
			ServerAuthenticationException exception = new ServerAuthenticationException(NULL_MESSAGE);

			assertNull(exception.getMessage());
		}

		@Test
		@DisplayName("Extends LoginException")
		void extendsLoginException() {
			ServerAuthenticationException exception = new ServerAuthenticationException(TEST_MESSAGE);

			assertInstanceOf(LoginException.class, exception);
			assertInstanceOf(ServerException.class, exception);
			assertInstanceOf(Exception.class, exception);
		}

		@Test
		@DisplayName("Is final class")
		void isFinalClass() {
			// Verify that the class is marked as final
			assertTrue(java.lang.reflect.Modifier.isFinal(ServerAuthenticationException.class.getModifiers()));
		}


		@Test
		@DisplayName("Serialization preserves message")
		void serialization_preservesMessage() throws IOException, ClassNotFoundException {
			ServerAuthenticationException original = new ServerAuthenticationException(TEST_MESSAGE);

			ServerAuthenticationException deserialized =
							(ServerAuthenticationException) serializeAndDeserialize(original);

			assertEquals(original.getMessage(), deserialized.getMessage());
			assertInstanceOf(ServerAuthenticationException.class, deserialized);
		}
	}

	@Nested
	@DisplayName("ConnectionNotAvailableException tests")
	class ConnectionNotAvailableExceptionTests {

		@Test
		@DisplayName("Default constructor creates exception with localized message")
		void defaultConstructor_createsExceptionWithLocalizedMessage() {
			ConnectionNotAvailableException exception = new ConnectionNotAvailableException();

			assertNotNull(exception.getMessage());
			assertFalse(exception.getMessage().isEmpty());
			// The message should be loaded from the resource bundle
			assertEquals("This server is not accepting more connections", exception.getMessage());
		}

		@Test
		@DisplayName("Extends ServerException")
		void extendsServerException() {
			ConnectionNotAvailableException exception = new ConnectionNotAvailableException();

			assertInstanceOf(ServerException.class, exception);
			assertInstanceOf(Exception.class, exception);
		}

		@Test
		@DisplayName("Is final class")
		void isFinalClass() {
			assertTrue(java.lang.reflect.Modifier.isFinal(ConnectionNotAvailableException.class.getModifiers()));
		}

		@Test
		@DisplayName("No-arg constructor always produces same message")
		void noArgConstructor_consistentMessage() {
			ConnectionNotAvailableException exception1 = new ConnectionNotAvailableException();
			ConnectionNotAvailableException exception2 = new ConnectionNotAvailableException();

			assertEquals(exception1.getMessage(), exception2.getMessage());
		}

		@Test
		@DisplayName("Message is not null or empty")
		void message_isNotNullOrEmpty() {
			ConnectionNotAvailableException exception = new ConnectionNotAvailableException();

			assertNotNull(exception.getMessage());
			assertFalse(exception.getMessage().trim().isEmpty());
		}

		@Test
		@DisplayName("Serialization preserves localized message")
		void serialization_preservesLocalizedMessage() throws IOException, ClassNotFoundException {
			ConnectionNotAvailableException original = new ConnectionNotAvailableException();

			ConnectionNotAvailableException deserialized =
							(ConnectionNotAvailableException) serializeAndDeserialize(original);

			assertEquals(original.getMessage(), deserialized.getMessage());
			assertInstanceOf(ConnectionNotAvailableException.class, deserialized);
		}
	}

	@Nested
	@DisplayName("Exception hierarchy tests")
	class ExceptionHierarchyTests {

		@Test
		@DisplayName("All server exceptions extend ServerException")
		void allServerExceptions_extendServerException() {
			assertInstanceOf(ServerException.class, new LoginException(TEST_MESSAGE));
			assertInstanceOf(ServerException.class, new ServerAuthenticationException(TEST_MESSAGE));
			assertInstanceOf(ServerException.class, new ConnectionNotAvailableException());
		}

		@Test
		@DisplayName("ServerAuthenticationException is a LoginException")
		void serverAuthenticationException_isLoginException() {
			ServerAuthenticationException exception = new ServerAuthenticationException(TEST_MESSAGE);

			assertInstanceOf(LoginException.class, exception);
		}

		@Test
		@DisplayName("ConnectionNotAvailableException is not a LoginException")
		void connectionNotAvailableException_isNotLoginException() {
			ConnectionNotAvailableException exception = new ConnectionNotAvailableException();

			// Check using class hierarchy
			assertFalse(LoginException.class.isAssignableFrom(exception.getClass()));
		}

		@Test
		@DisplayName("Exception type distinction")
		void exceptionTypeDistinction() {
			LoginException loginException = new LoginException(TEST_MESSAGE);
			ServerAuthenticationException authException = new ServerAuthenticationException(TEST_MESSAGE);
			ConnectionNotAvailableException connException = new ConnectionNotAvailableException();

			// Check class hierarchy relationships
			assertFalse(ServerAuthenticationException.class.isAssignableFrom(loginException.getClass()));
			assertFalse(ConnectionNotAvailableException.class.isAssignableFrom(loginException.getClass()));
			assertFalse(ConnectionNotAvailableException.class.isAssignableFrom(authException.getClass()));

			// Verify correct relationships do exist
			assertTrue(LoginException.class.isAssignableFrom(authException.getClass()));
			assertTrue(ServerException.class.isAssignableFrom(connException.getClass()));
		}
	}

	@Nested
	@DisplayName("Error handling scenarios")
	class ErrorHandlingScenariosTests {

		@Test
		@DisplayName("Throwing and catching ServerException")
		void throwingAndCatching_serverException() {
			assertThrows(ServerException.class, () -> {
				throw new TestableServerException(TEST_MESSAGE);
			});
		}

		@Test
		@DisplayName("Throwing and catching LoginException")
		void throwingAndCatching_loginException() {
			LoginException caught = assertThrows(LoginException.class, () -> {
				throw new LoginException(TEST_MESSAGE);
			});

			assertEquals(TEST_MESSAGE, caught.getMessage());
		}

		@Test
		@DisplayName("Catching LoginException as ServerException")
		void catchingLoginException_asServerException() {
			ServerException caught = assertThrows(ServerException.class, () -> {
				throw new LoginException(TEST_MESSAGE);
			});

			assertInstanceOf(LoginException.class, caught);
			assertEquals(TEST_MESSAGE, caught.getMessage());
		}

		@Test
		@DisplayName("Catching ServerAuthenticationException as LoginException")
		void catchingServerAuthenticationException_asLoginException() {
			LoginException caught = assertThrows(LoginException.class, () -> {
				throw new ServerAuthenticationException(TEST_MESSAGE);
			});

			assertInstanceOf(ServerAuthenticationException.class, caught);
			assertEquals(TEST_MESSAGE, caught.getMessage());
		}

		@Test
		@DisplayName("Multiple exception types in try-catch")
		void multipleExceptionTypes_inTryCatch() {
			// Test that we can catch different types appropriately
			assertThrows(LoginException.class, () -> {
				try {
					throw new ServerAuthenticationException("Auth failed");
				}
				catch (ServerAuthenticationException e) {
					// Re-throw as general LoginException
					throw new LoginException("Login failed: " + e.getMessage());
				}
			});
		}
	}

	// Helper methods
	private <T extends Exception> T serializeAndDeserialize(T exception)
					throws IOException, ClassNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(exception);
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		try (ObjectInputStream ois = new ObjectInputStream(bais)) {
			return (T) ois.readObject();
		}
	}

	// Test helper class to make package-private ServerException testable
	private static class TestableServerException extends ServerException {
		TestableServerException(String message) {
			super(message);
		}
	}
}