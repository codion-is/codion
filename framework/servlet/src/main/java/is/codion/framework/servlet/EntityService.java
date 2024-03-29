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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.servlet;

import is.codion.common.Configuration;
import is.codion.common.Serializer;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.property.PropertyValue;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.AuxiliaryServer;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.rmi.server.exception.ServerException;
import is.codion.common.user.User;
import is.codion.common.version.Version;
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
import io.javalin.http.Handler;
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
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.json.domain.EntityObjectMapper.ENTITY_LIST_REFERENCE;
import static is.codion.framework.json.domain.EntityObjectMapper.KEY_LIST_REFERENCE;
import static java.util.Objects.requireNonNull;

public final class EntityService implements AuxiliaryServer {

	private static final Logger LOG = LoggerFactory.getLogger(EntityService.class);

	/**
	 * The port on which the http server is made available to clients.<br>
	 * Value type: Integer<br>
	 * Default value: 8080
	 */
	public static final PropertyValue<Integer> HTTP_SERVER_PORT = Configuration.integerValue("codion.server.http.port", 8080);

	/**
	 * The port on which the http server is made available to clients.<br>
	 * Value type: Integer<br>
	 * Default value: 4443
	 */
	public static final PropertyValue<Integer> HTTP_SERVER_SECURE_PORT = Configuration.integerValue("codion.server.http.securePort", 4443);

	/**
	 * Specifies whether https should be used.<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	public static final PropertyValue<Boolean> HTTP_SERVER_SECURE = Configuration.booleanValue("codion.server.http.secure", true);

	/**
	 * The https keystore to use on the classpath, this will be resolved to a temporary file and set
	 * as the codion.server.http.keyStore system property on server start<br>
	 * Value type: String
	 * Default value: null
	 */
	public static final PropertyValue<String> HTTP_SERVER_CLASSPATH_KEYSTORE = Configuration.stringValue("codion.server.http.classpathKeyStore");

	/**
	 * Specifies the keystore to use for securing http connections.<br>
	 * Value type: String<br>
	 * Default value: null
	 * @see #HTTP_SERVER_CLASSPATH_KEYSTORE
	 */
	public static final PropertyValue<String> HTTP_SERVER_KEYSTORE_PATH = Configuration.stringValue("codion.server.http.keyStore");

	/**
	 * Specifies the password for the keystore used for securing http connections.<br>
	 * Value type: String<br>
	 * Default value: null
	 */
	public static final PropertyValue<String> HTTP_SERVER_KEYSTORE_PASSWORD = Configuration.stringValue("codion.server.http.keyStorePassword");

	static final String DOMAIN_TYPE_NAME = "domainTypeName";
	static final String CLIENT_TYPE_ID = "clientTypeId";
	static final String CLIENT_ID = "clientId";

	private static final String AUTHORIZATION = "Authorization";
	private static final String BASIC_PREFIX = "basic ";
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final int BASIC_PREFIX_LENGTH = BASIC_PREFIX.length();

	private static final String URL_JAVA_SERIALIZATION = "entities/ser/";
	private static final String URL_JSON_SERIALIZATION = "entities/json/";

	private final Server<RemoteEntityConnection, ? extends ServerAdmin> server;
	private final int port;
	private final int securePort;
	private final boolean sslEnabled;

	private final Map<DomainType, ObjectMapper> domainObjectMappers = new ConcurrentHashMap<>();

	private Javalin javalin;

	static {
		resolveClasspathKeyStore();
	}

	/**
	 * Instantiates a new EntityServletServer, the port specified by {@link #HTTP_SERVER_PORT}.
	 * @param server the parent server
	 */
	EntityService(Server<RemoteEntityConnection, ? extends ServerAdmin> server) {
		this(server, HTTP_SERVER_PORT.getOrThrow(), HTTP_SERVER_SECURE_PORT.getOrThrow(), HTTP_SERVER_SECURE.getOrThrow());
	}

