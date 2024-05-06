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

import static is.codion.common.Serializer.serialize;
import static is.codion.common.Text.nullOrEmpty;
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
	public static final PropertyValue<String> HTTP_SERVER_KEYSTORE_PASSWORD =
					Configuration.stringValue("codion.server.http.keyStorePassword");

	static final String DOMAIN_TYPE_NAME = "domainTypeName";
	static final String CLIENT_TYPE_ID = "clientTypeId";
	static final String CLIENT_ID = "clientId";

	private static final String AUTHORIZATION = "Authorization";
	private static final String BASIC_PREFIX = "basic ";
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final int BASIC_PREFIX_LENGTH = BASIC_PREFIX.length();

	private static final String URL_SERIAL = "entities/serial/";
	private static final String URL_JSON = "entities/json/";

	private final EntitiesHandler entities = new EntitiesHandler();
	private final CloseHandler close = new CloseHandler();
	private final StartTransactionHandler startTransaction = new StartTransactionHandler();
	private final CommitTransactionHandler commitTransaction = new CommitTransactionHandler();
	private final RollbackTransactionHandler rollbackTransaction = new RollbackTransactionHandler();
	private final IsTransactionOpenHandler isTransactionOpen = new IsTransactionOpenHandler();
	private final IsQueryCacheEnabledHandler isQueryCacheEnabled = new IsQueryCacheEnabledHandler();
	private final SetQueryCacheEnabledHandler setQueryCacheEnabled = new SetQueryCacheEnabledHandler();
	private final SelectHandler select = new SelectHandler();
	private final SelectByKeyHandler selectByKey = new SelectByKeyHandler();
	private final InsertHandler insert = new InsertHandler();
	private final InsertSelectHandler insertSelect = new InsertSelectHandler();
	private final UpdateHandler update = new UpdateHandler();
	private final UpdateSelectHandler updateSelect = new UpdateSelectHandler();
	private final UpdateByConditionHandler updateByCondition = new UpdateByConditionHandler();
	private final DeleteHandler delete = new DeleteHandler();
	private final DeleteByKeyHandler deleteByKey = new DeleteByKeyHandler();
	private final ValuesHandler values = new ValuesHandler();
	private final CountHandler count = new CountHandler();
	private final DependenciesHandler dependencies = new DependenciesHandler();
	private final ProcedureHandler procedure = new ProcedureHandler();
	private final FunctionHandler function = new FunctionHandler();
	private final ReportHandler report = new ReportHandler();

	private final Server<RemoteEntityConnection, ? extends ServerAdmin> server;
	private final Javalin javalin;
	private final int port;
	private final int securePort;
	private final boolean sslEnabled;

	private final Map<DomainType, ObjectMapper> domainObjectMappers = new ConcurrentHashMap<>();

	static {
		resolveClasspathKeyStore();
	}

	/**
	 * Instantiates a new EntityService, the port specified by {@link #HTTP_SERVER_PORT}.
	 * @param server the parent server
	 */
	EntityService(Server<RemoteEntityConnection, ? extends ServerAdmin> server) {
		this(server, HTTP_SERVER_PORT.getOrThrow(), HTTP_SERVER_SECURE_PORT.getOrThrow(), HTTP_SERVER_SECURE.getOrThrow());
	}

	/**
	 * Instantiates a new EntityService.
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
		this.javalin = Javalin.create(new JavalinConfigurer());
	}

	@Override
	public void startServer() {
		javalin.start(sslEnabled ? securePort : port);
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
		// Note: some services only implement java serialization
		// (report f.ex) so these are not just some glaring mistakes
		// below, where a ::serial call is associated with the json url
		javalin.post(URL_SERIAL + "entities", entities::serial);
		javalin.post(URL_JSON + "entities", entities::serial);
		javalin.post(URL_SERIAL + "close", close::serial);
		javalin.post(URL_JSON + "close", close::serial);
		javalin.post(URL_SERIAL + "isTransactionOpen", isTransactionOpen::serial);
		javalin.post(URL_JSON + "isTransactionOpen", isTransactionOpen::json);
		javalin.post(URL_SERIAL + "startTransaction", startTransaction::serial);
		javalin.post(URL_JSON + "startTransaction", startTransaction::serial);
		javalin.post(URL_SERIAL + "rollbackTransaction", rollbackTransaction::serial);
		javalin.post(URL_JSON + "rollbackTransaction", rollbackTransaction::serial);
		javalin.post(URL_SERIAL + "commitTransaction", commitTransaction::serial);
		javalin.post(URL_JSON + "commitTransaction", commitTransaction::serial);
		javalin.post(URL_SERIAL + "isQueryCacheEnabled", isQueryCacheEnabled::serial);
		javalin.post(URL_JSON + "isQueryCacheEnabled", isQueryCacheEnabled::json);
		javalin.post(URL_SERIAL + "setQueryCacheEnabled", setQueryCacheEnabled::serial);
		javalin.post(URL_JSON + "setQueryCacheEnabled", setQueryCacheEnabled::json);
		javalin.post(URL_SERIAL + "procedure", procedure::serial);
		javalin.post(URL_JSON + "procedure", procedure::serial);
		javalin.post(URL_SERIAL + "function", function::serial);
		javalin.post(URL_JSON + "function", function::serial);
		javalin.post(URL_SERIAL + "report", report::serial);
		javalin.post(URL_JSON + "report", report::serial);
		javalin.post(URL_SERIAL + "dependencies", dependencies::serial);
		javalin.post(URL_JSON + "dependencies", dependencies::json);
		javalin.post(URL_SERIAL + "count", count::serial);
		javalin.post(URL_JSON + "count", count::json);
		javalin.post(URL_SERIAL + "values", values::serial);
		javalin.post(URL_JSON + "values", values::json);
		javalin.post(URL_SERIAL + "selectByKey", selectByKey::serial);
		javalin.post(URL_JSON + "selectByKey", selectByKey::json);
		javalin.post(URL_SERIAL + "select", select::serial);
		javalin.post(URL_JSON + "select", select::json);
		javalin.post(URL_SERIAL + "insert", insert::serial);
		javalin.post(URL_SERIAL + "insertSelect", insertSelect::serial);
		javalin.post(URL_JSON + "insert", insert::json);
		javalin.post(URL_JSON + "insertSelect", insertSelect::json);
		javalin.post(URL_SERIAL + "update", update::serial);
		javalin.post(URL_SERIAL + "updateSelect", updateSelect::serial);
		javalin.post(URL_JSON + "update", update::json);
		javalin.post(URL_JSON + "updateSelect", updateSelect::json);
		javalin.post(URL_SERIAL + "updateByCondition", updateByCondition::serial);
		javalin.post(URL_JSON + "updateByCondition", updateByCondition::json);
		javalin.post(URL_SERIAL + "delete", delete::serial);
		javalin.post(URL_JSON + "delete", delete::json);
		javalin.post(URL_SERIAL + "deleteByKey", deleteByKey::serial);
		javalin.post(URL_JSON + "deleteByKey", deleteByKey::json);
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
								.result(serialize(connection.isQueryCacheEnabled()));
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
								.result(objectMapper.writeValueAsString(connection.isQueryCacheEnabled()));
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
				connection.setQueryCacheEnabled(deserialize(context.req()));
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
				connection.setQueryCacheEnabled(objectMapper.readValue(context.req().getInputStream(), Boolean.class));
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
				Object argument = parameters.size() > 1 ? parameters.get(1) : null;
				connection.execute((ProcedureType<? extends EntityConnection, Object>) parameters.get(0), argument);
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
				FunctionType<? extends EntityConnection, Object, Object> functionType =
								(FunctionType<? extends EntityConnection, Object, Object>) parameters.get(0);
				Object argument = parameters.size() > 1 ? parameters.get(1) : null;
				context.status(HttpStatus.OK_200)
								.contentType(ContentType.APPLICATION_OCTET_STREAM)
								.result(serialize(connection.execute(functionType, argument)));
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
				Collection<Entity> updated = connection.updateSelect((List<Entity>) deserialize(context.req()));
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
				List<Entity.Key> keys = deserialize(context.req());
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
