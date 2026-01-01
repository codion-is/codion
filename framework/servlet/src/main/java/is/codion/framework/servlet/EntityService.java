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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.servlet;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.AuxiliaryServer;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.rmi.server.exception.ServerException;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.db.DatabaseObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapperFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.community.ssl.SslConfig;
import io.javalin.community.ssl.SslPlugin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static is.codion.common.db.operation.FunctionType.functionType;
import static is.codion.common.db.operation.ProcedureType.procedureType;
import static is.codion.common.db.report.ReportType.reportType;
import static is.codion.common.utilities.Configuration.*;
import static is.codion.common.utilities.Serializer.serialize;
import static is.codion.common.utilities.Text.nullOrEmpty;
import static is.codion.framework.json.domain.EntityObjectMapper.ENTITY_LIST_REFERENCE;
import static is.codion.framework.json.domain.EntityObjectMapper.KEY_LIST_REFERENCE;
import static java.util.Objects.requireNonNull;

/**
 * An {@link AuxiliaryServer} implementation providing HTTP based entity services.
 */
public final class EntityService implements AuxiliaryServer {

	private static final Logger LOG = LoggerFactory.getLogger(EntityService.class);

	/**
	 * The port on which the http server is made available to clients.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 8080
	 * </ul>
	 */
	public static final PropertyValue<Integer> PORT = integerValue("codion.server.http.port", 8080);

	/**
	 * The port on which the http server is made available to clients.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 4443
	 * </ul>
	 */
	public static final PropertyValue<Integer> SECURE_PORT = integerValue("codion.server.http.securePort", 4443);

	/**
	 * Specifies whether https should be used.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> SECURE = booleanValue("codion.server.http.secure", true);

	/**
	 * The https keystore to use on the classpath, this will be resolved to a temporary file and set
	 * as the codion.server.http.keyStore system property on server start
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	public static final PropertyValue<String> CLASSPATH_KEYSTORE = stringValue("codion.server.http.classpathKeyStore");

	/**
	 * Specifies the keystore to use for securing http connections.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 * @see #CLASSPATH_KEYSTORE
	 */
	public static final PropertyValue<String> KEYSTORE_PATH = stringValue("codion.server.http.keyStore");

