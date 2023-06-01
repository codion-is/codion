/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Configuration;
import is.codion.common.Serializer;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.common.properties.PropertyValue;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.AuxiliaryServer;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.rmi.server.exception.ServerException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.json.db.ConditionObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapperFactory;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.RemoteException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;

public final class EntityService implements AuxiliaryServer {

  private static final Logger LOG = LoggerFactory.getLogger(EntityService.class);

  public static final PropertyValue<Integer> HTTP_SERVER_PORT = Configuration.integerValue("codion.server.http.port", 8080);

  static final String DOMAIN_TYPE_NAME = "domainTypeName";
  static final String CLIENT_TYPE_ID = "clientTypeId";
  static final String CLIENT_ID = "clientId";

  private static final String AUTHORIZATION = "Authorization";
  private static final String BASIC_PREFIX = "basic ";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private static final int BASIC_PREFIX_LENGTH = BASIC_PREFIX.length();

  private static final String URL_SERIALIZATION = "entities/ser/";
  private static final String URL_JSON = "entities/json/";

  private final Server<RemoteEntityConnection, ? extends ServerAdmin> server;
  private final int port;

  private final Map<DomainType, EntityObjectMapper> entityObjectMappers = new ConcurrentHashMap<>();
  private final Map<DomainType, ConditionObjectMapper> conditionObjectMappers = new ConcurrentHashMap<>();

  private Javalin javalin;

  /**
   * Instantiates a new EntityServletServer, the port specified by {@link #HTTP_SERVER_PORT}.
   * @param server the parent server
   */
  EntityService(Server<RemoteEntityConnection, ? extends ServerAdmin> server) {
    this(server, HTTP_SERVER_PORT.getOrThrow());
  }

  /**
   * Instantiates a new EntityServletServer.
   * @param server the parent server
   * @param port the server port
   */
  EntityService(Server<RemoteEntityConnection, ? extends ServerAdmin> server, int port) {
    this.server = requireNonNull(server);
    this.port = port;
  }

  @Override
  public void startServer() throws Exception {
    javalin = Javalin.create().start(port);
    setupHandlers();
  }

  @Override
  public void stopServer() throws Exception {
    javalin.close();
  }

