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
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * Abstract base class for AWS Lambda handlers that implement the Codion entity protocol.
 * Handles the protocol details and delegates customization points to implementing classes.
 * <p>
 * This implementation uses Codion's connection pooling framework to manage database
 * connections efficiently across Lambda invocations. Connections are pooled per user
 * and reused between invocations as long as the Lambda container remains warm.
 * <p>
 * To create a Lambda handler for your application:
 * <pre>
 * public class MyAppLambdaHandler extends AbstractLambdaEntityHandler {
 *     public MyAppLambdaHandler() {
 *         super(new MyDomainImpl());
 *     }
 * }
 * </pre>
 * <p>
 * Connection pool configuration can be controlled via environment variables:
 * <ul>
 *   <li>CONNECTION_POOL_SIZE - Maximum pool size (default: 5)</li>
 *   <li>CONNECTION_POOL_TIMEOUT - Idle timeout in seconds (default: 30)</li>
 * </ul>
 */
public abstract class AbstractLambdaEntityHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>, LambdaEntityHandler {

	// Protocol operation constants
	private static final String SELECT = "select";
	private static final String SELECT_BY_KEY = "selectByKey";
	private static final String COUNT = "count";
	private static final String INSERT = "insert";
	private static final String INSERT_SELECT = "insertSelect";
	private static final String UPDATE = "update";
	private static final String UPDATE_SELECT = "updateSelect";
	private static final String UPDATE_BY_CONDITION = "updateByCondition";
	private static final String DELETE = "delete";
	private static final String DELETE_BY_KEY = "deleteByKey";
	private static final String VALUES = "values";
	private static final String DEPENDENCIES = "dependencies";
	private static final String IS_TRANSACTION_OPEN = "isTransactionOpen";
	private static final String SET_QUERY_CACHE_ENABLED = "setQueryCacheEnabled";
	private static final String IS_QUERY_CACHE_ENABLED = "isQueryCacheEnabled";
	private static final String EXECUTE = "execute";
	private static final String PROCEDURE = "procedure";
	private static final String FUNCTION = "function";
	private static final String REPORT = "report";

	private final Domain domain;
	private final LambdaConfiguration configuration;
	private final Database database;
	private final ConnectionPoolWrapper connectionPool;
	
	// Cache connection pools per user for multi-tenant scenarios
	private final Map<String, ConnectionPoolWrapper> userPools = new ConcurrentHashMap<>();

	/**
	 * Creates a new Lambda handler for the given domain.
	 * @param domain the domain to serve
	 */
	protected AbstractLambdaEntityHandler(Domain domain) {
		this(domain, LambdaConfiguration.create());
	}

	/**
	 * Creates a new Lambda handler with custom configuration.
	 * @param domain the domain to serve
	 * @param configuration the configuration to use
	 */
	protected AbstractLambdaEntityHandler(Domain domain, LambdaConfiguration configuration) {
		this.domain = requireNonNull(domain);
		this.configuration = requireNonNull(configuration);
		this.database = initializeDatabase();
		
		// Create main connection pool with default user
		this.connectionPool = createConnectionPool(configuration.defaultUser());
		
		// Initialize database on cold start
		try {
			initializeDatabaseSchema();
		}
		catch (Exception e) {
			System.err.println("Failed to initialize database: " + e.getMessage());
		}
	}

	@Override
	public final Domain domain() {
		return domain;
	}

	@Override
	public final LambdaConfiguration configuration() {
		return configuration;
	}

	@Override
	public final APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

		try {
			String path = extractPath(input);
			Map<String, String> headers = input.getHeaders();
			
			// Pre-request hook
			beforeRequest(path, headers, context);
			
			try {
				if (path.startsWith("/entities/serial/")) {
					handleEntityRequest(input, response, context);
				}
				else if (path.equals("/entities") || path.equals("/entities/")) {
					handleEntitiesRequest(response, context);
				}
				else if (path.equals("/health") || path.equals("/health/")) {
					handleHealthCheck(response);
				}
				else {
					sendErrorResponse(response, new IllegalArgumentException("Not found: " + path), context);
				}
				
				// Post-request hook on success
				afterRequest(path, headers, context);
			}
			catch (Exception e) {
				// Error hook
				onError(path, headers, e, context);
				throw e;
			}
		}
		catch (Exception e) {
			sendErrorResponse(response, e, context);
		}