	/**
	 * Instantiates a new EntityServletServer.
	 * @param server the parent server
	 * @param port the server port
	 * @param securePort the server secure port (https)
	 * @param sslEnabled true if ssl should be enabled
	 */
	EntityService(Server<RemoteEntityConnection, ? extends ServerAdmin> server, int port, int securePort, boolean sslEnabled) {
		this.server = requireNonNull(server);
		this.port = port;
		this.securePort = securePort;
		this.sslEnabled = sslEnabled;
	}

	@Override
	public void startServer() {
		javalin = Javalin.create(new JavalinConfigurer()).start(sslEnabled ? securePort : port);
		setupHandlers();
	}

	@Override
	public void stopServer() {
		if (javalin != null) {
			javalin.stop();
		}
	}

	@Override
	public String serverInformation() {
		return "Entity Service " + Version.version()
						+ " started on port: " + port
						+ ", securePort: " + securePort
						+ ", sslEnabled: " + sslEnabled;
	}

	private void setupHandlers() {
		EntitiesHandler entitiesHandler = new EntitiesHandler();
		javalin.post(URL_JAVA_SERIALIZATION + "entities", entitiesHandler);
		javalin.post(URL_JSON_SERIALIZATION + "entities", entitiesHandler);
		CloseHandler closeHandler = new CloseHandler();
		javalin.post(URL_JAVA_SERIALIZATION + "close", closeHandler);
		javalin.post(URL_JSON_SERIALIZATION + "close", closeHandler);
		javalin.post(URL_JAVA_SERIALIZATION + "isTransactionOpen", new IsTransactionOpenHandler());
		javalin.post(URL_JSON_SERIALIZATION + "isTransactionOpen", new IsTransactionOpenJsonHandler());
		BeginTransactionHandler beginTransactionHandler = new BeginTransactionHandler();
		javalin.post(URL_JAVA_SERIALIZATION + "beginTransaction", beginTransactionHandler);
		javalin.post(URL_JSON_SERIALIZATION + "beginTransaction", beginTransactionHandler);
		RollbackTransactionHandler rollbackTransactionHandler = new RollbackTransactionHandler();
		javalin.post(URL_JAVA_SERIALIZATION + "rollbackTransaction", rollbackTransactionHandler);
		javalin.post(URL_JSON_SERIALIZATION + "rollbackTransaction", rollbackTransactionHandler);
		CommitTransactionHandler commitTransactionHandler = new CommitTransactionHandler();
		javalin.post(URL_JAVA_SERIALIZATION + "commitTransaction", commitTransactionHandler);
		javalin.post(URL_JSON_SERIALIZATION + "commitTransaction", commitTransactionHandler);
		javalin.post(URL_JAVA_SERIALIZATION + "isQueryCacheEnabled", new IsQueryCacheEnabledHandler());
		javalin.post(URL_JSON_SERIALIZATION + "isQueryCacheEnabled", new IsQueryCacheEnabledJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "setQueryCacheEnabled", new SetQueryCacheEnabledHandler());
		javalin.post(URL_JSON_SERIALIZATION + "setQueryCacheEnabled", new SetQueryCacheEnabledJsonHandler());
		ProcedureHandler procedureHandler = new ProcedureHandler();
		javalin.post(URL_JAVA_SERIALIZATION + "procedure", procedureHandler);
		javalin.post(URL_JSON_SERIALIZATION + "procedure", procedureHandler);
		FunctionHandler functionHandler = new FunctionHandler();
		javalin.post(URL_JAVA_SERIALIZATION + "function", functionHandler);
		javalin.post(URL_JSON_SERIALIZATION + "function", functionHandler);
		ReportHandler reportHandler = new ReportHandler();
		javalin.post(URL_JAVA_SERIALIZATION + "report", reportHandler);
		javalin.post(URL_JSON_SERIALIZATION + "report", reportHandler);
		javalin.post(URL_JAVA_SERIALIZATION + "dependencies", new DependenciesHandler());
		javalin.post(URL_JSON_SERIALIZATION + "dependencies", new DependenciesJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "count", new CountHandler());
		javalin.post(URL_JSON_SERIALIZATION + "count", new CountJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "values", new ValuesHandler());
		javalin.post(URL_JSON_SERIALIZATION + "values", new ValuesJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "selectByKey", new SelectByKeyHandler());
		javalin.post(URL_JSON_SERIALIZATION + "selectByKey", new SelectByKeyJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "select", new SelectHandler());
		javalin.post(URL_JSON_SERIALIZATION + "select", new SelectJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "insert", new InsertHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "insertSelect", new InsertSelectHandler());
		javalin.post(URL_JSON_SERIALIZATION + "insert", new InsertJsonHandler());
		javalin.post(URL_JSON_SERIALIZATION + "insertSelect", new InsertSelectJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "update", new UpdateHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "updateSelect", new UpdateSelectHandler());
		javalin.post(URL_JSON_SERIALIZATION + "update", new UpdateJsonHandler());
		javalin.post(URL_JSON_SERIALIZATION + "updateSelect", new UpdateSelectJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "updateByCondition", new UpdateByConditionHandler());
		javalin.post(URL_JSON_SERIALIZATION + "updateByCondition", new UpdateByConditionJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "delete", new DeleteHandler());
		javalin.post(URL_JSON_SERIALIZATION + "delete", new DeleteJsonHandler());
		javalin.post(URL_JAVA_SERIALIZATION + "deleteByKey", new DeleteByKeyHandler());
		javalin.post(URL_JSON_SERIALIZATION + "deleteByKey", new DeleteByKeyJsonHandler());
	}

