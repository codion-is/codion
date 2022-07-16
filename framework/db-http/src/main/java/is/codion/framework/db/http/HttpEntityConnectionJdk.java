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
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.condition.Conditions.where;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService using the JDK HTTP client
 */
final class HttpEntityConnectionJdk implements EntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(),
          Locale.getDefault());

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnectionJdk.class);

  private static final Executor EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1, new DaemonThreadFactory());

  private static final String AUTHORIZATION = "Authorization";
  private static final String BASIC = "Basic ";
  private static final String DOMAIN_TYPE_NAME = "domainTypeName";
  private static final String CLIENT_TYPE_ID = "clientTypeId";
  private static final String CLIENT_ID = "clientId";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  private static final String HTTP = "http://";
  private static final String HTTPS = "https://";
  private static final int HTTP_STATUS_OK = 200;

  private final User user;
  private final String baseurl;
  private final HttpClient httpClient;
  private final Entities entities;
  private final String[] headers;

  private boolean closed;

  /**
   * Instantiates a new {@link HttpEntityConnectionJdk} instance
   * @param domain the entities entities
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param httpsEnabled if true then https is used
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   */
  HttpEntityConnectionJdk(String domainName, String serverHostName, int serverPort,
                          boolean httpsEnabled, User user, String clientTypeId, UUID clientId) {
    this.user = Objects.requireNonNull(user, "user");
    this.baseurl = (httpsEnabled ? HTTPS : HTTP) + Objects.requireNonNull(serverHostName, "serverHostName") + ":" + serverPort + "/entities/ser/";
    this.httpClient = createHttpClient();
    this.headers = new String[] {
            DOMAIN_TYPE_NAME, Objects.requireNonNull(domainName, DOMAIN_TYPE_NAME),
            CLIENT_TYPE_ID, Objects.requireNonNull(clientTypeId, CLIENT_TYPE_ID),
            CLIENT_ID, Objects.requireNonNull(clientId, CLIENT_ID).toString(),
            CONTENT_TYPE, APPLICATION_OCTET_STREAM,
            AUTHORIZATION, BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + String.valueOf(user.getPassword())).getBytes())
    };
    this.entities = initializeEntities();
  }

  @Override
  public Entities getEntities() {
    return entities;
  }

  @Override
  public User getUser() {
    return user;
  }

  @Override
  public boolean isConnected() {
    synchronized (this.entities) {
      return !closed;
    }
  }

  @Override
  public void close() {
    try {
      synchronized (this.entities) {
        handleResponse(execute(createRequest("close")));
        closed = true;
      }
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isTransactionOpen() {
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("isTransactionOpen")));
      }
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void beginTransaction() {
    try {
      synchronized (this.entities) {
        handleResponse(execute(createRequest("beginTransaction")));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void rollbackTransaction() {
    try {
      synchronized (this.entities) {
        handleResponse(execute(createRequest("rollbackTransaction")));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void commitTransaction() {
    try {
      synchronized (this.entities) {
        handleResponse(execute(createRequest("commitTransaction")));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setQueryCacheEnabled(boolean queryCacheEnabled) {
    try {
      synchronized (this.entities) {
        handleResponse(execute(createRequest("setQueryCacheEnabled", queryCacheEnabled)));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isQueryCacheEnabled() {
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("isQueryCacheEnabled")));
      }
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
        return handleResponse(execute(createRequest("function", asList(functionType, argument))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
        handleResponse(execute(createRequest("procedure", asList(procedureType, argument))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("insert", entities)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("update", entities)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public int update(UpdateCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("updateByCondition", condition)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
        handleResponse(execute(createRequest("deleteByKey", keys)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public int delete(Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("delete", condition)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("values", asList(attribute, condition))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> Entity selectSingle(Attribute<T> attribute, T value) throws DatabaseException {
    return selectSingle(where(attribute).equalTo(value));
  }

  @Override
  public Entity select(Key key) throws DatabaseException {
    return selectSingle(condition(key));
  }

  @Override
  public Entity selectSingle(Condition condition) throws DatabaseException {
    List<Entity> selected = select(condition);
    if (Util.nullOrEmpty(selected)) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (selected.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("many_records_found"));
    }

    return selected.get(0);
  }

  @Override
  public List<Entity> select(Collection<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys, "keys");
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("selectByKey", keys)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Entity> select(Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition, "condition");
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("select", condition)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> List<Entity> select(Attribute<T> attribute, T value) throws DatabaseException {
    return select(attribute, singletonList(value));
  }

  @Override
  public <T> List<Entity> select(Attribute<T> attribute, Collection<T> values) throws DatabaseException {
    return select(where(attribute).equalTo(values));
  }

  @Override
  public Map<EntityType, Collection<Entity>> selectDependencies(Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("dependencies", entities)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public int rowCount(Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("count", condition)));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T, R, P> R fillReport(ReportType<T, R, P> reportType, P reportParameters) throws DatabaseException, ReportException {
    Objects.requireNonNull(reportType, "reportType");
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("report", Arrays.asList(reportType, reportParameters))));
      }
    }
    catch (ReportException | DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeBlob(Key primaryKey, Attribute<byte[]> blobAttribute, byte[] blobData)
          throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobAttribute, "blobAttribute");
    Objects.requireNonNull(blobData, "blobData");
    try {
      synchronized (this.entities) {
        handleResponse(execute(createRequest("writeBlob", Arrays.asList(primaryKey, blobAttribute, blobData))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] readBlob(Key primaryKey, Attribute<byte[]> blobAttribute) throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobAttribute, "blobAttribute");
    try {
      synchronized (this.entities) {
        return handleResponse(execute(createRequest("readBlob", Arrays.asList(primaryKey, blobAttribute))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private Entities initializeEntities() {
    try {
      return handleResponse(execute(createRequest("getEntities")));
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private <T> HttpResponse<T> execute(HttpRequest operation) throws Exception {
    synchronized (httpClient) {
      return (HttpResponse<T>) httpClient.send(operation, HttpResponse.BodyHandlers.ofByteArray());
    }
  }

  private HttpRequest createRequest(String path) throws IOException {
    return createRequest(path, null);
  }

  private HttpRequest createRequest(String path, Object data) throws IOException {
    return HttpRequest.newBuilder()
            .uri(URI.create(baseurl + path))
            .POST(data == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(data)))
            .headers(headers).build();
  }

  private static HttpClient createHttpClient() {
    return HttpClient.newBuilder().executor(EXECUTOR)
            .cookieHandler(new CookieManager())
            .connectTimeout(Duration.ofSeconds(2)).build();
  }

  private static <T> T handleResponse(HttpResponse<T> response) throws Exception {
    if (response.statusCode() != HTTP_STATUS_OK) {
      throw (Exception) Serializer.deserialize((byte[]) response.body());
    }

    return Serializer.deserialize((byte[]) response.body());
  }

  private static class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  }
}
