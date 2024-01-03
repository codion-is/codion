/*
 * Copyright (c) 2017 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.Serializer;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.db.DatabaseObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapperFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * A Http based {@link EntityConnection} implementation based on EntityJsonService
 */
final class JsonHttpEntityConnection extends AbstractHttpEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(),
          Locale.getDefault());

  private final EntityObjectMapper entityObjectMapper;
  private final DatabaseObjectMapper databaseObjectMapper;

  /**
   * Instantiates a new {@link JsonHttpEntityConnection} instance
   * @param domainType the domain model type
   * @param hostName the http server host name
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   * @param port the http server port
   * @param securePort the https server port
   * @param httpsEnabled if true then https is used
   * @param socketTimeout the socket timeout
   * @param connectTimeout the connect timeout
   * @param connectionManager the connection manager
   */
  JsonHttpEntityConnection(DomainType domainType, String hostName, User user, String clientTypeId, UUID clientId,
                           int port, int securePort, boolean httpsEnabled, int socketTimeout, int connectTimeout,
                           HttpClientConnectionManager connectionManager) {
    super(domainType, hostName, user, clientTypeId, clientId, "application/json", "/entities/json",
            port, securePort, httpsEnabled, socketTimeout, connectTimeout, connectionManager);
    this.entityObjectMapper = EntityObjectMapperFactory.instance(entities().domainType()).entityObjectMapper(entities());
    this.databaseObjectMapper = DatabaseObjectMapper.databaseObjectMapper(entityObjectMapper);
  }

  @Override
  public boolean transactionOpen() {
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("isTransactionOpen")), entityObjectMapper, Boolean.class);
      }
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void beginTransaction() {
    try {
      synchronized (this.entities) {
        onJsonResponse(execute(createHttpPost("beginTransaction")));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void rollbackTransaction() {
    try {
      synchronized (this.entities) {
        onJsonResponse(execute(createHttpPost("rollbackTransaction")));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void commitTransaction() {
    try {
      synchronized (this.entities) {
        onJsonResponse(execute(createHttpPost("commitTransaction")));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void setQueryCacheEnabled(boolean queryCacheEnabled) {
    try {
      synchronized (this.entities) {
        onJsonResponse(execute(createHttpPost("setQueryCacheEnabled", byteArrayEntity(queryCacheEnabled))));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public boolean isQueryCacheEnabled() {
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("isQueryCacheEnabled")), entityObjectMapper, Boolean.class);
      }
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType) throws DatabaseException {
    return execute(functionType, null);
  }

  @Override
  public <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T argument) throws DatabaseException {
    Objects.requireNonNull(functionType);
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("function", byteArrayEntity(asList(functionType, argument)))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) throws DatabaseException {
    execute(procedureType, null);
  }

  @Override
  public <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T argument) throws DatabaseException {
    Objects.requireNonNull(procedureType);
    try {
      synchronized (this.entities) {
        onResponse(execute(createHttpPost("procedure", byteArrayEntity(asList(procedureType, argument)))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Entity.Key insert(Entity entity) throws DatabaseException {
    return insert(singletonList(entity)).iterator().next();
  }

  @Override
  public Entity insertSelect(Entity entity) throws DatabaseException {
    return insertSelect(singletonList(entity)).iterator().next();
  }

  @Override
  public Collection<Entity.Key> insert(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("insert",
                        stringEntity(entityObjectMapper.writeValueAsString(entities)))),
                entityObjectMapper, EntityObjectMapper.KEY_LIST_REFERENCE);
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Collection<Entity> insertSelect(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("insertSelect",
                        stringEntity(entityObjectMapper.writeValueAsString(entities)))),
                entityObjectMapper, EntityObjectMapper.ENTITY_LIST_REFERENCE);
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void update(Entity entity) throws DatabaseException {
    update(singletonList(entity));
  }

  @Override
  public Entity updateSelect(Entity entity) throws DatabaseException {
    return updateSelect(singletonList(entity)).iterator().next();
  }

  @Override
  public void update(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (this.entities) {
        onJsonResponse(execute(createHttpPost("update",
                stringEntity(entityObjectMapper.writeValueAsString(entities)))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Collection<Entity> updateSelect(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("updateSelect",
                        stringEntity(entityObjectMapper.writeValueAsString(entities)))),
                entityObjectMapper, EntityObjectMapper.ENTITY_LIST_REFERENCE);
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public int update(Update update) throws DatabaseException {
    Objects.requireNonNull(update);
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("updateByCondition",
                        stringEntity(databaseObjectMapper.writeValueAsString(update)))),
                entityObjectMapper, Integer.class);
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void delete(Entity.Key key) throws DatabaseException {
    delete(singletonList(key));
  }

  @Override
  public void delete(Collection<Entity.Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys);
    try {
      synchronized (this.entities) {
        onJsonResponse(execute(createHttpPost("deleteByKey",
                stringEntity(entityObjectMapper.writeValueAsString(keys)))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public int delete(Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("delete",
                        stringEntity(databaseObjectMapper.writeValueAsString(condition)))),
                entityObjectMapper, Integer.class);
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T> List<T> select(Column<T> column) throws DatabaseException {
    return select(Objects.requireNonNull(column), Select.all(column.entityType())
            .orderBy(ascending(column))
            .build());
  }

  @Override
  public <T> List<T> select(Column<T> column, Condition condition) throws DatabaseException {
    return select(column, Select.where(condition)
            .orderBy(ascending(column))
            .build());
  }

  @Override
  public <T> List<T> select(Column<T> column, Select select) throws DatabaseException {
    Objects.requireNonNull(column);
    Objects.requireNonNull(select);
    try {
      ObjectNode node = entityObjectMapper.createObjectNode();
      node.set("column", databaseObjectMapper.valueToTree(column.name()));
      node.set("entityType", databaseObjectMapper.valueToTree(column.entityType().name()));
      node.set("condition", databaseObjectMapper.valueToTree(select));
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("values", stringEntity(node.toString()))), entityObjectMapper,
                entityObjectMapper.getTypeFactory().constructCollectionType(List.class, column.type().valueClass()));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Entity select(Entity.Key key) throws DatabaseException {
    return selectSingle(key(key));
  }

  @Override
  public Entity selectSingle(Condition condition) throws DatabaseException {
    return selectSingle(Select.where(condition).build());
  }

  @Override
  public Entity selectSingle(Select select) throws DatabaseException {
    List<Entity> selected = select(select);
    if (nullOrEmpty(selected)) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (selected.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("multiple_records_found"));
    }

    return selected.get(0);
  }

  @Override
  public Collection<Entity> select(Collection<Entity.Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys, "keys");
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("selectByKey",
                        stringEntity(entityObjectMapper.writeValueAsString(keys)))),
                entityObjectMapper, EntityObjectMapper.ENTITY_LIST_REFERENCE);
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public List<Entity> select(Condition condition) throws DatabaseException {
    return select(Select.where(condition).build());
  }

  @Override
  public List<Entity> select(Select select) throws DatabaseException {
    Objects.requireNonNull(select, "select");
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("select",
                        stringEntity(databaseObjectMapper.writeValueAsString(select)))),
                entityObjectMapper, EntityObjectMapper.ENTITY_LIST_REFERENCE);
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Map<EntityType, Collection<Entity>> dependencies(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      Map<EntityType, Collection<Entity>> dependencies = new HashMap<>();
      DomainType domainType = entities().domainType();

      synchronized (this.entities) {
        onJsonResponse(execute(createHttpPost("dependencies",
                        stringEntity(entityObjectMapper.writeValueAsString(new ArrayList<>(entities))))),
                entityObjectMapper, new TypeReference<Map<String, Collection<Entity>>>() {}).forEach((entityTypeName, deps) ->
                dependencies.put(domainType.entityType(entityTypeName), deps));
      }

      return dependencies;
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public int count(Count count) throws DatabaseException {
    Objects.requireNonNull(count);
    try {
      synchronized (this.entities) {
        return onJsonResponse(execute(createHttpPost("count",
                        stringEntity(databaseObjectMapper.writeValueAsString(count)))),
                entityObjectMapper, Integer.class);
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T, R, P> R report(ReportType<T, R, P> reportType, P reportParameters) throws DatabaseException, ReportException {
    Objects.requireNonNull(reportType, "reportType");
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("report", byteArrayEntity(asList(reportType, reportParameters)))));
      }
    }
    catch (ReportException | DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  private static <T> T onJsonResponse(CloseableHttpResponse closeableHttpResponse, ObjectMapper mapper,
                                      JavaType javaType) throws Exception {
    try (CloseableHttpResponse response = closeableHttpResponse) {
      throwIfError(response);

      return mapper.readValue(response.getEntity().getContent(), javaType);
    }
  }

  private static <T> T onJsonResponse(CloseableHttpResponse closeableHttpResponse, ObjectMapper mapper,
                                      TypeReference<T> typeReference) throws Exception {
    try (CloseableHttpResponse response = closeableHttpResponse) {
      throwIfError(response);

      return mapper.readValue(response.getEntity().getContent(), typeReference);
    }
  }

  private static void onJsonResponse(CloseableHttpResponse closeableHttpResponse) throws Exception {
    onJsonResponse(closeableHttpResponse, null, (Class<?>) null);
  }

  private static <T> T onJsonResponse(CloseableHttpResponse closeableHttpResponse, ObjectMapper mapper,
                                      Class<T> valueClass) throws Exception {
    try (CloseableHttpResponse response = closeableHttpResponse) {
      throwIfError(response);

      if (mapper != null && valueClass != null) {
        return mapper.readValue(response.getEntity().getContent(), valueClass);
      }

      return null;
    }
  }

  private static void throwIfError(CloseableHttpResponse response) throws Exception {
    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      response.getEntity().writeTo(outputStream);

      throw Serializer.<Exception>deserialize(outputStream.toByteArray());
    }
  }

  private static HttpEntity byteArrayEntity(Object data) throws IOException {
    return new ByteArrayEntity(Serializer.serialize(data));
  }

  private static StringEntity stringEntity(String data) {
    return new StringEntity(data, StandardCharsets.UTF_8);
  }
}