  private void setupHandlers() {
    EntitiesHandler entitiesHandler = new EntitiesHandler();
    javalin.post(URL_SERIALIZATION + "entities", entitiesHandler);
    javalin.post(URL_JSON + "entities", entitiesHandler);
    CloseHandler closeHandler = new CloseHandler();
    javalin.post(URL_SERIALIZATION + "close", closeHandler);
    javalin.post(URL_JSON + "close", closeHandler);
    javalin.post(URL_SERIALIZATION + "isTransactionOpen", new IsTransactionOpenHandler());
    javalin.post(URL_JSON + "isTransactionOpen", new IsTransactionOpenJsonHandler());
    BeginTransactionHandler beginTransactionHandler = new BeginTransactionHandler();
    javalin.post(URL_SERIALIZATION + "beginTransaction", beginTransactionHandler);
    javalin.post(URL_JSON + "beginTransaction", beginTransactionHandler);
    RollbackTransactionHandler rollbackTransactionHandler = new RollbackTransactionHandler();
    javalin.post(URL_SERIALIZATION + "rollbackTransaction", rollbackTransactionHandler);
    javalin.post(URL_JSON + "rollbackTransaction", rollbackTransactionHandler);
    CommitTransactionHandler commitTransactionHandler = new CommitTransactionHandler();
    javalin.post(URL_SERIALIZATION + "commitTransaction", commitTransactionHandler);
    javalin.post(URL_JSON + "commitTransaction", commitTransactionHandler);
    javalin.post(URL_SERIALIZATION + "isQueryCacheEnabled", new IsQueryCacheEnabledHandler());
    javalin.post(URL_JSON + "isQueryCacheEnabled", new IsQueryCacheEnabledJsonHandler());
    javalin.post(URL_SERIALIZATION + "setQueryCacheEnabled", new SetQueryCacheEnabledHandler());
    javalin.post(URL_JSON + "setQueryCacheEnabled", new SetQueryCacheEnabledJsonHandler());
    ProcedureHandler procedureHandler = new ProcedureHandler();
    javalin.post(URL_SERIALIZATION + "procedure", procedureHandler);
    javalin.post(URL_JSON + "procedure", procedureHandler);
    FunctionHandler functionHandler = new FunctionHandler();
    javalin.post(URL_SERIALIZATION + "function", functionHandler);
    javalin.post(URL_JSON + "function", functionHandler);
    ReportHandler reportHandler = new ReportHandler();
    javalin.post(URL_SERIALIZATION + "report", reportHandler);
    javalin.post(URL_JSON + "report", reportHandler);
    javalin.post(URL_SERIALIZATION + "dependencies", new DependenciesHandler());
    javalin.post(URL_JSON + "dependencies", new DependenciesJsonHandler());
    javalin.post(URL_SERIALIZATION + "count", new CountHandler());
    javalin.post(URL_JSON + "count", new CountJsonHandler());
    javalin.post(URL_SERIALIZATION + "values", new ValuesHandler());
    javalin.post(URL_JSON + "values", new ValuesJsonHandler());
    javalin.post(URL_SERIALIZATION + "selectByKey", new SelectByKeyHandler());
    javalin.post(URL_JSON + "selectByKey", new SelectByKeyJsonHandler());
    javalin.post(URL_SERIALIZATION + "select", new SelectHandler());
    javalin.post(URL_JSON + "select", new SelectJsonHandler());
    javalin.post(URL_SERIALIZATION + "insert", new InsertHandler());
    javalin.post(URL_JSON + "insert", new InsertJsonHandler());
    javalin.post(URL_SERIALIZATION + "update", new UpdateHandler());
    javalin.post(URL_JSON + "update", new UpdateJsonHandler());
    javalin.post(URL_SERIALIZATION + "updateByCondition", new UpdateByConditionHandler());
    javalin.post(URL_JSON + "updateByCondition", new UpdateByConditionJsonHandler());
    javalin.post(URL_SERIALIZATION + "delete", new DeleteHandler());
    javalin.post(URL_JSON + "delete", new DeleteJsonHandler());
    javalin.post(URL_SERIALIZATION + "deleteByKey", new DeleteByKeyHandler());
    javalin.post(URL_JSON + "deleteByKey", new DeleteByKeyJsonHandler());
    WriteBlobHandler writeBlobHandler = new WriteBlobHandler();
    javalin.post(URL_SERIALIZATION + "writeBlob", writeBlobHandler);
    javalin.post(URL_JSON + "writeBlob", writeBlobHandler);
    ReadBlobHandler readBlobHandler = new ReadBlobHandler();
    javalin.post(URL_SERIALIZATION + "readBlob", readBlobHandler);
    javalin.post(URL_JSON + "readBlob", readBlobHandler);
  }

