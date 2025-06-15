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

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Configuration abstraction for AWS Lambda deployments.
 * Provides access to environment variables and request headers
 * with defaults and type conversion.
 */
public final class LambdaConfiguration {

	private static final String DATABASE_URL = "DATABASE_URL";
	private static final String DATABASE_USER = "DATABASE_USER";
	private static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";
	private static final String DATABASE_INIT_SCRIPTS = "DATABASE_INIT_SCRIPTS";
	private static final String CONNECTION_POOL_SIZE = "CONNECTION_POOL_SIZE";
	private static final String CONNECTION_POOL_TIMEOUT = "CONNECTION_POOL_TIMEOUT";

	private static final String DEFAULT_DATABASE_URL = "jdbc:h2:mem:codion";
	private static final String DEFAULT_DATABASE_USER = "sa";
	private static final String DEFAULT_DATABASE_PASSWORD = "";
	private static final int DEFAULT_CONNECTION_POOL_SIZE = 5;
	private static final int DEFAULT_CONNECTION_POOL_TIMEOUT = 30;

	private final Map<String, String> environment;

	private LambdaConfiguration(Map<String, String> environment) {
		this.environment = environment;
	}

	/**
	 * @return the database URL
	 */
	public String databaseUrl() {
		return environment.getOrDefault(DATABASE_URL, DEFAULT_DATABASE_URL);
	}

	/**
	 * @return the database username
	 */
	public String databaseUser() {
		return environment.getOrDefault(DATABASE_USER, DEFAULT_DATABASE_USER);
	}

	/**
	 * @return the database password
	 */
	public String databasePassword() {
		return environment.getOrDefault(DATABASE_PASSWORD, DEFAULT_DATABASE_PASSWORD);
	}

	/**
	 * @return the database initialization scripts, if specified
	 */
	public Optional<String> databaseInitScripts() {
		return Optional.ofNullable(environment.get(DATABASE_INIT_SCRIPTS));
	}

	/**
	 * @return the connection pool size
	 */
	public int connectionPoolSize() {
		String size = environment.get(CONNECTION_POOL_SIZE);
		if (size != null) {
			try {
				return Integer.parseInt(size);
			}
			catch (NumberFormatException e) {
				// Fall through to default
			}
		}
		return DEFAULT_CONNECTION_POOL_SIZE;
	}

	/**
	 * @return the connection pool timeout in seconds
	 */
	public int connectionPoolTimeout() {
		String timeout = environment.get(CONNECTION_POOL_TIMEOUT);
		if (timeout != null) {
			try {
				return Integer.parseInt(timeout);
			}
			catch (NumberFormatException e) {
				// Fall through to default
			}
		}
		return DEFAULT_CONNECTION_POOL_TIMEOUT;
	}

	/**
	 * Extracts user from request headers.
	 * Supports Basic authentication and custom X-User header.
	 * @param headers the request headers
	 * @return the authenticated user
	 * @throws IllegalArgumentException if no authentication is provided
	 */
	static User extractUser(Map<String, String> headers) {
		if (headers == null) {
			throw new IllegalArgumentException("No authentication provided - missing headers");
		}

		// Check for Basic auth header (Lambda Function URLs lowercase headers)
		String auth = headers.get("authorization");
		if (auth == null) {
			auth = headers.get("Authorization");
		}
		if (auth != null && auth.startsWith("Basic ")) {
			try {
				String encoded = auth.substring(6);
				byte[] decoded = java.util.Base64.getDecoder().decode(encoded);
				return User.parse(new String(decoded));
			}
			catch (Exception e) {
				// Fall through to default
			}
		}

		// Check for custom user header (try both cases)
		String userHeader = headers.get("x-user");
		if (userHeader == null) {
			userHeader = headers.get("X-User");
		}
		if (userHeader != null) {
			try {
				return User.parse(userHeader);
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Invalid X-User header format: " + e.getMessage());
			}
		}

		throw new IllegalArgumentException("No authentication provided - missing Authorization or X-User header");
	}

	/**
	 * Creates a configuration backed by environment variables.
	 * @return a configuration instance
	 */
	public static LambdaConfiguration create() {
		return new LambdaConfiguration(System.getenv());
	}

	/**
	 * Creates a configuration with custom environment map (for testing).
	 * @param environment the environment map
	 * @return a configuration instance
	 */
	public static LambdaConfiguration create(Map<String, String> environment) {
		return new LambdaConfiguration(requireNonNull(environment));
	}
}