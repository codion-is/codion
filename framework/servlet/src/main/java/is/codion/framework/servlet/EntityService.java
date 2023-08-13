/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.json.db.ConditionObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapperFactory;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

  private final Map<DomainType, EntityObjectMapper> entityObjectMappers = new ConcurrentHashMap<>();
  private final Map<DomainType, ConditionObjectMapper> conditionObjectMappers = new ConcurrentHashMap<>();

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
      javalin.close();
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
    javalin.post(URL_JSON_SERIALIZATION + "insert", new InsertJsonHandler());
    javalin.post(URL_JAVA_SERIALIZATION + "update", new UpdateHandler());
    javalin.post(URL_JSON_SERIALIZATION + "update", new UpdateJsonHandler());
    javalin.post(URL_JAVA_SERIALIZATION + "updateByCondition", new UpdateByConditionHandler());
    javalin.post(URL_JSON_SERIALIZATION + "updateByCondition", new UpdateByConditionJsonHandler());
    javalin.post(URL_JAVA_SERIALIZATION + "delete", new DeleteHandler());
    javalin.post(URL_JSON_SERIALIZATION + "delete", new DeleteJsonHandler());
    javalin.post(URL_JAVA_SERIALIZATION + "deleteByKey", new DeleteByKeyHandler());
    javalin.post(URL_JSON_SERIALIZATION + "deleteByKey", new DeleteByKeyJsonHandler());
    WriteBlobHandler writeBlobHandler = new WriteBlobHandler();
    javalin.post(URL_JAVA_SERIALIZATION + "writeBlob", writeBlobHandler);
    javalin.post(URL_JSON_SERIALIZATION + "writeBlob", writeBlobHandler);
    ReadBlobHandler readBlobHandler = new ReadBlobHandler();
    javalin.post(URL_JAVA_SERIALIZATION + "readBlob", readBlobHandler);
    javalin.post(URL_JSON_SERIALIZATION + "readBlob", readBlobHandler);
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
        context.req.getSession().invalidate();
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
                .result(Serializer.serialize(connection.isTransactionOpen()));
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
        EntityObjectMapper entityObjectMapper = entityObjectMapper(connection.entities());
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(entityObjectMapper.writeValueAsString(connection.isTransactionOpen()));
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
        EntityObjectMapper entityObjectMapper = entityObjectMapper(connection.entities());
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(entityObjectMapper.writeValueAsString(connection.isQueryCacheEnabled()));
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
        connection.setQueryCacheEnabled(deserialize(context.req));
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
        connection.setQueryCacheEnabled(deserialize(context.req));
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
        List<Object> parameters = deserialize(context.req);
        Object argument = parameters.size() > 1 ? parameters.get(1) : null;
        connection.executeProcedure((ProcedureType<? extends EntityConnection, Object>) parameters.get(0), argument);
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
        List<Object> parameters = deserialize(context.req);
        FunctionType<? extends EntityConnection, Object, Object> functionType =
                (FunctionType<? extends EntityConnection, Object, Object>) parameters.get(0);
        Object argument = parameters.size() > 1 ? parameters.get(1) : null;
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(connection.executeFunction(functionType, argument)));
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
        List<Object> parameters = deserialize(context.req);
        ReportType<?, ?, Object> reportType = (ReportType<?, ?, Object>) parameters.get(0);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(connection.fillReport(reportType, parameters.get(1))));
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
                .result(Serializer.serialize(connection.selectDependencies(deserialize(context.req))));
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
        EntityObjectMapper entityObjectMapper = entityObjectMapper(connection.entities());
        List<Entity> entities = entityObjectMapper.deserializeEntities(context.req.getInputStream());
        Map<EntityType, Collection<Entity>> dependencies = connection.selectDependencies(entities);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(entityObjectMapper.writeValueAsString(dependencies));
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
        int rowCount = connection.rowCount(deserialize(context.req));
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
        ConditionObjectMapper conditionObjectMapper = conditionObjectMapper(connection.entities());
        int rowCount = connection.rowCount(conditionObjectMapper.readValue(context.req.getInputStream(), Criteria.class));
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(conditionObjectMapper.entityObjectMapper().writeValueAsString(rowCount));
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
        List<Object> parameters = deserialize(context.req);
        List<?> values = connection.select((Column<?>) parameters.get(0), (SelectCondition) parameters.get(1));
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
        ConditionObjectMapper mapper = conditionObjectMapper(entities);
        JsonNode jsonNode = mapper.readTree(context.req.getInputStream());
        EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
        Column<?> column = (Column<?>) entities.definition(entityType).attribute(jsonNode.get("column").textValue());
        SelectCondition condition = null;
        JsonNode conditionNode = jsonNode.get("condition");
        if (conditionNode != null) {
          condition = mapper.readValue(conditionNode.toString(), SelectCondition.class);
        }
        List<?> values = connection.select(column, condition);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.entityObjectMapper().writeValueAsString(values));
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
        List<Key> keys = deserialize(context.req);
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
        EntityObjectMapper entityObjectMapper = entityObjectMapper(connection.entities());
        List<Key> keysFromJson = entityObjectMapper.deserializeKeys(context.req.getInputStream());
        Collection<Entity> selected = connection.select(keysFromJson);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(entityObjectMapper.writeValueAsString(selected));
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
        SelectCondition selectCondition = deserialize(context.req);
        List<Entity> selected = connection.select(selectCondition);
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
        ConditionObjectMapper mapper = conditionObjectMapper(connection.entities());
        SelectCondition selectConditionJson = mapper.readValue(context.req.getInputStream(), SelectCondition.class);
        List<Entity> selected = connection.select(selectConditionJson);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.entityObjectMapper().writeValueAsString(selected));
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
        Collection<Key> keys = connection.insert((Collection<Entity>) deserialize(context.req));
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(keys));
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
        EntityObjectMapper mapper = entityObjectMapper(connection.entities());
        Collection<Entity> entities = mapper.deserializeEntities(context.req.getInputStream());
        Collection<Key> keys = connection.insert(entities);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.writeValueAsString(keys));
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
        Collection<Entity> updated = connection.update((List<Entity>) deserialize(context.req));
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
        EntityObjectMapper mapper = entityObjectMapper(connection.entities());
        List<Entity> entities = mapper.deserializeEntities(context.req.getInputStream());
        Collection<Entity> updated = connection.update(entities);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.writeValueAsString(updated));
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
        int updateCount = connection.update((UpdateCondition) deserialize(context.req));
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
        ConditionObjectMapper mapper = conditionObjectMapper(connection.entities());
        UpdateCondition updateCondition = mapper.readValue(context.req.getInputStream(), UpdateCondition.class);
        int updateCount = connection.update(updateCondition);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.entityObjectMapper().writeValueAsString(updateCount));
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
        Criteria criteria = deserialize(context.req);
        int deleteCount = connection.delete(criteria);
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
        ConditionObjectMapper mapper = conditionObjectMapper(connection.entities());
        Criteria deleteCriteria = mapper.readValue(context.req.getInputStream(), Criteria.class);
        int deleteCount = connection.delete(deleteCriteria);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.entityObjectMapper().writeValueAsString(deleteCount));
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
        List<Key> keys = deserialize(context.req);
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
        EntityObjectMapper mapper = entityObjectMapper(connection.entities());
        List<Key> keys = mapper.deserializeKeys(context.req.getInputStream());
        connection.delete(keys);
        context.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(context, e);
      }
    }
  }

  private final class WriteBlobHandler implements Handler {

    @Override
    public void handle(Context context) {
      try {
        RemoteEntityConnection connection = authenticate(context);
        List<Object> parameters = deserialize(context.req);
        Key key = (Key) parameters.get(0);
        Column<byte[]> column = (Column<byte[]>) parameters.get(1);
        byte[] data = (byte[]) parameters.get(2);
        connection.writeBlob(key, column, data);
        context.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(context, e);
      }
    }
  }

  private final class ReadBlobHandler implements Handler {

    @Override
    public void handle(Context context) {
      try {
        RemoteEntityConnection connection = authenticate(context);
        List<Object> parameters = deserialize(context.req);
        Key key = (Key) parameters.get(0);
        Column<byte[]> column = (Column<byte[]>) parameters.get(1);
        byte[] data = connection.readBlob(key, column);
        context.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(data));
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
            .parameter(Server.CLIENT_HOST, remoteHost(context.req))
            .build());
  }

  private final class JavalinConfigurer implements Consumer<JavalinConfig> {

    @Override
    public void accept(JavalinConfig config) {
//      if (sslEnabled) {
//        config.plugins.register(new SSLPlugin(new SSLPLuginConfigurer()));
//      }
    }
  }
/*
  private final class SSLPLuginConfigurer implements Consumer<SSLConfig> {

    @Override
    public void accept(SSLConfig ssl) {
      ssl.keystoreFromPath(HTTP_SERVER_KEYSTORE_PATH.getOrThrow(), HTTP_SERVER_KEYSTORE_PASSWORD.getOrThrow());
      ssl.securePort = securePort;
      ssl.insecurePort = port;
    }
  }
*/
  private EntityObjectMapper entityObjectMapper(Entities entities) {
    return entityObjectMappers.computeIfAbsent(entities.domainType(), domainType ->
            EntityObjectMapperFactory.instance(domainType).entityObjectMapper(entities));
  }

  private ConditionObjectMapper conditionObjectMapper(Entities entities) {
    return conditionObjectMappers.computeIfAbsent(entities.domainType(), domainType ->
            ConditionObjectMapper.conditionObjectMapper(entityObjectMapper(entities)));
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
    HttpSession session = context.req.getSession();
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