  private final class EntitiesHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        ctx.status(HttpStatus.OK_200)
                .result(Serializer.serialize(connection.entities()));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class CloseHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        authenticate(ctx).close();
        ctx.req.getSession().invalidate();
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class IsTransactionOpenHandler implements Handler {
    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(connection.isTransactionOpen()));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class IsTransactionOpenJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        EntityObjectMapper entityObjectMapper = entityObjectMapper(connection.entities());
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(entityObjectMapper.writeValueAsString(connection.isTransactionOpen()));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class BeginTransactionHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        connection.beginTransaction();
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class RollbackTransactionHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        connection.rollbackTransaction();
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class CommitTransactionHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        connection.commitTransaction();
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class IsQueryCacheEnabledHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(connection.isQueryCacheEnabled()));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class IsQueryCacheEnabledJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        EntityObjectMapper entityObjectMapper = entityObjectMapper(connection.entities());
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(entityObjectMapper.writeValueAsString(connection.isQueryCacheEnabled()));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class SetQueryCacheEnabledHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        connection.setQueryCacheEnabled(deserialize(ctx.req));
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class SetQueryCacheEnabledJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        connection.setQueryCacheEnabled(deserialize(ctx.req));
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class ProcedureHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      RemoteEntityConnection connection = authenticate(ctx);
      List<Object> parameters = deserialize(ctx.req);
      try {
        Object argument = parameters.size() > 1 ? parameters.get(1) : null;
        connection.executeProcedure((ProcedureType<? extends EntityConnection, Object>) parameters.get(0), argument);
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class FunctionHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      RemoteEntityConnection connection = authenticate(ctx);
      List<Object> parameters = deserialize(ctx.req);
      try {
        FunctionType<? extends EntityConnection, Object, Object> functionType =
                (FunctionType<? extends EntityConnection, Object, Object>) parameters.get(0);
        Object argument = parameters.size() > 1 ? parameters.get(1) : null;
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(connection.executeFunction(functionType, argument)));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class ReportHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        List<Object> parameters = deserialize(ctx.req);
        ReportType<?, ?, Object> reportType = (ReportType<?, ?, Object>) parameters.get(0);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(connection.fillReport(reportType, parameters.get(1))));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class DependenciesHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(connection.selectDependencies(deserialize(ctx.req))));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class DependenciesJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        EntityObjectMapper entityObjectMapper = entityObjectMapper(connection.entities());
        List<Entity> entities = entityObjectMapper.deserializeEntities(ctx.req.getInputStream());
        Map<EntityType, Collection<Entity>> dependencies = connection.selectDependencies(entities);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(entityObjectMapper.writeValueAsString(dependencies));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class CountHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        int rowCount = connection.rowCount(deserialize(ctx.req));
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(rowCount));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class CountJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        ConditionObjectMapper conditionObjectMapper = conditionObjectMapper(connection.entities());
        int rowCount = connection.rowCount(conditionObjectMapper.readValue(ctx.req.getInputStream(), Condition.class));
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(conditionObjectMapper.entityObjectMapper().writeValueAsString(rowCount));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class ValuesHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        List<Object> parameters = deserialize(ctx.req);
        List<?> values = connection.select((Attribute<?>) parameters.get(0), (Condition) parameters.get(1));
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(values));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class ValuesJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        Entities entities = connection.entities();
        ConditionObjectMapper mapper = conditionObjectMapper(entities);
        JsonNode jsonNode = mapper.readTree(ctx.req.getInputStream());
        EntityType entityType = entities.domainType().entityType(jsonNode.get("entityType").asText());
        Attribute<?> attribute = entities.definition(entityType).attribute(jsonNode.get("attribute").textValue());
        Condition condition = null;
        JsonNode conditionNode = jsonNode.get("condition");
        if (conditionNode != null) {
          condition = mapper.readValue(conditionNode.toString(), Condition.class);
        }
        List<?> values = connection.select(attribute, condition);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.entityObjectMapper().writeValueAsString(values));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class SelectByKeyHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        List<Key> keys = deserialize(ctx.req);
        List<Entity> selected = connection.select(keys);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(selected));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class SelectByKeyJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        EntityObjectMapper entityObjectMapper = entityObjectMapper(connection.entities());
        List<Key> keysFromJson = entityObjectMapper.deserializeKeys(ctx.req.getInputStream());
        List<Entity> selected = connection.select(keysFromJson);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(entityObjectMapper.writeValueAsString(selected));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class SelectHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        Condition selectCondition = deserialize(ctx.req);
        List<Entity> selected = connection.select(selectCondition);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(selected));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class SelectJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        ConditionObjectMapper mapper = conditionObjectMapper(connection.entities());
        SelectCondition selectConditionJson = mapper.readValue(ctx.req.getInputStream(), SelectCondition.class);
        List<Entity> selected = connection.select(selectConditionJson);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.entityObjectMapper().writeValueAsString(selected));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class InsertHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        List<Key> keys = connection.insert((List<Entity>) deserialize(ctx.req));
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(keys));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class InsertJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        EntityObjectMapper mapper = entityObjectMapper(connection.entities());
        List<Entity> entities = mapper.deserializeEntities(ctx.req.getInputStream());
        List<Key> keys = connection.insert(entities);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.writeValueAsString(keys));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class UpdateHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        Collection<Entity> updated = connection.update((List<Entity>) deserialize(ctx.req));
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(updated));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private  final class UpdateJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        EntityObjectMapper mapper = entityObjectMapper(connection.entities());
        List<Entity> entities = mapper.deserializeEntities(ctx.req.getInputStream());
        Collection<Entity> updated = connection.update(entities);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.writeValueAsString(updated));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class UpdateByConditionHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        int updateCount = connection.update((UpdateCondition) deserialize(ctx.req));
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(updateCount));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class UpdateByConditionJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        ConditionObjectMapper mapper = conditionObjectMapper(connection.entities());
        UpdateCondition updateCondition = mapper.readValue(ctx.req.getInputStream(), UpdateCondition.class);
        int updateCount = connection.update(updateCondition);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.entityObjectMapper().writeValueAsString(updateCount));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class DeleteHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        Condition condition = deserialize(ctx.req);
        int deleteCount = connection.delete(condition);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(deleteCount));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class DeleteJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        ConditionObjectMapper mapper = conditionObjectMapper(connection.entities());
        Condition deleteCondition = mapper.readValue(ctx.req.getInputStream(), Condition.class);
        int deleteCount = connection.delete(deleteCondition);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_JSON)
                .result(mapper.entityObjectMapper().writeValueAsString(deleteCount));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class DeleteByKeyHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        List<Key> keys = deserialize(ctx.req);
        connection.delete(keys);
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class DeleteByKeyJsonHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        EntityObjectMapper mapper = entityObjectMapper(connection.entities());
        List<Key> keys = mapper.deserializeKeys(ctx.req.getInputStream());
        connection.delete(keys);
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class WriteBlobHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        List<Object> parameters = deserialize(ctx.req);
        Key key = (Key) parameters.get(0);
        Attribute<byte[]> attribute = (Attribute<byte[]>) parameters.get(1);
        byte[] data = (byte[]) parameters.get(2);
        connection.writeBlob(key, attribute, data);
        ctx.status(HttpStatus.OK_200);
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private final class ReadBlobHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      try {
        RemoteEntityConnection connection = authenticate(ctx);
        List<Object> parameters = deserialize(ctx.req);
        Key key = (Key) parameters.get(0);
        Attribute<byte[]> attribute = (Attribute<byte[]>) parameters.get(1);
        byte[] data = connection.readBlob(key, attribute);
        ctx.status(HttpStatus.OK_200)
                .contentType(ContentType.APPLICATION_OCTET_STREAM)
                .result(Serializer.serialize(data));
      }
      catch (Exception e) {
        handleException(ctx, e);
      }
    }
  }

  private RemoteEntityConnection authenticate(Context ctx) throws RemoteException, ServerException {
    if (server == null) {
      throw new IllegalStateException("EntityServer has not been set for EntityService");
    }

    Map<String, String> headers = ctx.headerMap();
    String domainTypeName = domainTypeName(headers);
    String clientTypeId = clientTypeId(headers);
    UUID clientId = clientId(headers, ctx.req.getSession());
    User user = user(headers);

    return server.connect(ConnectionRequest.builder()
            .user(user)
            .clientId(clientId)
            .clientTypeId(clientTypeId)
            .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, domainTypeName)
            .parameter(Server.CLIENT_HOST_KEY, remoteHost(ctx.req))
            .build());
  }

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

  private static String domainTypeName(Map<String, String> headers) throws ServerAuthenticationException {
    return checkHeaderParameter(headers.get(DOMAIN_TYPE_NAME), DOMAIN_TYPE_NAME);
  }

  private static String clientTypeId(Map<String, String> headers) throws ServerAuthenticationException {
    return checkHeaderParameter(headers.get(CLIENT_TYPE_ID), CLIENT_TYPE_ID);
  }

  private static UUID clientId(Map<String, String> headers, HttpSession session)
          throws ServerAuthenticationException {
    UUID headerClientId = UUID.fromString(checkHeaderParameter(headers.get(CLIENT_ID), CLIENT_ID));
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

  private static User user(Map<String, String> headers) throws ServerAuthenticationException {
    String basicAuth = headers.get(AUTHORIZATION);
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

  private static void handleException(Context ctx, Exception exception) {
    LOG.error(exception.getMessage(), exception);
    ctx.status(exceptionStatus(exception))
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
}
