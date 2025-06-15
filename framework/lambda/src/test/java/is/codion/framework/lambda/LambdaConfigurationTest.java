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
package is.codion.framework.lambda;

import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class LambdaConfigurationTest {

	@Test
	void testDefaultConfiguration() {
		LambdaConfiguration config = LambdaConfiguration.create();

		// Test defaults
		assertEquals("jdbc:h2:mem:codion", config.databaseUrl());
		assertEquals("sa", config.databaseUser());
		assertEquals("", config.databasePassword());
		assertEquals(5, config.connectionPoolSize());
		assertEquals(30, config.connectionPoolTimeout());
		assertFalse(config.databaseInitScripts().isPresent());
	}

	@Test
	void testCustomConfiguration() {
		Map<String, String> env = new HashMap<>();
		env.put("DATABASE_URL", "jdbc:postgresql://localhost:5432/test");
		env.put("DATABASE_USER", "testuser");
		env.put("DATABASE_PASSWORD", "testpass");
		env.put("DATABASE_INIT_SCRIPTS", "classpath:init.sql");
		env.put("CONNECTION_POOL_SIZE", "10");
		env.put("CONNECTION_POOL_TIMEOUT", "60");

		LambdaConfiguration config = LambdaConfiguration.create(env);

		assertEquals("jdbc:postgresql://localhost:5432/test", config.databaseUrl());
		assertEquals("testuser", config.databaseUser());
		assertEquals("testpass", config.databasePassword());
		assertEquals(10, config.connectionPoolSize());
		assertEquals(60, config.connectionPoolTimeout());
		assertTrue(config.databaseInitScripts().isPresent());
		assertEquals("classpath:init.sql", config.databaseInitScripts().get());
	}

	@Test
	void testInvalidNumberConfiguration() {
		Map<String, String> env = new HashMap<>();
		env.put("CONNECTION_POOL_SIZE", "invalid");
		env.put("CONNECTION_POOL_TIMEOUT", "also-invalid");

		LambdaConfiguration config = LambdaConfiguration.create(env);

		// Should fall back to defaults on parse errors
		assertEquals(5, config.connectionPoolSize());
		assertEquals(30, config.connectionPoolTimeout());
	}

	@Test
	void testExtractUserFromBasicAuth() {
		Map<String, String> headers = new HashMap<>();
		String credentials = Base64.getEncoder().encodeToString("testuser:testpass".getBytes());
		headers.put("Authorization", "Basic " + credentials);

		User user = LambdaConfiguration.extractUser(headers);

		assertEquals("testuser", user.username());
		assertEquals("testpass", String.valueOf(user.password()));
	}

	@Test
	void testExtractUserFromCustomHeader() {
		Map<String, String> headers = new HashMap<>();
		headers.put("X-User", "customuser:custompass");

		User user = LambdaConfiguration.extractUser(headers);

		assertEquals("customuser", user.username());
		assertEquals("custompass", String.valueOf(user.password()));
	}

	@Test
	void testExtractUserFromLowercaseHeaders() {
		Map<String, String> headers = new HashMap<>();
		// Test with lowercase headers (as Lambda Function URLs convert headers to lowercase)
		headers.put("x-user", "customuser:custompass");

		User user = LambdaConfiguration.extractUser(headers);

		assertEquals("customuser", user.username());
		assertEquals("custompass", String.valueOf(user.password()));
	}

	@Test
	void testExtractUserFromInvalidCustomHeader() {
		Map<String, String> headers = new HashMap<>();
		headers.put("X-User", ""); // Empty string should fail

		// Should throw for invalid user format
		assertThrows(IllegalArgumentException.class, () -> LambdaConfiguration.extractUser(headers));
	}

	@Test
	void testExtractUserWithInvalidAuth() {
		Map<String, String> headers = new HashMap<>();
		headers.put("Authorization", "Basic invalid-base64!");

		// Should throw exception for invalid auth
		assertThrows(IllegalArgumentException.class, () -> LambdaConfiguration.extractUser(headers));
	}

	@Test
	void testExtractUserWithNoHeaders() {
		// Test null headers - should throw
		assertThrows(IllegalArgumentException.class, () -> LambdaConfiguration.extractUser(null));

		// Test empty headers - should throw
		assertThrows(IllegalArgumentException.class, () -> LambdaConfiguration.extractUser(new HashMap<>()));
	}
}