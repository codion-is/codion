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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.user;

import is.codion.common.Serializer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 11.4.2010
 * Time: 13:50:45
 */
public class UserTest {

	@Test
	void test() throws IOException, ClassNotFoundException {
		User user = User.parse("scott:tiger");
		assertEquals("scott", user.username());
		assertEquals("tiger", String.valueOf(user.password()));
		assertEquals("scott".hashCode(), user.hashCode());
		assertEquals(User.user("scott"), user);
		user.clearPassword();
		assertEquals("", String.valueOf(user.password()));
		assertNotEquals("scott", user);
		assertEquals(user, User.parse("scott:blabla"));
		assertEquals(User.user("scott"), User.user("ScoTT"));
		assertEquals(Serializer.deserialize(Serializer.serialize(User.user("scott", "test".toCharArray()))), User.user("Scott"));
		User copy = user.copy();
		assertEquals(user.username(), copy.username());
		assertNotSame(user.password(), copy.password());
		assertEquals(String.valueOf(user.password()), String.valueOf(copy.password()));
		assertEquals("User: scott", user.toString());
		copy.clearPassword();
		assertNotNull(copy.password());
		assertEquals(0, copy.password().length);
	}

	@Test
	void parse() {
		User user = User.parse("scott:tiger");
		assertEquals("scott", user.username());
		assertEquals("tiger", String.valueOf(user.password()));
		user = User.parse(" scott:ti ger");
		assertEquals("scott", user.username());
		assertEquals("ti ger", String.valueOf(user.password()));
		user = User.parse("pete");
		assertEquals("pete", user.username());
		assertTrue(String.valueOf(user.password()).isEmpty());
		user = User.parse(" john ");
		assertEquals("john", user.username());
		assertTrue(String.valueOf(user.password()).isEmpty());
		user = User.parse("scott:tiger:pet:e");
		assertEquals("scott", user.username());
		assertEquals("tiger:pet:e", String.valueOf(user.password()));
	}

	@Test
	void parseNoUsername() {
		assertThrows(IllegalArgumentException.class, () -> User.parse(":tiger"));
		assertThrows(IllegalArgumentException.class, () -> User.parse("::"));
		assertThrows(IllegalArgumentException.class, () -> User.parse("::tiger:"));
	}

	@Test
	void parseNoUserInfo() {
		assertThrows(IllegalArgumentException.class, () -> User.parse(""));
	}

	@Test
	void resourceExhaustion_OversizedUsername() {
		// Verify length limit prevents DoS
		String hugeUsername = "a".repeat(10_000_000); // 10MB username
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
						() -> User.user(hugeUsername));
		assertTrue(exception.getMessage().contains("Username length"));
		assertTrue(exception.getMessage().contains("exceeds maximum allowed length"));
	}

	@Test
	void resourceExhaustion_OversizedPassword() {
		// Verify length limit prevents DoS
		char[] hugePassword = new char[10_000_000]; // 10MB password
		Arrays.fill(hugePassword, 'x');
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> User.user("scott", hugePassword));
		assertTrue(exception.getMessage().contains("Password length"));
		assertTrue(exception.getMessage().contains("exceeds maximum allowed length"));
	}

	@Test
	void maxLengthConfiguration() {
		// Verify default limits (fixed at class load time)
		assertEquals(256, User.MAXIMUM_USERNAME_LENGTH.get());
		assertEquals(1024, User.MAXIMUM_PASSWORD_LENGTH.get());

		// Verify we can create users up to the limit
		String maxUsername = "a".repeat(256);
		char[] maxPassword = new char[1024];
		Arrays.fill(maxPassword, 'x');
		User user = User.user(maxUsername, maxPassword);
		assertEquals(maxUsername, user.username());
		assertEquals(1024, user.password().length);

		// Verify one character over fails
		assertThrows(IllegalArgumentException.class, () -> User.user(maxUsername + "a"));
		char[] tooLongPassword = new char[1025];
		assertThrows(IllegalArgumentException.class, () -> User.user("scott", tooLongPassword));
	}
}
