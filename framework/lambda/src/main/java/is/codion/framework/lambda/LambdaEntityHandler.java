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

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.Domain;

import com.amazonaws.services.lambda.runtime.Context;

import java.sql.Connection;
import java.util.Map;

/**
 * Interface for customizing Lambda entity handlers.
 * Implementations can override specific behaviors while
 * inheriting the core protocol handling from {@link AbstractLambdaEntityHandler}.
 */
public interface LambdaEntityHandler {

	/**
	 * @return the domain this handler serves
	 */
	Domain domain();

	/**
	 * @return the configuration for this handler
	 */
	LambdaConfiguration configuration();

	/**
	 * Called before processing a request.
	 * Can be used for custom authentication, logging, etc.
	 * @param path the request path
	 * @param headers the request headers
	 * @param context the Lambda context
	 * @throws Exception if the request should be rejected
	 */
	default void beforeRequest(String path, Map<String, String> headers, Context context) throws Exception {
		// Default: no-op
	}

	/**
	 * Called after successfully processing a request.
	 * Can be used for logging, metrics, cleanup, etc.
	 * @param path the request path
	 * @param headers the request headers
	 * @param context the Lambda context
	 */
	default void afterRequest(String path, Map<String, String> headers, Context context) {
		// Default: no-op
	}

	/**
	 * Called when an error occurs during request processing.
	 * @param path the request path
	 * @param headers the request headers
	 * @param error the error that occurred
	 * @param context the Lambda context
	 */
	default void onError(String path, Map<String, String> headers, Exception error, Context context) {
		// Default: log the error
		context.getLogger().log("Error processing " + path + ": " + error.getMessage());
	}

	/**
	 * Extracts the user from request headers.
	 * Override to implement custom authentication logic.
	 * @param headers the request headers
	 * @return the authenticated user
	 */
	default User extractUser(Map<String, String> headers) {
		return configuration().extractUser(headers);
	}

	/**
	 * Creates a JDBC connection for the given user.
	 * Override to implement custom connection logic (e.g., per-tenant databases).
	 * @param user the authenticated user
	 * @return a JDBC connection
	 * @throws Exception if connection creation fails
	 */
	Connection createConnection(User user) throws Exception;

	/**
	 * Wraps a JDBC connection in a LocalEntityConnection.
	 * Override to customize connection behavior.
	 * @param jdbcConnection the JDBC connection
	 * @param user the authenticated user
	 * @return a LocalEntityConnection
	 * @throws Exception if wrapping fails
	 */
	default LocalEntityConnection wrapConnection(Connection jdbcConnection, User user) throws Exception {
		Database database = Database.instance();
		return LocalEntityConnection.localEntityConnection(database, domain(), jdbcConnection);
	}

	/**
	 * Handles execute operations.
	 * Override to enable function/consumer execution.
	 * @param executable the function/consumer to execute
	 * @param connection the entity connection
	 * @return the result of execution
	 * @throws Exception if execution fails
	 */
	default Object handleExecute(Object executable, EntityConnection connection) throws Exception {
		throw new UnsupportedOperationException("Execute operations not supported");
	}

	/**
	 * Handles stored procedure calls.
	 * Override to enable procedure execution.
	 * @param procedureName the procedure name
	 * @param arguments the procedure arguments
	 * @param connection the entity connection
	 * @throws Exception if execution fails
	 */
	default void handleProcedure(String procedureName, Object arguments, EntityConnection connection) throws Exception {
		throw new UnsupportedOperationException("Stored procedures not supported");
	}

	/**
	 * Handles function calls.
	 * Override to enable function execution.
	 * @param function the function to execute
	 * @param connection the entity connection
	 * @return the function result
	 * @throws Exception if execution fails
	 */
	default Object handleFunction(Object function, EntityConnection connection) throws Exception {
		throw new UnsupportedOperationException("Functions not supported");
	}

	/**
	 * Handles report generation.
	 * Override to enable report support.
	 * @param reportType the report type
	 * @param connection the entity connection
	 * @return the generated report
	 * @throws Exception if generation fails
	 */
	default Object handleReport(Object reportType, EntityConnection connection) throws Exception {
		throw new UnsupportedOperationException("Reports not supported");
	}
}