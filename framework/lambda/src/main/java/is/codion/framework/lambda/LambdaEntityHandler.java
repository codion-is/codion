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
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse.APIGatewayV2HTTPResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static is.codion.common.rmi.server.Server.CLIENT_HOST;
import static is.codion.framework.db.rmi.RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

/**
 * AWS Lambda handler that implements the Codion entity protocol using EntityServer.
 * <p>
 * This implementation creates a local EntityServer instance and delegates all
 * entity operations to it. The EntityServer handles authentication, connection pooling,
 * transaction management, and all other complex server-side logic.
 * <p>
 * To deploy this handler directly:
 * <pre>
 * aws lambda create-function \
 *   --handler is.codion.framework.lambda.LambdaEntityHandler::handleRequest \
 *   --runtime java21
 * </pre>
 * <p>
 * Configuration:
 * Use JAVA_TOOL_OPTIONS environment variable to set system properties:
 * <pre>
 * JAVA_TOOL_OPTIONS="-Dcodion.db.url=jdbc:postgresql://host/db -Dcodion.server.idleConnectionTimeout=10"
 * </pre>
 */
public class LambdaEntityHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

	private static final Logger LOG = LoggerFactory.getLogger(LambdaEntityHandler.class);

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
	private static final String START_TRANSACTION = "startTransaction";
	private static final String COMMIT_TRANSACTION = "commitTransaction";
	private static final String ROLLBACK_TRANSACTION = "rollbackTransaction";
	private static final String SET_QUERY_CACHE_ENABLED = "setQueryCacheEnabled";
	private static final String IS_QUERY_CACHE_ENABLED = "isQueryCacheEnabled";
	private static final String PROCEDURE = "procedure";
	private static final String FUNCTION = "function";
	private static final String REPORT = "report";
	private static final String ENTITIES_SERIAL = "/entities/serial/";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	private static final String GET_POST_OPTIONS = "GET, POST, OPTIONS";
	private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String X_LAMBDA_REQUEST_ID = "X-Lambda-Request-Id";
	private static final String MISSING_REQUEST_HEADERS = "Missing request headers";
	private static final String ENTITIES = "entities";
	private static final String CLOSE = "close";
	private static final String CLIENT_ID = "clientId";
	private static final String CLIENT_TYPE = "clientType";

	private static final Set<String> NO_BODY_OPERATIONS = unmodifiableSet(new HashSet<>(asList(
					ENTITIES, CLOSE, START_TRANSACTION, COMMIT_TRANSACTION,
					ROLLBACK_TRANSACTION, IS_TRANSACTION_OPEN, IS_QUERY_CACHE_ENABLED)));

	private static final Set<String> NO_RETURN_DATA_OPERATIONS = unmodifiableSet(new HashSet<>(asList(
					CLOSE, START_TRANSACTION, COMMIT_TRANSACTION, ROLLBACK_TRANSACTION)));

	private final EntityServer entityServer;

	/**
	 * Creates a new Lambda handler.
	 */
	public LambdaEntityHandler() {
		try {
			entityServer = EntityServer.startServer(createServerConfiguration());
			LOG.info("EntityServer started successfully");
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to start EntityServer", e);
		}
	}

	/**
	 * Creates the EntityServer configuration.
	 * Override to customize server settings.
	 * @return the server configuration
	 */
	private static EntityServerConfiguration createServerConfiguration() {
		return EntityServerConfiguration
						.builder(1098, 1099) // These are not exposed anywhere
						.database(Database.instance())
						.sslEnabled(false)
						.build();
	}

	@Override
	public final APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
		APIGatewayV2HTTPResponseBuilder response = APIGatewayV2HTTPResponse.builder();
		try {
			String path = extractPath(input);
			// Handle OPTIONS requests for CORS preflight
			if ("OPTIONS".equals(input.getRequestContext().getHttp().getMethod())) {
				handleOptionsRequest(response);
			}
			else if (path.startsWith(ENTITIES_SERIAL)) {
				handleEntityRequest(input, response, context);
			}
			else if ("/entities".equals(path) || "/entities/".equals(path)) {
				handleEntitiesRequest(input, response, context);
			}
			else if ("/health".equals(path) || "/health/".equals(path)) {
				handleHealthCheck(response);
			}
			else {
				sendErrorResponse(response, new IllegalArgumentException("Not found: " + path), context);
			}
		}
		catch (Exception e) {
			sendErrorResponse(response, e, context);
		}

		return response.build();
	}

	private static String extractPath(APIGatewayV2HTTPEvent input) {
		String path = input.getRawPath();

		return path == null || path.isEmpty() ? "/" : path;
	}

	/**
	 * Extracts the client ID from request headers.
	 * @param headers the request headers
	 * @return the client ID
	 * @throws IllegalArgumentException if client ID is missing or invalid
	 */
	private static UUID extractClientId(Map<String, String> headers) {
		if (headers == null) {
			throw new IllegalArgumentException(MISSING_REQUEST_HEADERS);
		}

		// Lambda Function URLs convert all header names to lowercase
		// Try both cases to be safe
		String clientId = headers.get("clientid");
		if (clientId == null) {
			clientId = headers.get(CLIENT_ID);
		}

		if (clientId == null || clientId.trim().isEmpty()) {
			throw new IllegalArgumentException("Missing required header: clientId");
		}

		try {
			return UUID.fromString(clientId.trim());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid clientId format: " + clientId, e);
		}
	}

	/**
	 * Extracts the client type from request headers.
	 * @param headers the request headers
	 * @return the client type
	 * @throws IllegalArgumentException if client type is missing
	 */
	private static String extractClientType(Map<String, String> headers) {
		if (headers == null) {
			throw new IllegalArgumentException(MISSING_REQUEST_HEADERS);
		}

		// Lambda Function URLs convert all header names to lowercase
		// Try both cases to be safe
		String clientType = headers.get("clienttype");
		if (clientType == null) {
			clientType = headers.get(CLIENT_TYPE);
		}

		if (clientType == null || clientType.trim().isEmpty()) {
			throw new IllegalArgumentException("Missing required header: clientType");
		}

		return clientType.trim();
	}

	private static void handleOptionsRequest(APIGatewayV2HTTPResponseBuilder response) {
		response.withStatusCode(200)
						.withHeaders(createOptionsHeaders())
						.withBody("");
	}

	/**
	 * Extracts the domain type name from request headers.
	 * @param headers the request headers
	 * @return the domain type name
	 * @throws IllegalArgumentException if domain type name is missing
	 */
	private static String extractDomainTypeName(Map<String, String> headers) {
		if (headers == null) {
			throw new IllegalArgumentException(MISSING_REQUEST_HEADERS);
		}

		// Lambda Function URLs convert all header names to lowercase
		// Try both cases to be safe
		String domainTypeName = headers.get("domaintypename");
		if (domainTypeName == null) {
			domainTypeName = headers.get("domainTypeName");
		}

		if (domainTypeName == null || domainTypeName.trim().isEmpty()) {
			throw new IllegalArgumentException("Missing required header: domainTypeName");
		}

		return domainTypeName.trim();
	}

	/**
	 * Gets a connection from the EntityServer for the request.
	 * @param user the authenticated user
	 * @param domainTypeName the domain type name
	 * @param clientId the client ID from headers
	 * @param clientType the client type from headers
	 * @return a remote entity connection
	 * @throws Exception if connection fails
	 */
	private RemoteEntityConnection connection(User user, String domainTypeName, UUID clientId, String clientType) throws Exception {
		return (RemoteEntityConnection) entityServer.connect(ConnectionRequest.builder()
						.user(user)
						.clientId(clientId)
						.clientType(clientType)
						.parameter(REMOTE_CLIENT_DOMAIN_TYPE, domainTypeName)
						.parameter(CLIENT_HOST, "lambda")
						.build());
	}

	private void handleEntityRequest(APIGatewayV2HTTPEvent input,
																	 APIGatewayV2HTTPResponseBuilder response,
																	 Context context) throws Exception {
		String path = extractPath(input);
		String operation = null;

		if (path.startsWith(ENTITIES_SERIAL)) {
			operation = path.substring(ENTITIES_SERIAL.length());
		}

		if (operation == null || operation.isEmpty()) {
			sendErrorResponse(response, new IllegalArgumentException("Invalid path - no operation specified"), context);
			return;
		}

		// Extract user, domain, client ID, and client type
		User user = extractUser(input.getHeaders());
		String domainTypeName = extractDomainTypeName(input.getHeaders());
		UUID clientId = extractClientId(input.getHeaders());
		String clientType = extractClientType(input.getHeaders());

		// Operations that don't require a request body
		if (NO_BODY_OPERATIONS.contains(operation)) {
			handleNoBodyOperation(operation, user, domainTypeName, clientId, clientType, response, context);
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
				processOperation(operation, ois, oos, user, domainTypeName, clientId, clientType);
			}

			// Build successful response
			response.withStatusCode(200)
							.withHeaders(createCorsHeadersWithContent(context.getAwsRequestId()))
							.withBody(Base64.getEncoder().encodeToString(baos.toByteArray()))
							.withIsBase64Encoded(true);
		}
	}

	private void processOperation(String operation, ObjectInputStream input, ObjectOutputStream output,
																User user, String domainTypeName, UUID clientId, String clientType) throws Exception {
		RemoteEntityConnection connection = connection(user, domainTypeName, clientId, clientType);
		try {
			switch (operation) {
				case SELECT:
					output.writeObject(connection.select((EntityConnection.Select) input.readObject()));
					break;
				case SELECT_BY_KEY:
					output.writeObject(connection.select((Collection<Entity.Key>) input.readObject()));
					break;
				case COUNT:
					output.writeObject(connection.count((EntityConnection.Count) input.readObject()));
					break;
				case INSERT:
					output.writeObject(connection.insert((Collection<Entity>) input.readObject()));
					break;
				case INSERT_SELECT:
					output.writeObject(connection.insertSelect((Collection<Entity>) input.readObject()));
					break;
				case UPDATE:
					connection.update((Collection<Entity>) input.readObject());
					output.writeObject(null);
					break;
				case UPDATE_SELECT:
					output.writeObject(connection.updateSelect((Collection<Entity>) input.readObject()));
					break;
				case DELETE:
					Object deleteParam = input.readObject();
					if (deleteParam instanceof Collection) {
						connection.delete((Collection<Entity.Key>) deleteParam);
						output.writeObject(null);
					}
					else {
						output.writeObject(connection.delete((Condition) deleteParam));
					}
					break;
				case DELETE_BY_KEY:
					connection.delete((Collection<Entity.Key>) input.readObject());
					output.writeObject(null);
					break;
				case UPDATE_BY_CONDITION:
					output.writeObject(connection.update((EntityConnection.Update) input.readObject()));
					break;
				case VALUES:
					List<Object> params = (List<Object>) input.readObject();
					output.writeObject(connection.select((Column<?>) params.get(0), (EntityConnection.Select) params.get(1)));
					break;
				case DEPENDENCIES:
					output.writeObject(connection.dependencies((Collection<Entity>) input.readObject()));
					break;
				case SET_QUERY_CACHE_ENABLED:
					connection.queryCache((Boolean) input.readObject());
					output.writeObject(null);
					break;
				case PROCEDURE:
					List<Object> parameters = (List<Object>) input.readObject();
					ProcedureType<EntityConnection, Object> procedureType = (ProcedureType<EntityConnection, Object>) parameters.get(0);
					connection.execute(procedureType, parameters.size() > 1 ? parameters.get(1) : null);
					output.writeObject(null);
					break;
				case FUNCTION:
					parameters = (List<Object>) input.readObject();
					FunctionType<EntityConnection, Object, Object> functionType = (FunctionType<EntityConnection, Object, Object>) parameters.get(0);
					output.writeObject(connection.execute(functionType, parameters.size() > 1 ? parameters.get(1) : null));
					break;
				case REPORT:
					parameters = (List<Object>) input.readObject();
					ReportType<Object, Object, Object> reportType = (ReportType<Object, Object, Object>) parameters.get(0);
					output.writeObject(connection.report(reportType, parameters.get(1)));
					break;
				default:
					throw new UnsupportedOperationException("Unknown operation: " + operation);
			}
		}
		finally {
			// Note: EntityServer automatically manages connection lifecycle
			// including transaction state, so we don't close the connection here
		}
	}

	private void handleNoBodyOperation(String operation, User user, String domainTypeName, UUID clientId, String clientType,
																		 APIGatewayV2HTTPResponseBuilder response,
																		 Context context) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			RemoteEntityConnection connection = connection(user, domainTypeName, clientId, clientType);
			switch (operation) {
				case ENTITIES:
					oos.writeObject(connection.entities());
					break;
				case CLOSE:
					connection.close();
					break;
				case START_TRANSACTION:
					connection.startTransaction();
					break;
				case COMMIT_TRANSACTION:
					connection.commitTransaction();
					break;
				case ROLLBACK_TRANSACTION:
					connection.rollbackTransaction();
					break;
				case IS_TRANSACTION_OPEN:
					oos.writeObject(connection.transactionOpen());
					break;
				case IS_QUERY_CACHE_ENABLED:
					oos.writeObject(connection.queryCache());
					break;
				default:
					throw new UnsupportedOperationException("Unknown no-body operation: " + operation);
			}
		}

		// Build response
		if (NO_RETURN_DATA_OPERATIONS.contains(operation)) {
			// These operations return no data, just status
			response.withStatusCode(200)
							.withHeaders(createCorsHeaders());
		}
		else {
			// Operations that return data
			response.withStatusCode(200)
							.withHeaders(createCorsHeadersWithContent(context.getAwsRequestId()))
							.withBody(Base64.getEncoder().encodeToString(baos.toByteArray()))
							.withIsBase64Encoded(true);
		}
	}

	private void handleEntitiesRequest(APIGatewayV2HTTPEvent input,
																		 APIGatewayV2HTTPResponseBuilder response,
																		 Context context) throws Exception {
		// Extract user, domain, client ID, and client type
		User user = extractUser(input.getHeaders());
		String domainTypeName = extractDomainTypeName(input.getHeaders());
		UUID clientId = extractClientId(input.getHeaders());
		String clientType = extractClientType(input.getHeaders());

		// Get connection and entities
		RemoteEntityConnection connection = connection(user, domainTypeName, clientId, clientType);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (ObjectOutputStream stream = new ObjectOutputStream(outputStream)) {
			stream.writeObject(connection.entities());
		}

		response.withStatusCode(200)
						.withHeaders(createCorsHeadersWithContent(context.getAwsRequestId()))
						.withBody(Base64.getEncoder().encodeToString(outputStream.toByteArray()))
						.withIsBase64Encoded(true);
	}

	private void handleHealthCheck(APIGatewayV2HTTPResponseBuilder response) {
		try {
			// Simple health check - just verify server is available
			boolean available = entityServer.connectionsAvailable();
			String status = available ? "UP" : "DOWN";

			response.withStatusCode(200)
							.withHeaders(createJsonHeaders())
							.withBody("{\"status\":\"" + status +
											"\",\"service\":\"codion-lambda\"}");
		}
		catch (Exception e) {
			response.withStatusCode(503)
							.withHeaders(createJsonHeaders())
							.withBody("{\"status\":\"DOWN\",\"error\":\"" + e.getMessage() + "\"}");
		}
	}


	private static void sendErrorResponse(APIGatewayV2HTTPResponseBuilder response,
																				Exception error, Context context) {
		// Special handling for authentication errors
		int statusCode = 500;
		if (error instanceof LoginException || error.getMessage() != null && error.getMessage().contains("authentication")) {
			statusCode = 401;
		}

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try (ObjectOutputStream stream = new ObjectOutputStream(outputStream)) {
				stream.writeObject(error);
			}

			response.withStatusCode(statusCode)
							.withHeaders(createCorsHeadersWithContent(context.getAwsRequestId()))
							.withBody(Base64.getEncoder().encodeToString(outputStream.toByteArray()))
							.withIsBase64Encoded(true);
		}
		catch (Exception e) {
			context.getLogger().log("Failed to serialize error response: " + e.getMessage());
			response.withStatusCode(statusCode)
							.withHeaders(createTextHeaders())
							.withBody("Serialization error");
		}
	}

	private static Map<String, String> createCorsHeaders() {
		Map<String, String> headers = new HashMap<>();
		headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		headers.put(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_OPTIONS);

		return headers;
	}

	private static Map<String, String> createCorsHeadersWithContent(String requestId) {
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
		headers.put(X_LAMBDA_REQUEST_ID, requestId);
		headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		headers.put(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_OPTIONS);

		return headers;
	}

	private static Map<String, String> createJsonHeaders() {
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE, "application/json");

		return headers;
	}

	private static Map<String, String> createTextHeaders() {
		Map<String, String> headers = new HashMap<>();
		headers.put(CONTENT_TYPE, "text/plain");
		headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		headers.put(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_OPTIONS);

		return headers;
	}

	private static Map<String, String> createOptionsHeaders() {
		Map<String, String> headers = new HashMap<>();
		headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		headers.put(ACCESS_CONTROL_ALLOW_METHODS, GET_POST_OPTIONS);
		headers.put(ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, content-type, domainTypeName, domaintypename, clientId, clientid, clientType, clienttype, Authorization, authorization");
		headers.put(ACCESS_CONTROL_MAX_AGE, "86400");

		return headers;
	}

	/**
	 * Extracts user from request headers using Basic authentication.
	 * @param headers the request headers
	 * @return the authenticated user
	 * @throws IllegalArgumentException if no authentication is provided or if authentication is invalid
	 */
	private static User extractUser(Map<String, String> headers) {
		if (headers == null) {
			throw new IllegalArgumentException("No authentication provided - missing headers");
		}

		// Check for Basic auth header (Lambda Function URLs lowercase headers)
		String auth = headers.get("authorization");
		if (auth == null) {
			auth = headers.get("Authorization");
		}
		if (auth == null || !auth.startsWith("Basic ")) {
			throw new IllegalArgumentException("No authentication provided - missing Authorization header");
		}
		try {
			String encoded = auth.substring(6);
			byte[] decoded = java.util.Base64.getDecoder().decode(encoded);
			return User.parse(new String(decoded));
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid Basic authentication format", e);
		}
	}
}