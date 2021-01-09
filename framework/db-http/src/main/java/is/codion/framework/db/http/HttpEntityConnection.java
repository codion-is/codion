/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.Serializer;
import is.codion.common.Util;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.MultipleRecordsFoundException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportType;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;

import org.apache.http.HttpEntity;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

import static is.codion.framework.db.condition.Conditions.condition;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService
 */
final class HttpEntityConnection extends AbstractHttpEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(),
          Locale.getDefault());

  /**
   * Instantiates a new {@link HttpEntityConnection} instance
   * @param domainTypeName the name of the domain model type
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param httpsEnabled if true then https is used
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   */
  HttpEntityConnection(final String domainTypeName, final String serverHostName, final int serverPort,
                       final ClientHttps httpsEnabled, final User user, final String clientTypeId, final UUID clientId,
                       final HttpClientConnectionManager connectionManager) {
    super(domainTypeName, serverHostName, serverPort, httpsEnabled, user, clientTypeId, clientId,
            "application/octet-stream", "/entities/ser", connectionManager);
  }

  @Override
  public boolean isTransactionOpen() {
    try {
      return onResponse(execute(createHttpPost("isTransactionOpen")));
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void beginTransaction() {
    try {
      onResponse(execute(createHttpPost("beginTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void rollbackTransaction() {
    try {
      onResponse(execute(createHttpPost("rollbackTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void commitTransaction() {
    try {
      onResponse(execute(createHttpPost("commitTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(final FunctionType<C, T, R> functionType) throws DatabaseException {
    return executeFunction(functionType, emptyList());
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(final FunctionType<C, T, R> functionType, final List<T> arguments) throws DatabaseException {
    Objects.requireNonNull(functionType);
    try {
      return onResponse(execute(createHttpPost("function", byteArrayEntity(asList(functionType, arguments)))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(final ProcedureType<C, T> procedureType) throws DatabaseException {
    executeProcedure(procedureType, emptyList());
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(final ProcedureType<C, T> procedureType, final List<T> arguments) throws DatabaseException {
    Objects.requireNonNull(procedureType);
    try {
      onResponse(execute(createHttpPost("procedure", byteArrayEntity(asList(procedureType, arguments)))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
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
      return onResponse(execute(createHttpPost("insert", byteArrayEntity(entities))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
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
      return onResponse(execute(createHttpPost("update", byteArrayEntity(entities))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public int update(final UpdateCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onResponse(execute(createHttpPost("updateByCondition", byteArrayEntity(condition))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public boolean delete(final Key entityKey) throws DatabaseException {
    return delete(singletonList(entityKey)) == 1;
  }

  @Override
  public int delete(final List<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys);
    try {
      return onResponse(execute(createHttpPost("deleteByKey", byteArrayEntity(keys))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public int delete(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onResponse(execute(createHttpPost("delete", byteArrayEntity(condition))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
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
      return onResponse(execute(createHttpPost("values", byteArrayEntity(asList(attribute, condition)))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T> Entity selectSingle(final Attribute<T> attribute, final T value) throws DatabaseException {
    return selectSingle(condition(attribute).equalTo(value));
  }

  @Override
  public Entity selectSingle(final Key key) throws DatabaseException {
    return selectSingle(condition(key));
  }

  @Override
  public Entity selectSingle(final Condition condition) throws DatabaseException {
    final List<Entity> selected = select(condition);
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
      return onResponse(execute(createHttpPost("selectByKey", byteArrayEntity(keys))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public List<Entity> select(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition, "condition");
    try {
      return onResponse(execute(createHttpPost("select", byteArrayEntity(condition))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final T value) throws DatabaseException {
    return select(condition(attribute).equalTo(value));
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final Collection<T> values) throws DatabaseException {
    return select(condition(attribute).equalTo(values));
  }

  @Override
  public Map<EntityType<?>, Collection<Entity>> selectDependencies(final Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      return onResponse(execute(createHttpPost("dependencies", byteArrayEntity(entities))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public int rowCount(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onResponse(execute(createHttpPost("count", byteArrayEntity(condition))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public <T, R, P> R fillReport(final ReportType<T, R, P> reportType, final P reportParameters) throws DatabaseException, ReportException {
    Objects.requireNonNull(reportType, "report");
    try {
      return onResponse(execute(createHttpPost("report", byteArrayEntity(asList(reportType, reportParameters)))));
    }
    catch (final ReportException | DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
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
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
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
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      throw logAndWrap(e);
    }
  }

  private static HttpEntity byteArrayEntity(final Object data) throws IOException {
    return new ByteArrayEntity(Serializer.serialize(data));
  }
}
