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
		assertEquals(User.parse("lambda:lambda"), config.defaultUser());
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
		env.put("DEFAULT_USER", "custom:password");
		env.put("DATABASE_INIT_SCRIPTS", "classpath:init.sql");
		env.put("CONNECTION_POOL_SIZE", "10");
		env.put("CONNECTION_POOL_TIMEOUT", "60");

		LambdaConfiguration config = LambdaConfiguration.create(env);

		assertEquals("jdbc:postgresql://localhost:5432/test", config.databaseUrl());
		assertEquals("testuser", config.databaseUser());
		assertEquals("testpass", config.databasePassword());
		assertEquals(User.parse("custom:password"), config.defaultUser());
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

		LambdaConfiguration config = LambdaConfiguration.create();
		User user = config.extractUser(headers);

		assertEquals("testuser", user.username());
		assertEquals("testpass", String.valueOf(user.password()));
	}

	@Test
	void testExtractUserFromCustomHeader() {
		Map<String, String> headers = new HashMap<>();
		headers.put("X-User", "customuser:custompass");

		LambdaConfiguration config = LambdaConfiguration.create();
		User user = config.extractUser(headers);

		assertEquals("customuser", user.username());
		assertEquals("custompass", String.valueOf(user.password()));
	}

	@Test
	void testExtractUserWithInvalidAuth() {
		Map<String, String> headers = new HashMap<>();
		headers.put("Authorization", "Basic invalid-base64!");

		LambdaConfiguration config = LambdaConfiguration.create();
		User user = config.extractUser(headers);

		// Should fall back to default user
		assertEquals(config.defaultUser(), user);
	}

	@Test
	void testExtractUserWithNoHeaders() {
		LambdaConfiguration config = LambdaConfiguration.create();

		// Test null headers
		assertEquals(config.defaultUser(), config.extractUser(null));

		// Test empty headers
		assertEquals(config.defaultUser(), config.extractUser(new HashMap<>()));
	}
}