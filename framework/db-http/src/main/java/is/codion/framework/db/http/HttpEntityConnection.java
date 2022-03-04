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
import static is.codion.framework.db.condition.Conditions.where;
import static java.util.Arrays.asList;
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
  HttpEntityConnection(String domainTypeName, String serverHostName, int serverPort,
                       ClientHttps httpsEnabled, User user, String clientTypeId, UUID clientId,
                       HttpClientConnectionManager connectionManager) {
    super(domainTypeName, serverHostName, serverPort, httpsEnabled, user, clientTypeId, clientId,
            "application/octet-stream", "/entities/ser", connectionManager);
  }

  @Override
  public boolean isTransactionOpen() {
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("isTransactionOpen")));
      }
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public void beginTransaction() {
    try {
      synchronized (entities) {
        onResponse(execute(createHttpPost("beginTransaction")));
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
      synchronized (entities) {
        onResponse(execute(createHttpPost("rollbackTransaction")));
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
      synchronized (entities) {
        onResponse(execute(createHttpPost("commitTransaction")));
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
  public <C extends EntityConnection, T, R> R executeFunction(FunctionType<C, T, R> functionType) throws DatabaseException {
    return executeFunction(functionType, null);
  }

  @Override
  public <C extends EntityConnection, T, R> R executeFunction(FunctionType<C, T, R> functionType, T argument) throws DatabaseException {
    Objects.requireNonNull(functionType);
    try {
      synchronized (entities) {
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
  public <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType) throws DatabaseException {
    executeProcedure(procedureType, null);
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType, T argument) throws DatabaseException {
    Objects.requireNonNull(procedureType);
    try {
      synchronized (entities) {
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
  public Key insert(Entity entity) throws DatabaseException {
    return insert(singletonList(entity)).get(0);
  }

  @Override
  public List<Key> insert(List<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("insert", byteArrayEntity(entities))));
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
  public Entity update(Entity entity) throws DatabaseException {
    return update(singletonList(entity)).get(0);
  }

  @Override
  public List<Entity> update(List<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("update", byteArrayEntity(entities))));
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
  public int update(UpdateCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("updateByCondition", byteArrayEntity(condition))));
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
  public void delete(Key entityKey) throws DatabaseException {
    delete(singletonList(entityKey));
  }

  @Override
  public void delete(List<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys);
    try {
      synchronized (entities) {
        onResponse(execute(createHttpPost("deleteByKey", byteArrayEntity(keys))));
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
      synchronized (entities) {
        return onResponse(execute(createHttpPost("delete", byteArrayEntity(condition))));
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
  public <T> List<T> select(Attribute<T> attribute) throws DatabaseException {
    return select(attribute, (Condition) null);
  }

  @Override
  public <T> List<T> select(Attribute<T> attribute, Condition condition) throws DatabaseException {
    Objects.requireNonNull(attribute);
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("values", byteArrayEntity(asList(attribute, condition)))));
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
  public <T> Entity selectSingle(Attribute<T> attribute, T value) throws DatabaseException {
    return selectSingle(where(attribute).equalTo(value));
  }

  @Override
  public Entity selectSingle(Key key) throws DatabaseException {
    return selectSingle(condition(key));
  }

  @Override
  public Entity selectSingle(Condition condition) throws DatabaseException {
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
  public List<Entity> select(List<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys, "keys");
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("selectByKey", byteArrayEntity(keys))));
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
    Objects.requireNonNull(condition, "condition");
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("select", byteArrayEntity(condition))));
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
  public <T> List<Entity> select(Attribute<T> attribute, T value) throws DatabaseException {
    return select(where(attribute).equalTo(value));
  }

  @Override
  public <T> List<Entity> select(Attribute<T> attribute, Collection<T> values) throws DatabaseException {
    return select(where(attribute).equalTo(values));
  }

  @Override
  public Map<EntityType, Collection<Entity>> selectDependencies(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("dependencies", byteArrayEntity(entities))));
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
  public int rowCount(Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("count", byteArrayEntity(condition))));
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
  public <T, R, P> R fillReport(ReportType<T, R, P> reportType, P reportParameters) throws DatabaseException, ReportException {
    Objects.requireNonNull(reportType, "report");
    try {
      synchronized (entities) {
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

  @Override
  public void writeBlob(Key primaryKey, Attribute<byte[]> blobAttribute, byte[] blobData)
          throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobAttribute, "blobAttribute");
    Objects.requireNonNull(blobData, "blobData");
    try {
      synchronized (entities) {
        onResponse(execute(createHttpPost("writeBlob", byteArrayEntity(asList(primaryKey, blobAttribute, blobData)))));
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
  public byte[] readBlob(Key primaryKey, Attribute<byte[]> blobAttribute) throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobAttribute, "blobAttribute");
    try {
      synchronized (entities) {
        return onResponse(execute(createHttpPost("readBlob", byteArrayEntity(asList(primaryKey, blobAttribute)))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  private static HttpEntity byteArrayEntity(Object data) throws IOException {
    return new ByteArrayEntity(Serializer.serialize(data));
  }
}