	/**
	 * Specifies the password for the keystore used for securing http connections.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	public static final PropertyValue<String> KEYSTORE_PASSWORD =
					stringValue("codion.server.http.keyStorePassword");

	/**
	 * Specifies whether virtual threads should be used.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> USE_VIRTUAL_THREADS =
					booleanValue("codion.server.http.useVirtualThreads", false);

	/**
	 * Specifies whether java serialization based services should be enabled.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> SERALIZATION = booleanValue("codion.server.http.serialization", false);

	/**
	 * Specifies whether json based services should be enabled.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> JSON = booleanValue("codion.server.http.json", true);

	static final String DOMAIN_TYPE_NAME = "domainTypeName";
	static final String CLIENT_TYPE = "clientType";
	static final String CLIENT_ID = "clientId";
	static final String CLIENT_VERSION = "clientVersion";

	private static final String AUTHORIZATION = "Authorization";
	private static final String BASIC_PREFIX = "basic ";
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final int BASIC_PREFIX_LENGTH = BASIC_PREFIX.length();

	private static final String URL_SERIAL = "entities/serial/";
	private static final String URL_JSON = "entities/json/";
	private static final String PARAMETER = "parameter";

	private final EntitiesHandler entitiesHandler = new EntitiesHandler();
	private final CloseHandler closeHandler = new CloseHandler();
	private final StartTransactionHandler startTransactionHandler = new StartTransactionHandler();
	private final CommitTransactionHandler commitTransactionHandler = new CommitTransactionHandler();
	private final RollbackTransactionHandler rollbackTransactionHandler = new RollbackTransactionHandler();
	private final IsTransactionOpenHandler isTransactionOpenHandler = new IsTransactionOpenHandler();
	private final IsQueryCacheEnabledHandler isQueryCacheEnabledHandler = new IsQueryCacheEnabledHandler();
	private final SetQueryCacheEnabledHandler setQueryCacheEnabledHandler = new SetQueryCacheEnabledHandler();
	private final SelectHandler selectHandler = new SelectHandler();
	private final SelectByKeyHandler selectByKeyHandler = new SelectByKeyHandler();
	private final InsertHandler insertHandler = new InsertHandler();
	private final InsertSelectHandler insertSelectHandler = new InsertSelectHandler();
	private final UpdateHandler updateHandler = new UpdateHandler();
	private final UpdateSelectHandler updateSelectHandler = new UpdateSelectHandler();
	private final UpdateByConditionHandler updateByConditionHandler = new UpdateByConditionHandler();
	private final DeleteHandler deleteHandler = new DeleteHandler();
	private final DeleteByKeyHandler deleteByKeyHandler = new DeleteByKeyHandler();
	private final ValuesHandler valuesHandler = new ValuesHandler();
	private final CountHandler countHandler = new CountHandler();
	private final DependenciesHandler dependenciesHandler = new DependenciesHandler();
	private final ProcedureHandler procedureHandler = new ProcedureHandler();
	private final FunctionHandler functionHandler = new FunctionHandler();
	private final ReportHandler reportHandler = new ReportHandler();

	private final Server<RemoteEntityConnection, ? extends ServerAdmin> server;
	private final Javalin javalin;
	private final int port;
	private final int securePort;
	private final boolean serialization = SERALIZATION.getOrThrow();
	private final boolean json = JSON.getOrThrow();
	private final boolean sslEnabled;
	private final boolean useVirtualThreads;

	private final Map<DomainType, ObjectMapper> domainObjectMappers = new ConcurrentHashMap<>();

	static {
		resolveClasspathKeyStore();
	}

	EntityService(Server<RemoteEntityConnection, ? extends ServerAdmin> server) {
		this.server = requireNonNull(server);
		this.port = PORT.getOrThrow();
		this.securePort = SECURE_PORT.getOrThrow();
		this.sslEnabled = SECURE.getOrThrow();
		this.useVirtualThreads = USE_VIRTUAL_THREADS.getOrThrow();
		this.javalin = Javalin.create(new JavalinConfigurer());
	}

	@Override
	public void start() {
		if (serialization) {
			addSerializationHandlers();
		}
		if (json) {
			addJsonHandlers();
		}
		javalin.start(sslEnabled ? securePort : port);
	}

	@Override
	public void stop() {
		if (javalin != null) {
			javalin.stop();
		}
	}

	@Override
	public String information() {
		return "Entity Service " + Version.version()
						+ " started on port: " + port
						+ ", securePort: " + securePort
						+ ", sslEnabled: " + sslEnabled;
	}

	private void addSerializationHandlers() {
		javalin.post(URL_SERIAL + "entities", entitiesHandler::serial);
		javalin.post(URL_SERIAL + "close", closeHandler::serial);
		javalin.post(URL_SERIAL + "isTransactionOpen", isTransactionOpenHandler::serial);
		javalin.post(URL_SERIAL + "startTransaction", startTransactionHandler::serial);
		javalin.post(URL_SERIAL + "rollbackTransaction", rollbackTransactionHandler::serial);
		javalin.post(URL_SERIAL + "commitTransaction", commitTransactionHandler::serial);
		javalin.post(URL_SERIAL + "isQueryCacheEnabled", isQueryCacheEnabledHandler::serial);
		javalin.post(URL_SERIAL + "setQueryCacheEnabled", setQueryCacheEnabledHandler::serial);
		javalin.post(URL_SERIAL + "procedure", procedureHandler::serial);
		javalin.post(URL_SERIAL + "function", functionHandler::serial);
		javalin.post(URL_SERIAL + "report", reportHandler::serial);
		javalin.post(URL_SERIAL + "dependencies", dependenciesHandler::serial);
		javalin.post(URL_SERIAL + "count", countHandler::serial);
		javalin.post(URL_SERIAL + "values", valuesHandler::serial);
		javalin.post(URL_SERIAL + "selectByKey", selectByKeyHandler::serial);
		javalin.post(URL_SERIAL + "select", selectHandler::serial);
		javalin.post(URL_SERIAL + "insert", insertHandler::serial);
		javalin.post(URL_SERIAL + "insertSelect", insertSelectHandler::serial);
		javalin.post(URL_SERIAL + "update", updateHandler::serial);
		javalin.post(URL_SERIAL + "updateSelect", updateSelectHandler::serial);
		javalin.post(URL_SERIAL + "updateByCondition", updateByConditionHandler::serial);
		javalin.post(URL_SERIAL + "delete", deleteHandler::serial);
		javalin.post(URL_SERIAL + "deleteByKey", deleteByKeyHandler::serial);
	}

	private void addJsonHandlers() {
		// Note: Some methods (for example those without parameters and return values)
		// use the java serialization handler, so these are not just some glaring
		// mistakes below, where a ::serial call is associated with the json url
		javalin.post(URL_JSON + "entities", entitiesHandler::serial);
		javalin.post(URL_JSON + "close", closeHandler::serial);
		javalin.post(URL_JSON + "isTransactionOpen", isTransactionOpenHandler::json);
		javalin.post(URL_JSON + "startTransaction", startTransactionHandler::serial);
		javalin.post(URL_JSON + "rollbackTransaction", rollbackTransactionHandler::serial);
		javalin.post(URL_JSON + "commitTransaction", commitTransactionHandler::serial);
		javalin.post(URL_JSON + "isQueryCacheEnabled", isQueryCacheEnabledHandler::json);
		javalin.post(URL_JSON + "setQueryCacheEnabled", setQueryCacheEnabledHandler::json);
		javalin.post(URL_JSON + "procedure", procedureHandler::json);
		javalin.post(URL_JSON + "function", functionHandler::json);
		javalin.post(URL_JSON + "report", reportHandler::json);
		javalin.post(URL_JSON + "dependencies", dependenciesHandler::json);
		javalin.post(URL_JSON + "count", countHandler::json);
		javalin.post(URL_JSON + "values", valuesHandler::json);
		javalin.post(URL_JSON + "selectByKey", selectByKeyHandler::json);
		javalin.post(URL_JSON + "select", selectHandler::json);
		javalin.post(URL_JSON + "insert", insertHandler::json);
		javalin.post(URL_JSON + "insertSelect", insertSelectHandler::json);
		javalin.post(URL_JSON + "update", updateHandler::json);
		javalin.post(URL_JSON + "updateSelect", updateSelectHandler::json);
		javalin.post(URL_JSON + "updateByCondition", updateByConditionHandler::json);
		javalin.post(URL_JSON + "delete", deleteHandler::json);
		javalin.post(URL_JSON + "deleteByKey", deleteByKeyHandler::json);
	}

	private final class EntitiesHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				context.status(HttpStatus.OK_200)
								.result(serialize(connection.entities()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class CloseHandler {

		private void serial(Context context) {
			try {
				authenticate(context).close();
				context.req().getSession().invalidate();
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class IsTransactionOpenHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(connection.transactionOpen()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(connection.transactionOpen()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class StartTransactionHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				connection.startTransaction();
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class RollbackTransactionHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				connection.rollbackTransaction();
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class CommitTransactionHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				connection.commitTransaction();
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class IsQueryCacheEnabledHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(connection.queryCache()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(connection.queryCache()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class SetQueryCacheEnabledHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				connection.queryCache(deserialize(context.req()));
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				connection.queryCache(objectMapper.readValue(context.req().getInputStream(), Boolean.class));
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class ProcedureHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Object> parameters = deserialize(context.req());
				Object parameter = parameters.size() > 1 ? parameters.get(1) : null;
				connection.execute((ProcedureType<EntityConnection, Object>) parameters.get(0), parameter);
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				DatabaseObjectMapper objectMapper = (DatabaseObjectMapper) objectMapper(connection.entities());

				JsonNode requestNode = objectMapper.readTree(context.req().getInputStream());
				ProcedureType<EntityConnection, Object> procedureType = procedureType(requestNode.get("procedureType").asText());
				Object parameter = null;
				JsonNode parameterNode = requestNode.get(PARAMETER);
				if (parameterNode != null) {
					Class<?> parameterType = objectMapper.entityObjectMapper().parameter(procedureType).get();
					parameter = objectMapper.readValue(parameterNode.toString(), parameterType);
				}

				connection.execute(procedureType, parameter);

				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class FunctionHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Object> parameters = deserialize(context.req());
				FunctionType<EntityConnection, Object, Object> functionType =
								(FunctionType<EntityConnection, Object, Object>) parameters.get(0);
				Object parameter = parameters.size() > 1 ? parameters.get(1) : null;
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(connection.execute(functionType, parameter)));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				DatabaseObjectMapper objectMapper = (DatabaseObjectMapper) objectMapper(connection.entities());

				JsonNode requestNode = objectMapper.readTree(context.req().getInputStream());
				FunctionType<EntityConnection, Object, Object> functionType = functionType(requestNode.get("functionType").asText());
				Object parameter = null;
				JsonNode parameterNode = requestNode.get(PARAMETER);
				if (parameterNode != null) {
					Class<?> parameterType = objectMapper.entityObjectMapper().parameter(functionType).get();
					parameter = objectMapper.readValue(parameterNode.toString(), parameterType);
				}

				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(connection.execute(functionType, parameter)));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class ReportHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Object> parameters = deserialize(context.req());
				ReportType<?, ?, Object> reportType = (ReportType<?, ?, Object>) parameters.get(0);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(connection.report(reportType, parameters.get(1))));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				DatabaseObjectMapper objectMapper = (DatabaseObjectMapper) objectMapper(connection.entities());

				JsonNode requestNode = objectMapper.readTree(context.req().getInputStream());
				ReportType<Object, Object, Object> reportType = reportType(requestNode.get("reportType").asText());
				Object parameter = null;
				JsonNode parameterNode = requestNode.get(PARAMETER);
				if (parameterNode != null) {
					Class<?> parameterType = objectMapper.entityObjectMapper().parameter(reportType).get();
					parameter = objectMapper.readValue(parameterNode.toString(), parameterType);
				}

				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(connection.report(reportType, parameter)));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class DependenciesHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(connection.dependencies(deserialize(context.req()))));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				List<Entity> entities = objectMapper.readValue(context.req().getInputStream(), ENTITY_LIST_REFERENCE);
				Map<EntityType, Collection<Entity>> dependencies = connection.dependencies(entities);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(dependencies));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class CountHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				int rowCount = connection.count(deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(rowCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				int rowCount = connection.count(objectMapper.readValue(context.req().getInputStream(), Count.class));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(rowCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class ValuesHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Object> parameters = deserialize(context.req());
				List<?> values = connection.select((Column<?>) parameters.get(0), (Select) parameters.get(1));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(values));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Entities entities = connection.entities();
				ObjectMapper objectMapper = objectMapper(entities);
				JsonNode jsonNode = objectMapper.readTree(context.req().getInputStream());
				EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
				Column<?> column = (Column<?>) entities.definition(entityType).attributes().getOrThrow(jsonNode.get("column").textValue());
				Select select = null;
				JsonNode conditionNode = jsonNode.get("condition");
				if (conditionNode != null) {
					select = objectMapper.readValue(conditionNode.toString(), Select.class);
				}
				List<?> values = connection.select(column, select);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(values));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class SelectByKeyHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Entity.Key> keys = deserialize(context.req());
				Collection<Entity> selected = connection.select(keys);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(selected));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				List<Entity.Key> keys = objectMapper.readValue(context.req().getInputStream(), KEY_LIST_REFERENCE);
				Collection<Entity> selected = connection.select(keys);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(selected));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class SelectHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Select select = deserialize(context.req());
				List<Entity> selected = connection.select(select);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(selected));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				Select selectJson = objectMapper.readValue(context.req().getInputStream(), Select.class);
				List<Entity> selected = connection.select(selectJson);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(selected));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class InsertHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Collection<Entity.Key> keys = connection.insert((Collection<Entity>) deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(keys));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				Collection<Entity> entities = objectMapper.readValue(context.req().getInputStream(), ENTITY_LIST_REFERENCE);
				Collection<Entity.Key> keys = connection.insert(entities);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(keys));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class InsertSelectHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Collection<Entity> entities = connection.insertSelect((Collection<Entity>) deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(entities));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				Collection<Entity> entities = objectMapper.readValue(context.req().getInputStream(), ENTITY_LIST_REFERENCE);
				Collection<Entity> inserted = connection.insertSelect(entities);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(inserted));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class UpdateHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				connection.update((List<Entity>) deserialize(context.req()));
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				List<Entity> entities = objectMapper.readValue(context.req().getInputStream(), ENTITY_LIST_REFERENCE);
				connection.update(entities);
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class UpdateSelectHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Collection<Entity> updated = connection.updateSelect((Collection<Entity>) deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(updated));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				List<Entity> entities = objectMapper.readValue(context.req().getInputStream(), ENTITY_LIST_REFERENCE);
				Collection<Entity> updated = connection.updateSelect(entities);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(updated));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class UpdateByConditionHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				int updateCount = connection.update((Update) deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(updateCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				Update update = objectMapper.readValue(context.req().getInputStream(), Update.class);
				int updateCount = connection.update(update);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(updateCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class DeleteHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Condition condition = deserialize(context.req());
				int deleteCount = connection.delete(condition);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(deleteCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				Condition deleteCondition = objectMapper.readValue(context.req().getInputStream(), Condition.class);
				int deleteCount = connection.delete(deleteCondition);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(deleteCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class DeleteByKeyHandler {

		private void serial(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Collection<Entity.Key> keys = deserialize(context.req());
				connection.delete(keys);
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}

		private void json(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				List<Entity.Key> keys = objectMapper.readValue(context.req().getInputStream(), KEY_LIST_REFERENCE);
				connection.delete(keys);
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private RemoteEntityConnection authenticate(Context context) throws RemoteException, ServerException {
		if (server == null) {
			throw new IllegalStateException("EntityServer has not been set for EntityService");
		}

		String domainTypeName = domainTypeName(context);
		String clientType = clientType(context);
		UUID clientId = clientId(context);
		User user = user(context);
		Version version = clientVersion(context);

		return server.connect(ConnectionRequest.builder()
						.user(user)
						.clientId(clientId)
						.clientType(clientType)
						.version(version)
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, domainTypeName)
						.parameter(Server.CLIENT_HOST, remoteHost(context.req()))
						.build());
	}

	private final class JavalinConfigurer implements Consumer<JavalinConfig> {

		@Override
		public void accept(JavalinConfig config) {
			config.useVirtualThreads = useVirtualThreads;
			if (sslEnabled) {
				config.registerPlugin(new SslPlugin(new SslPLuginConfigurer()));
			}
		}
	}

	private final class SslPLuginConfigurer implements Consumer<SslConfig> {

		@Override
		public void accept(SslConfig ssl) {
			ssl.keystoreFromPath(KEYSTORE_PATH.getOrThrow(), KEYSTORE_PASSWORD.getOrThrow());
			ssl.securePort = securePort;
			ssl.insecurePort = port;
		}
	}

	private ObjectMapper objectMapper(Entities entities) {
		return domainObjectMappers.computeIfAbsent(entities.domainType(), domainType ->
						DatabaseObjectMapper.databaseObjectMapper(EntityObjectMapperFactory.instance(domainType)
										.entityObjectMapper(entities)));
	}

	private static String remoteHost(HttpServletRequest request) {
		String forwardHeader = request.getHeader(X_FORWARDED_FOR);
		if (forwardHeader == null) {
			return request.getRemoteAddr();
		}

		return forwardHeader.split(",")[0];
	}

	private static String domainTypeName(Context context) throws ServerAuthenticationException {
		return checkHeaderParameter(context.header(DOMAIN_TYPE_NAME), DOMAIN_TYPE_NAME);
	}

	private static String clientType(Context context) throws ServerAuthenticationException {
		return checkHeaderParameter(context.header(CLIENT_TYPE), CLIENT_TYPE);
	}

	private static UUID clientId(Context context) throws ServerAuthenticationException {
		UUID headerClientId = UUID.fromString(checkHeaderParameter(context.header(CLIENT_ID), CLIENT_ID));
		HttpSession session = context.req().getSession();
		if (session.isNew()) {
			session.setAttribute(CLIENT_ID, headerClientId);
		}
		else {
			UUID sessionClientId = (UUID) session.getAttribute(CLIENT_ID);
			if (sessionClientId == null || !sessionClientId.equals(headerClientId)) {
				session.invalidate();

				throw new ServerAuthenticationException("Invalid client id");
			}
		}

		return headerClientId;
	}

	private static Version clientVersion(Context context) {
		String clientVersion = context.header(CLIENT_VERSION);

		return clientVersion == null ? null : Version.parse(clientVersion);
	}

	private static User user(Context context) throws ServerAuthenticationException {
		String basicAuth = context.header(AUTHORIZATION);
		if (nullOrEmpty(basicAuth)) {
			throw new ServerAuthenticationException("Authorization information missing");
		}

		if (basicAuth.length() > BASIC_PREFIX_LENGTH && BASIC_PREFIX.equalsIgnoreCase(basicAuth.substring(0, BASIC_PREFIX_LENGTH))) {
			return User.parse(new String(Base64.getDecoder().decode(basicAuth.substring(BASIC_PREFIX_LENGTH))));
		}

		throw new ServerAuthenticationException("Invalid authorization format");
	}

	private static String checkHeaderParameter(String header, String headerParameter)
					throws ServerAuthenticationException {
		if (nullOrEmpty(header)) {
			throw new ServerAuthenticationException(headerParameter + " header parameter is missing");
		}

		return header;
	}

	private static void handleException(Context context, Exception exception) {
		LOG.error(exception.getMessage(), exception);
		context.status(exceptionStatus(exception))
						.result(exceptionResult(exception));
	}

	private static int exceptionStatus(Exception exception) {
		if (exception instanceof ServerAuthenticationException) {
			return HttpStatus.UNAUTHORIZED_401;
		}

		return HttpStatus.INTERNAL_SERVER_ERROR_500;
	}

	private static byte[] exceptionResult(Exception exception) {
		try {
			return serialize(exception);
		}
		catch (IOException e) {
			LOG.error(e.getMessage(), e);
			try {
				return serialize(e);
			}
			catch (IOException io) {
				LOG.error(e.getMessage(), io);
				return new byte[0];
			}
		}
	}

	private static <T> T deserialize(HttpServletRequest request) throws IOException, ClassNotFoundException {
		/*
		 * SECURITY NOTE (CodeQL warning about deserialization of user-controlled data):
		 *
		 * This deserialization is protected by JVM-wide ObjectInputFilter configuration.
		 * EntityService is an AuxiliaryServer that runs exclusively alongside EntityServer
		 * in the same JVM process, and cannot be deployed independently.
		 *
		 * EntityServer enforces deserialization filter configuration through the following mechanism:
		 *
		 * 1. By default, EntityServer REQUIRES an ObjectInputFilterFactory to be configured via:
		 *    codion.server.objectInputFilterFactory=my.filter.MyObjectInputFilterFactory
		 *
		 * 2. If no filter factory is configured, the server throws an exception on startup and
		 *    refuses to start. This prevents accidental deployment without deserialization filtering.
		 *
		 * 3. The requirement can only be explicitly disabled via system property:
		 *    codion.server.objectInputFilterFactoryRequired=false
		 *    (not recommended for production)
		 *
		 * 4. The configured ObjectInputFilter applies globally to ALL ObjectInputStream instances
		 *    in the JVM, including this deserialization call, providing defense against
		 *    deserialization attacks.
		 *
		 * See: documentation/src/docs/asciidoc/technical/server.adoc for complete details on
		 * serialization filtering configuration, including pattern-based filtering, resource
		 * exhaustion limits (maxbytes, maxdepth, maxarray, maxrefs), and whitelist management.
		 *
		 * This architecture ensures that EntityService inherits the same deserialization
		 * protections as the EntityServer RMI interface, making deployment without proper
		 * filtering configuration impossible (by default).
		 */
		return (T) new ObjectInputStream(request.getInputStream()).readObject();
	}

	private static synchronized void resolveClasspathKeyStore() {
		String keystore = CLASSPATH_KEYSTORE.get();
		if (nullOrEmpty(keystore)) {
			LOG.debug("No classpath key store specified via {}", CLASSPATH_KEYSTORE.name());
			return;
		}
		if (!KEYSTORE_PATH.isNull()) {
			throw new IllegalStateException("Classpath keystore (" + keystore + ") can not be specified when "
							+ KEYSTORE_PATH.name() + " is already set to " + KEYSTORE_PATH.get());
		}
		try (InputStream inputStream = EntityService.class.getClassLoader().getResourceAsStream(keystore)) {
			if (inputStream == null) {
				LOG.debug("Specified key store not found on classpath: {}", keystore);
				return;
			}
			File file = File.createTempFile("serverKeyStore", "tmp");
			Files.write(file.toPath(), readBytes(inputStream));
			file.deleteOnExit();

			KEYSTORE_PATH.set(file.getPath());
			LOG.debug("Classpath key store {} written to file {} and set as {}",
							CLASSPATH_KEYSTORE.name(), file, KEYSTORE_PATH.name());
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static byte[] readBytes(InputStream stream) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int line;
		while ((line = stream.read(buffer)) != -1) {
			os.write(buffer, 0, line);
		}
		os.flush();

		return os.toByteArray();
	}
}