		return response;
	}

	@Override
	public Connection createConnection(User user) throws SQLException {
		try {
			// For the default user, use the main pool
			if (user.equals(configuration.defaultUser())) {
				return connectionPool.connection(user);
			}
			
			// For other users, create or get a dedicated pool
			ConnectionPoolWrapper pool = userPools.computeIfAbsent(user.username(), 
				username -> createConnectionPool(user));
			
			return pool.connection(user);
		}
		catch (DatabaseException e) {
			throw new SQLException("Failed to get connection from pool", e);
		}
	}

	/**
	 * Creates a connection pool for the given user.
	 * Override to customize pool creation.
	 * @param user the user to create a pool for
	 * @return a new connection pool
	 */
	protected ConnectionPoolWrapper createConnectionPool(User user) {
		ConnectionPoolWrapper pool = database.createConnectionPool(
			ConnectionPoolFactory.instance(), user);
		
		// Configure pool settings
		pool.setMaximumPoolSize(configuration.connectionPoolSize());
		pool.setIdleTimeout(configuration.connectionPoolTimeout());
		pool.setCollectSnapshotStatistics(true);
		pool.setCollectCheckOutTimes(true);
		
		return pool;
	}

	/**
	 * Initializes the database.
	 * Override to customize database initialization.
	 * @return the initialized database
	 */
	protected Database initializeDatabase() {
		// Set system properties for Database.instance()
		System.setProperty("codion.db.url", configuration.databaseUrl());
		System.setProperty("codion.db.username", configuration.databaseUser());
		System.setProperty("codion.db.password", configuration.databasePassword());
		
		configuration.databaseInitScripts().ifPresent(scripts ->
						System.setProperty("codion.db.initScripts", scripts));
		
		return Database.instance();
	}

	/**
	 * Initializes the database schema.
	 * Override to customize schema initialization.
	 * @throws Exception if initialization fails
	 */
	protected void initializeDatabaseSchema() throws Exception {
		try (Connection conn = connectionPool.connection(configuration.defaultUser())) {
			System.out.println("Database initialized: " + conn.getMetaData().getURL());
			System.out.println("Connection pool created with size: " + configuration.connectionPoolSize());
		}
	}

	private String extractPath(APIGatewayProxyRequestEvent input) {
		String path = input.getPath();
		
		// Check for path in headers (Lambda Function URLs)
		if (input.getHeaders() != null && input.getHeaders().containsKey("x-forwarded-path")) {
			path = input.getHeaders().get("x-forwarded-path");
		}
		
		return path == null || path.isEmpty() ? "/" : path;
	}

	private void handleEntityRequest(APIGatewayProxyRequestEvent input,
																	 APIGatewayProxyResponseEvent response,
																	 Context context) throws Exception {
		String path = extractPath(input);
		String operation = null;
		
		if (path.startsWith("/entities/serial/")) {
			operation = path.substring("/entities/serial/".length());
		}
		
		if (operation == null || operation.isEmpty()) {
			sendErrorResponse(response, new IllegalArgumentException("Invalid path - no operation specified"), context);
			return;
		}

		User user = extractUser(input.getHeaders());

		// Operations that don't require a request body
		if ("entities".equals(operation) || "close".equals(operation)) {
			handleNoBodyOperation(operation, response, context);
			return;
		}

		// For other operations, we need a request body
		if (input.getBody() == null || input.getBody().isEmpty()) {
			sendErrorResponse(response, new IllegalArgumentException("Empty request body for operation: " + operation), context);
			return;
		}

		// Process the operation
		byte[] requestBytes = Base64.getDecoder().decode(input.getBody());
		
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(requestBytes))) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
				processOperation(operation, ois, oos, user);
			}
			
			// Build successful response
			response.setStatusCode(200);
			response.setHeaders(Map.of(
							"Content-Type", "application/octet-stream",
							"X-Lambda-Request-Id", context.getAwsRequestId()
			));
			response.setBody(Base64.getEncoder().encodeToString(baos.toByteArray()));
			response.setIsBase64Encoded(true);
		}
	}

	private void handleNoBodyOperation(String operation, APIGatewayProxyResponseEvent response, Context context) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			if ("entities".equals(operation)) {
				oos.writeObject(domain.entities());
			}
			else if ("close".equals(operation)) {
				oos.writeObject(null);
			}
		}
		
		response.setStatusCode(200);
		response.setHeaders(Map.of(
						"Content-Type", "application/octet-stream",
						"X-Lambda-Request-Id", context.getAwsRequestId(),
						"Access-Control-Allow-Origin", "*",
						"Access-Control-Allow-Methods", "GET, POST, OPTIONS"
		));
		response.setBody(Base64.getEncoder().encodeToString(baos.toByteArray()));
		response.setIsBase64Encoded(true);
	}

	private void processOperation(String operation, ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		switch (operation) {
			case SELECT:
				handleSelect(ois, oos, user);
				break;
			case SELECT_BY_KEY:
				handleSelectByKey(ois, oos, user);
				break;
			case COUNT:
				handleCount(ois, oos, user);
				break;
			case INSERT:
				handleInsert(ois, oos, user);
				break;
			case INSERT_SELECT:
				handleInsertSelect(ois, oos, user);
				break;
			case UPDATE:
				handleUpdate(ois, oos, user);
				break;
			case UPDATE_SELECT:
				handleUpdateSelect(ois, oos, user);
				break;
			case DELETE:
				handleDelete(ois, oos, user);
				break;
			case DELETE_BY_KEY:
				handleDeleteByKey(ois, oos, user);
				break;
			case UPDATE_BY_CONDITION:
				handleUpdateByCondition(ois, oos, user);
				break;
			case VALUES:
				handleValues(ois, oos, user);
				break;
			case DEPENDENCIES:
				handleDependencies(ois, oos, user);
				break;
			case IS_TRANSACTION_OPEN:
				handleIsTransactionOpen(ois, oos, user);
				break;
			case SET_QUERY_CACHE_ENABLED:
				handleSetQueryCacheEnabled(ois, oos, user);
				break;
			case IS_QUERY_CACHE_ENABLED:
				handleIsQueryCacheEnabled(ois, oos, user);
				break;
			case EXECUTE:
				handleExecuteOperation(ois, oos, user);
				break;
			case PROCEDURE:
				handleProcedureOperation(ois, oos, user);
				break;
			case FUNCTION:
				handleFunctionOperation(ois, oos, user);
				break;
			case REPORT:
				handleReportOperation(ois, oos, user);
				break;
			default:
				throw new UnsupportedOperationException("Unknown operation: " + operation);
		}
	}

	private void handleSelect(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		EntityConnection.Select select = (EntityConnection.Select) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			Collection<Entity> result = connection.select(select);
			oos.writeObject(result);
		}
	}

	private void handleSelectByKey(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Entity.Key> keys = (Collection<Entity.Key>) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			Collection<Entity> result = connection.select(keys);
			oos.writeObject(result);
		}
	}

	private void handleCount(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		EntityConnection.Count count = (EntityConnection.Count) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			int result = connection.count(count);
			oos.writeObject(result);
		}
	}

	private void handleInsert(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Entity> entities = (Collection<Entity>) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			Collection<Entity.Key> keys = connection.insert(entities);
			oos.writeObject(keys);
		}
	}

	private void handleInsertSelect(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Entity> entities = (Collection<Entity>) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			Collection<Entity> inserted = connection.insertSelect(entities);
			oos.writeObject(inserted);
		}
	}

	private void handleUpdate(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Entity> entities = (Collection<Entity>) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			connection.update(entities);
			oos.writeObject(null);
		}
	}

	private void handleUpdateSelect(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Entity> entities = (Collection<Entity>) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			Collection<Entity> updated = connection.updateSelect(entities);
			oos.writeObject(updated);
		}
	}

	private void handleDelete(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		Object deleteParam = ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			if (deleteParam instanceof Collection) {
				@SuppressWarnings("unchecked")
				Collection<Entity.Key> keys = (Collection<Entity.Key>) deleteParam;
				connection.delete(keys);
				oos.writeObject(null);
			}
			else {
				Condition condition = (Condition) deleteParam;
				int count = connection.delete(condition);
				oos.writeObject(count);
			}
		}
	}

	private void handleDeleteByKey(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Entity.Key> keys = (Collection<Entity.Key>) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			connection.delete(keys);
			oos.writeObject(null);
		}
	}

	private void handleUpdateByCondition(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		EntityConnection.Update update = (EntityConnection.Update) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			int result = connection.update(update);
			oos.writeObject(result);
		}
	}

	private void handleValues(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		@SuppressWarnings("unchecked")
		List<Object> params = (List<Object>) ois.readObject();
		Column<?> column = (Column<?>) params.get(0);
		EntityConnection.Select select = (EntityConnection.Select) params.get(1);
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			List<?> result = connection.select(column, select);
			oos.writeObject(result);
		}
	}

	private void handleDependencies(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Entity> entities = (Collection<Entity>) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			var result = connection.dependencies(entities);
			oos.writeObject(result);
		}
	}

	private void handleIsTransactionOpen(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			boolean result = connection.transactionOpen();
			oos.writeObject(result);
		}
	}

	private void handleSetQueryCacheEnabled(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		Boolean enabled = (Boolean) ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			connection.queryCache(enabled);
			oos.writeObject(null);
		}
	}

	private void handleIsQueryCacheEnabled(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			boolean result = connection.queryCache();
			oos.writeObject(result);
		}
	}

	private void handleExecuteOperation(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		Object executable = ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			Object result = handleExecute(executable, connection);
			oos.writeObject(result);
		}
	}

	private void handleProcedureOperation(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		String procedureName = (String) ois.readObject();
		Object arguments = ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			handleProcedure(procedureName, arguments, connection);
			oos.writeObject(null);
		}
	}

	private void handleFunctionOperation(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		Object function = ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			Object result = handleFunction(function, connection);
			oos.writeObject(result);
		}
	}

	private void handleReportOperation(ObjectInputStream ois, ObjectOutputStream oos, User user) throws Exception {
		Object reportType = ois.readObject();
		
		try (Connection jdbcConnection = createConnection(user);
				 LocalEntityConnection connection = wrapConnection(jdbcConnection, user)) {
			Object report = handleReport(reportType, connection);
			oos.writeObject(report);
		}
	}

	private void handleHealthCheck(APIGatewayProxyResponseEvent response) {
		boolean healthy = false;
		String poolStatus = "";
		
		try (Connection conn = connectionPool.connection(configuration.defaultUser())) {
			healthy = conn.isValid(5);
			// Get current pool statistics
			var stats = connectionPool.statistics(System.currentTimeMillis());
			poolStatus = ", \"pool_size\":" + stats.size() + 
			            ", \"pool_in_use\":" + stats.inUse() +
			            ", \"pool_available\":" + (stats.size() - stats.inUse());
		}
		catch (Exception e) {
			// Connection failed
		}
		
		response.setStatusCode(200);
		response.setHeaders(Map.of("Content-Type", "application/json"));
		response.setBody("{\"status\":\"" + (healthy ? "UP" : "DOWN") + 
		                "\",\"service\":\"codion-lambda\",\"domain\":\"" + 
		                domain.type().name() + "\"" + poolStatus + "}");
	}

	private void handleEntitiesRequest(APIGatewayProxyResponseEvent response, Context context) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(domain.entities());
		}
		
		response.setStatusCode(200);
		response.setHeaders(Map.of(
						"Content-Type", "application/octet-stream",
						"X-Lambda-Request-Id", context.getAwsRequestId(),
						"Access-Control-Allow-Origin", "*",
						"Access-Control-Allow-Methods", "GET, POST, OPTIONS"
		));
		response.setBody(Base64.getEncoder().encodeToString(baos.toByteArray()));
		response.setIsBase64Encoded(true);
	}

	/**
	 * Gets connection pool statistics for monitoring.
	 * @return the main connection pool statistics
	 */
	protected ConnectionPoolWrapper getConnectionPool() {
		return connectionPool;
	}
	
	/**
	 * Gets connection pool statistics for a specific user.
	 * @param username the username  
	 * @return the user's connection pool, or null if not found
	 */
	protected ConnectionPoolWrapper getUserConnectionPool(String username) {
		return userPools.get(username);
	}
	
	/**
	 * Closes all connection pools. Called when Lambda container is being terminated.
	 * Note: Lambda doesn't guarantee this will be called, but it's good practice.
	 */
	protected void shutdown() {
		try {
			connectionPool.close();
			userPools.values().forEach(pool -> {
				try {
					pool.close();
				}
				catch (Exception e) {
					System.err.println("Failed to close user pool: " + e.getMessage());
				}
			});
			userPools.clear();
		}
		catch (Exception e) {
			System.err.println("Failed to close main connection pool: " + e.getMessage());
		}
	}
	
	private void sendErrorResponse(APIGatewayProxyResponseEvent response, Exception error, Context context) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
				oos.writeObject(error);
			}
			
			response.setStatusCode(500);
			response.setHeaders(Map.of(
							"Content-Type", "application/octet-stream",
							"X-Lambda-Request-Id", context.getAwsRequestId()
			));
			response.setBody(Base64.getEncoder().encodeToString(baos.toByteArray()));
			response.setIsBase64Encoded(true);
		}
		catch (Exception e) {
			context.getLogger().log("Failed to serialize error response: " + e.getMessage());
			response.setStatusCode(500);
			response.setBody("Serialization error");
		}
	}
}