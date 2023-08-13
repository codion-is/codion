/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;

import org.apache.http.HttpEntity;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.db.condition.Condition.where;
import static is.codion.framework.db.criteria.Criteria.key;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService
 */
final class DefaultHttpEntityConnection extends AbstractHttpEntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(),
          Locale.getDefault());

  /**
   * Instantiates a new {@link DefaultHttpEntityConnection} instance
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
  DefaultHttpEntityConnection(DomainType domainType, String hostName, User user, String clientTypeId, UUID clientId,
                              int port, int securePort, boolean httpsEnabled, int socketTimeout, int connectTimeout,
                              HttpClientConnectionManager connectionManager) {
    super(domainType, hostName, user, clientTypeId, clientId, "application/octet-stream", "/entities/ser",
            port, securePort, httpsEnabled, socketTimeout, connectTimeout, connectionManager);
  }

  @Override
  public boolean isTransactionOpen() {
    try {
      synchronized (this.entities) {
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
      synchronized (this.entities) {
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
      synchronized (this.entities) {
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
      synchronized (this.entities) {
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
  public void setQueryCacheEnabled(boolean queryCacheEnabled) {
    try {
      synchronized (this.entities) {
        onResponse(execute(createHttpPost("setQueryCacheEnabled", byteArrayEntity(queryCacheEnabled))));
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
        return onResponse(execute(createHttpPost("isQueryCacheEnabled")));
      }
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
  public <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType) throws DatabaseException {
    executeProcedure(procedureType, null);
  }

  @Override
  public <C extends EntityConnection, T> void executeProcedure(ProcedureType<C, T> procedureType, T argument) throws DatabaseException {
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
  public Key insert(Entity entity) throws DatabaseException {
    return insert(singletonList(entity)).iterator().next();
  }

  @Override
  public Collection<Key> insert(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (this.entities) {
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
    return update(singletonList(entity)).iterator().next();
  }

  @Override
  public Collection<Entity> update(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (this.entities) {
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
      synchronized (this.entities) {
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
  public void delete(Collection<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys);
    try {
      synchronized (this.entities) {
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
  public int delete(Criteria criteria) throws DatabaseException {
    Objects.requireNonNull(criteria);
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("delete", byteArrayEntity(criteria))));
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
    return select(requireNonNull(column), SelectCondition.all(column.entityType())
            .orderBy(ascending(column))
            .build());
  }

  @Override
  public <T> List<T> select(Column<T> column, Criteria criteria) throws DatabaseException {
    return select(column, SelectCondition.where(criteria)
            .orderBy(ascending(column))
            .build());
  }

  @Override
  public <T> List<T> select(Column<T> column, Condition condition) throws DatabaseException {
    Objects.requireNonNull(column);
    Objects.requireNonNull(condition);
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("values", byteArrayEntity(asList(column, condition)))));
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
  public Entity select(Key key) throws DatabaseException {
    return selectSingle(key(key));
  }

  @Override
  public Entity selectSingle(Criteria criteria) throws DatabaseException {
    return selectSingle(where(criteria));
  }

  @Override
  public Entity selectSingle(Condition condition) throws DatabaseException {
    List<Entity> selected = select(condition);
    if (nullOrEmpty(selected)) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (selected.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("multiple_records_found"));
    }

    return selected.get(0);
  }

  @Override
  public Collection<Entity> select(Collection<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys, "keys");
    try {
      synchronized (this.entities) {
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
  public List<Entity> select(Criteria criteria) throws DatabaseException {
    return select(where(criteria));
  }

  @Override
  public List<Entity> select(Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition, "condition");
    try {
      synchronized (this.entities) {
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
  public Map<EntityType, Collection<Entity>> selectDependencies(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      synchronized (this.entities) {
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
  public int rowCount(Criteria criteria) throws DatabaseException {
    Objects.requireNonNull(criteria);
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("count", byteArrayEntity(criteria))));
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

  @Override
  public void writeBlob(Key primaryKey, Column<byte[]> blobColumn, byte[] blobData)
          throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobColumn, "blobAttribute");
    Objects.requireNonNull(blobData, "blobData");
    try {
      synchronized (this.entities) {
        onResponse(execute(createHttpPost("writeBlob", byteArrayEntity(asList(primaryKey, blobColumn, blobData)))));
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
  public byte[] readBlob(Key primaryKey, Column<byte[]> blobColumn) throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobColumn, "blobAttribute");
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("readBlob", byteArrayEntity(asList(primaryKey, blobColumn)))));
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

  static final class DefaultBuilder implements Builder {

    private DomainType domainType;
    private String hostName = HttpEntityConnection.HOSTNAME.get();
    private int port = HttpEntityConnection.PORT.get();
    private int securePort = HttpEntityConnection.SECURE_PORT.get();
    private boolean https = HttpEntityConnection.SECURE.get();
    private boolean json = HttpEntityConnection.JSON.get();
    private int socketTimeout = HttpEntityConnection.SOCKET_TIMEOUT.get();
    private int connectTimeout = HttpEntityConnection.CONNECT_TIMEOUT.get();
    private User user;
    private String clientTypeId;
    private UUID clientId;

    @Override
    public Builder domainType(DomainType domainType) {
      this.domainType = requireNonNull(domainType);
      return this;
    }

    @Override
    public Builder hostName(String hostName) {
      this.hostName = requireNonNull(hostName);
      return this;
    }

    @Override
    public Builder port(int port) {
      this.port = port;
      return this;
    }

    @Override
    public Builder securePort(int securePort) {
      this.securePort = securePort;
      return this;
    }

    @Override
    public Builder https(boolean https) {
      this.https = https;
      return this;
    }

    @Override
    public Builder json(boolean json) {
      this.json = json;
      return this;
    }

    @Override
    public Builder socketTimeout(int socketTimeout) {
      this.socketTimeout = socketTimeout;
      return this;
    }

    @Override
    public Builder connectTimeout(int connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    @Override
    public Builder user(User user) {
      this.user = requireNonNull(user);
      return this;
    }

    @Override
    public Builder clientTypeId(String clientTypeId) {
      this.clientTypeId = requireNonNull(clientTypeId);
      return this;
    }

    @Override
    public Builder clientId(UUID clientId) {
      this.clientId = requireNonNull(clientId);
      return this;
    }

    @Override
    public EntityConnection build() {
      if (json) {
        return new JsonHttpEntityConnection(domainType, hostName, user, clientTypeId, clientId, port, securePort,
                https, socketTimeout, connectTimeout, new BasicHttpClientConnectionManager());
      }

      return new DefaultHttpEntityConnection(domainType, hostName, user, clientTypeId, clientId, port, securePort,
              https, socketTimeout, connectTimeout, new BasicHttpClientConnectionManager());
    }
  }
}
