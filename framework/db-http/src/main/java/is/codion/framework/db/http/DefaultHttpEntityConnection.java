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
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

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
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.key;
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
  public boolean transactionOpen() {
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
  public Collection<Entity> insertSelect(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("insertSelect", byteArrayEntity(entities))));
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
    update(singletonList(requireNonNull(entity, "entity")));
  }

  @Override
  public Entity updateSelect(Entity entity) throws DatabaseException {
    return updateSelect(singletonList(requireNonNull(entity, "entity"))).iterator().next();
  }

  @Override
  public void update(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      synchronized (this.entities) {
        onResponse(execute(createHttpPost("update", byteArrayEntity(entities))));
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
        return onResponse(execute(createHttpPost("updateSelect", byteArrayEntity(entities))));
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
        return onResponse(execute(createHttpPost("updateByCondition", byteArrayEntity(update))));
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
      synchronized (this.entities) {
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
  public <T> List<T> select(Column<T> column) throws DatabaseException {
    return select(requireNonNull(column), Select.all(column.entityType())
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
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("values", byteArrayEntity(asList(column, select)))));
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
    return select(Select.where(condition).build());
  }

  @Override
  public List<Entity> select(Select select) throws DatabaseException {
    Objects.requireNonNull(select, "select");
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("select", byteArrayEntity(select))));
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
  public int count(Count count) throws DatabaseException {
    Objects.requireNonNull(count);
    try {
      synchronized (this.entities) {
        return onResponse(execute(createHttpPost("count", byteArrayEntity(count))));
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
  public void writeBlob(Entity.Key primaryKey, Column<byte[]> blobColumn, byte[] blobData)
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
  public byte[] readBlob(Entity.Key primaryKey, Column<byte[]> blobColumn) throws DatabaseException {
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
