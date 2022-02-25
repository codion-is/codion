/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.Serializer;
import is.codion.common.Util;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.report.ReportType;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.plugin.jackson.json.db.ConditionObjectMapper;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.type.TypeReference;
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

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.plugin.jackson.json.domain.EntityObjectMapperFactory.entityObjectMapperFactory;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * A Http based {@link EntityConnection} implementation based on EntityJsonService
 */
final class HttpJsonEntityConnection extends AbstractHttpEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(),
          Locale.getDefault());

  private final EntityObjectMapper entityObjectMapper;
  private final ConditionObjectMapper conditionObjectMapper;

  /**
   * Instantiates a new {@link HttpJsonEntityConnection} instance
   * @param domainTypeName the name of the domain model type
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param httpsEnabled if true then https is used
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   */
  HttpJsonEntityConnection(final String domainTypeName, final String serverHostName, final int serverPort,
                           final ClientHttps httpsEnabled, final User user, final String clientTypeId, final UUID clientId,
                           final HttpClientConnectionManager connectionManager) {
    super(domainTypeName, serverHostName, serverPort, httpsEnabled, user, clientTypeId, clientId,
            "application/json", "/entities/json", connectionManager);
    this.entityObjectMapper = entityObjectMapperFactory(getEntities().getDomainType()).createEntityObjectMapper(getEntities());
    this.conditionObjectMapper = new ConditionObjectMapper(entityObjectMapper);
  }

  @Override
  public boolean isTransactionOpen() {
    try {
      return onJsonResponse(execute(createHttpPost("isTransactionOpen")), entityObjectMapper, Boolean.class);
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void beginTransaction() {
    try {
      onJsonResponse(execute(createHttpPost("beginTransaction")));
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
      onJsonResponse(execute(createHttpPost("rollbackTransaction")));
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
      onJsonResponse(execute(createHttpPost("commitTransaction")));
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(final FunctionType<C, T, R> functionType) throws DatabaseException {
    return executeFunction(functionType, null);
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(final FunctionType<C, T, R> functionType, final T argument) throws DatabaseException {
    Objects.requireNonNull(functionType);
    try {
      return onResponse(execute(createHttpPost("function", byteArrayEntity(asList(functionType, argument)))));
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(final ProcedureType<C, T> procedureType) throws DatabaseException {
    executeProcedure(procedureType, null);
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(final ProcedureType<C, T> procedureType, final T argument) throws DatabaseException {
    Objects.requireNonNull(procedureType);
    try {
      onResponse(execute(createHttpPost("procedure", byteArrayEntity(asList(procedureType, argument)))));
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Key insert(final Entity entity) throws DatabaseException {
    return insert(singletonList(entity)).get(0);
  }

  @Override
  public List<Key> insert(final List<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      return onJsonResponse(execute(createHttpPost("insert",
              stringEntity(entityObjectMapper.writeValueAsString(entities)))),
              entityObjectMapper, EntityObjectMapper.KEY_LIST_REFERENCE);
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public Entity update(final Entity entity) throws DatabaseException {
    return update(singletonList(entity)).get(0);
  }

  @Override
  public List<Entity> update(final List<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      return onJsonResponse(execute(createHttpPost("update",
              stringEntity(entityObjectMapper.writeValueAsString(entities)))),
              entityObjectMapper, EntityObjectMapper.ENTITY_LIST_REFERENCE);
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public int update(final UpdateCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onJsonResponse(execute(createHttpPost("updateByCondition",
              stringEntity(conditionObjectMapper.writeValueAsString(condition)))),
              entityObjectMapper, Integer.class);
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void delete(final Key entityKey) throws DatabaseException {
    delete(singletonList(entityKey));
  }

  @Override
  public void delete(final List<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys);
    try {
      onJsonResponse(execute(createHttpPost("deleteByKey",
              stringEntity(entityObjectMapper.writeValueAsString(keys)))));
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public int delete(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onJsonResponse(execute(createHttpPost("delete",
              stringEntity(conditionObjectMapper.writeValueAsString(condition)))),
              entityObjectMapper, Integer.class);
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T> List<T> select(final Attribute<T> attribute) throws DatabaseException {
    return select(attribute, (Condition) null);
  }

  @Override
  public <T> List<T> select(final Attribute<T> attribute, final Condition condition) throws DatabaseException {
    Objects.requireNonNull(attribute);
    try {
      ObjectNode node = entityObjectMapper.createObjectNode();
      node.set("attribute", conditionObjectMapper.valueToTree(attribute.getName()));
      node.set("entityType", conditionObjectMapper.valueToTree(attribute.getEntityType().getName()));
      if (condition != null) {
        node.set("condition", conditionObjectMapper.valueToTree(condition));
      }

      return onJsonResponse(execute(createHttpPost("values", stringEntity(node.toString()))), entityObjectMapper, List.class);
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T> Entity selectSingle(final Attribute<T> attribute, final T value) throws DatabaseException {
    return selectSingle(where(attribute).equalTo(value));
  }

  @Override
  public Entity selectSingle(final Key key) throws DatabaseException {
    return selectSingle(condition(key));
  }

  @Override
  public Entity selectSingle(final Condition condition) throws DatabaseException {
    List<Entity> selected = select(condition);
    if (Util.nullOrEmpty(selected)) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (selected.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("multiple_records_found"));
    }

    return selected.get(0);
  }

  @Override
  public List<Entity> select(final List<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys, "keys");
    try {
      return onJsonResponse(execute(createHttpPost("selectByKey",
              stringEntity(entityObjectMapper.writeValueAsString(keys)))),
              entityObjectMapper, EntityObjectMapper.ENTITY_LIST_REFERENCE);
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public List<Entity> select(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition, "condition");
    try {
      return onJsonResponse(execute(createHttpPost("select",
              stringEntity(conditionObjectMapper.writeValueAsString(condition)))),
              entityObjectMapper, EntityObjectMapper.ENTITY_LIST_REFERENCE);
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final T value) throws DatabaseException {
    return select(where(attribute).equalTo(value));
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final Collection<T> values) throws DatabaseException {
    return select(where(attribute).equalTo(values));
  }

  @Override
  public Map<EntityType, Collection<Entity>> selectDependencies(final Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      Map<EntityType, Collection<Entity>> dependencies = new HashMap<>();
      DomainType domainType = getEntities().getDomainType();

      onJsonResponse(execute(createHttpPost("dependencies",
              stringEntity(entityObjectMapper.writeValueAsString(new ArrayList<>(entities))))),
              entityObjectMapper, new TypeReference<Map<String, Collection<Entity>>>() {}).forEach((entityTypeName, deps) ->
              dependencies.put(domainType.entityType(entityTypeName), deps));

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
  public int rowCount(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onJsonResponse(execute(createHttpPost("count",
              stringEntity(conditionObjectMapper.writeValueAsString(condition)))),
              entityObjectMapper, Integer.class);
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T, R, P> R fillReport(final ReportType<T, R, P> reportType, final P reportParameters) throws DatabaseException, ReportException {
    Objects.requireNonNull(reportType, "report");
    try {
      return onResponse(execute(createHttpPost("report", byteArrayEntity(asList(reportType, reportParameters)))));
    }
    catch (ReportException | DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void writeBlob(final Key primaryKey, final Attribute<byte[]> blobAttribute, final byte[] blobData)
          throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobAttribute, "blobAttribute");
    Objects.requireNonNull(blobData, "blobData");
    try {
      onResponse(execute(createHttpPost("writeBlob", byteArrayEntity(asList(primaryKey, blobAttribute, blobData)))));
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public byte[] readBlob(final Key primaryKey, final Attribute<byte[]> blobAttribute) throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobAttribute, "blobAttribute");
    try {
      return onResponse(execute(createHttpPost("readBlob", byteArrayEntity(asList(primaryKey, blobAttribute)))));
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  private static <T> T onJsonResponse(final CloseableHttpResponse closeableHttpResponse, final ObjectMapper mapper,
                                      final TypeReference<T> typeReference) throws Exception {
    try (final CloseableHttpResponse response = closeableHttpResponse) {
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outputStream);

        throw Serializer.<Exception>deserialize(outputStream.toByteArray());
      }

      return mapper.readValue(response.getEntity().getContent(), typeReference);
    }
  }

  private static void onJsonResponse(final CloseableHttpResponse closeableHttpResponse) throws Exception {
    onJsonResponse(closeableHttpResponse, null, (Class<?>) null);
  }

  private static <T> T onJsonResponse(final CloseableHttpResponse closeableHttpResponse, final ObjectMapper mapper,
                                      final Class<T> valueClass) throws Exception {
    try (final CloseableHttpResponse response = closeableHttpResponse) {
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outputStream);

        throw Serializer.<Exception>deserialize(outputStream.toByteArray());
      }

      if (mapper != null && valueClass != null) {
        return mapper.readValue(response.getEntity().getContent(), valueClass);
      }

      return null;
    }
  }

  private static HttpEntity byteArrayEntity(final Object data) throws IOException {
    return new ByteArrayEntity(Serializer.serialize(data));
  }

  private static StringEntity stringEntity(final String data) {
    return new StringEntity(data, StandardCharsets.UTF_8);
  }
}