	private final class EntitiesHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				context.status(HttpStatus.OK_200)
								.result(Serializer.serialize(connection.entities()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class CloseHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class IsTransactionOpenHandler implements Handler {
		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(connection.transactionOpen()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class IsTransactionOpenJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class BeginTransactionHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				connection.beginTransaction();
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class RollbackTransactionHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class CommitTransactionHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class IsQueryCacheEnabledHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(connection.isQueryCacheEnabled()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class IsQueryCacheEnabledJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_JSON)
								.result(objectMapper.writeValueAsString(connection.isQueryCacheEnabled()));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class SetQueryCacheEnabledHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				connection.setQueryCacheEnabled(deserialize(context.req()));
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class SetQueryCacheEnabledJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				ObjectMapper objectMapper = objectMapper(connection.entities());
				connection.setQueryCacheEnabled(objectMapper.readValue(context.req().getInputStream(), Boolean.class));
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class ProcedureHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Object> parameters = deserialize(context.req());
				Object argument = parameters.size() > 1 ? parameters.get(1) : null;
				connection.execute((ProcedureType<? extends EntityConnection, Object>) parameters.get(0), argument);
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class FunctionHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Object> parameters = deserialize(context.req());
				FunctionType<? extends EntityConnection, Object, Object> functionType =
								(FunctionType<? extends EntityConnection, Object, Object>) parameters.get(0);
				Object argument = parameters.size() > 1 ? parameters.get(1) : null;
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(connection.execute(functionType, argument)));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class ReportHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Object> parameters = deserialize(context.req());
				ReportType<?, ?, Object> reportType = (ReportType<?, ?, Object>) parameters.get(0);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(connection.report(reportType, parameters.get(1))));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class DependenciesHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(connection.dependencies(deserialize(context.req()))));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class DependenciesJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class CountHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				int rowCount = connection.count(deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(rowCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class CountJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class ValuesHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Object> parameters = deserialize(context.req());
				List<?> values = connection.select((Column<?>) parameters.get(0), (Select) parameters.get(1));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(values));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class ValuesJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Entities entities = connection.entities();
				ObjectMapper objectMapper = objectMapper(entities);
				JsonNode jsonNode = objectMapper.readTree(context.req().getInputStream());
				EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
				Column<?> column = (Column<?>) entities.definition(entityType).attributes().get(jsonNode.get("column").textValue());
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

	private final class SelectByKeyHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Entity.Key> keys = deserialize(context.req());
				Collection<Entity> selected = connection.select(keys);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(selected));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class SelectByKeyJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class SelectHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Select select = deserialize(context.req());
				List<Entity> selected = connection.select(select);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(selected));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class SelectJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class InsertHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Collection<Entity.Key> keys = connection.insert((Collection<Entity>) deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(keys));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class InsertSelectHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Collection<Entity> entities = connection.insertSelect((Collection<Entity>) deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(entities));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class InsertJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class InsertSelectJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class UpdateHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				connection.update((List<Entity>) deserialize(context.req()));
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class UpdateSelectHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Collection<Entity> updated = connection.updateSelect((List<Entity>) deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(updated));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class UpdateJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class UpdateSelectJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class UpdateByConditionHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				int updateCount = connection.update((Update) deserialize(context.req()));
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(updateCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class UpdateByConditionJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class DeleteHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				Condition condition = deserialize(context.req());
				int deleteCount = connection.delete(condition);
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(Serializer.serialize(deleteCount));
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class DeleteJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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

	private final class DeleteByKeyHandler implements Handler {

		@Override
		public void handle(Context context) {
			try {
				RemoteEntityConnection connection = authenticate(context);
				List<Entity.Key> keys = deserialize(context.req());
				connection.delete(keys);
				context.status(HttpStatus.OK_200);
			}
			catch (Exception e) {
				handleException(context, e);
			}
		}
	}

	private final class DeleteByKeyJsonHandler implements Handler {

		@Override
		public void handle(Context context) {
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
		String clientTypeId = clientTypeId(context);
		UUID clientId = clientId(context);
		User user = user(context);

		return server.connect(ConnectionRequest.builder()
						.user(user)
						.clientId(clientId)
						.clientTypeId(clientTypeId)
						.parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, domainTypeName)
						.parameter(Server.CLIENT_HOST, remoteHost(context.req()))
						.build());
	}

	private final class JavalinConfigurer implements Consumer<JavalinConfig> {

		@Override
		public void accept(JavalinConfig config) {
			if (sslEnabled) {
				config.registerPlugin(new SslPlugin(new SslPLuginConfigurer()));
			}
		}
	}

	private final class SslPLuginConfigurer implements Consumer<SslConfig> {

		@Override
		public void accept(SslConfig ssl) {
			ssl.keystoreFromPath(HTTP_SERVER_KEYSTORE_PATH.getOrThrow(), HTTP_SERVER_KEYSTORE_PASSWORD.getOrThrow());
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

	private static String clientTypeId(Context context) throws ServerAuthenticationException {
		return checkHeaderParameter(context.header(CLIENT_TYPE_ID), CLIENT_TYPE_ID);
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
			return Serializer.serialize(exception);
		}
		catch (IOException e) {
			LOG.error(e.getMessage(), e);
			try {
				return Serializer.serialize(e);
			}
			catch (IOException io) {
				LOG.error(e.getMessage(), io);
				return new byte[0];
			}
		}
	}

	private static <T> T deserialize(HttpServletRequest request) throws IOException, ClassNotFoundException {
		return (T) new ObjectInputStream(request.getInputStream()).readObject();
	}

	private static synchronized void resolveClasspathKeyStore() {
		String keystore = HTTP_SERVER_CLASSPATH_KEYSTORE.get();
		if (nullOrEmpty(keystore)) {
			LOG.debug("No classpath key store specified via {}", HTTP_SERVER_CLASSPATH_KEYSTORE.propertyName());
			return;
		}
		if (HTTP_SERVER_KEYSTORE_PATH.isNotNull()) {
			throw new IllegalStateException("Classpath keystore (" + keystore + ") can not be specified when "
							+ HTTP_SERVER_KEYSTORE_PATH.propertyName() + " is already set to " + HTTP_SERVER_KEYSTORE_PATH.get());
		}
		try (InputStream inputStream = EntityService.class.getClassLoader().getResourceAsStream(keystore)) {
			if (inputStream == null) {
				LOG.debug("Specified key store not found on classpath: {}", keystore);
				return;
			}
			File file = File.createTempFile("serverKeyStore", "tmp");
			Files.write(file.toPath(), readBytes(inputStream));
			file.deleteOnExit();

			HTTP_SERVER_KEYSTORE_PATH.set(file.getPath());
			LOG.debug("Classpath key store {} written to file {} and set as {}",
							HTTP_SERVER_CLASSPATH_KEYSTORE.propertyName(), file, HTTP_SERVER_KEYSTORE_PATH.propertyName());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
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
