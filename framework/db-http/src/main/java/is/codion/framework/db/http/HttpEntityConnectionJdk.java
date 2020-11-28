/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService
 */
final class HttpEntityConnectionJdk implements EntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnectionJdk.class.getName(), Locale.getDefault());

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
  HttpEntityConnectionJdk(final String domainName, final String serverHostName, final int serverPort,
                          final boolean httpsEnabled, final User user, final String clientTypeId, final UUID clientId) {
    this.user = Objects.requireNonNull(user, "user");
    this.baseurl = (httpsEnabled ? HTTPS : HTTP) + Objects.requireNonNull(serverHostName, "serverHostName") + ":" + serverPort + "/entities/";
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
    return !closed;
  }

  @Override
  public void close() {
    try {
      handleResponse(execute(createRequest("close")));
      closed = true;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isTransactionOpen() {
    try {
      return handleResponse(execute(createRequest("isTransactionOpen")));
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void beginTransaction() {
    try {
      handleResponse(execute(createRequest("beginTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void rollbackTransaction() {
    try {
      handleResponse(execute(createRequest("rollbackTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void commitTransaction() {
    try {
      handleResponse(execute(createRequest("commitTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      return handleResponse(execute(createRequest("function", asList(functionType, arguments))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      handleResponse(execute(createRequest("procedure", Arrays.asList(procedureType, arguments))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      return handleResponse(execute(createRequest("insert", entities)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      return handleResponse(execute(createRequest("update", entities)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public int update(final UpdateCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return handleResponse(execute(createRequest("updateByCondition", condition)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      return handleResponse(execute(createRequest("deleteByKey", keys)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public int delete(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return handleResponse(execute(createRequest("delete", condition)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      return handleResponse(execute(createRequest("values", asList(attribute, condition))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      throw new MultipleRecordsFoundException(MESSAGES.getString("many_records_found"));
    }

    return selected.get(0);
  }

  @Override
  public List<Entity> select(final List<Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys, "keys");
    try {
      return handleResponse(execute(createRequest("selectByKey", keys)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Entity> select(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition, "condition");
    try {
      return handleResponse(execute(createRequest("select", condition)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final T value) throws DatabaseException {
    return select(attribute, singletonList(value));
  }

  @Override
  public <T> List<Entity> select(final Attribute<T> attribute, final Collection<T> values) throws DatabaseException {
    return select(condition(attribute).equalTo(values));
  }

  @Override
  public Map<EntityType<?>, Collection<Entity>> selectDependencies(final Collection<? extends Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      return handleResponse(execute(createRequest("dependencies", entities)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public int rowCount(final Condition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return handleResponse(execute(createRequest("count", condition)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T, R, P> R fillReport(final ReportType<T, R, P> reportType, final P reportParameters) throws DatabaseException, ReportException {
    Objects.requireNonNull(reportType, "reportType");
    try {
      return handleResponse(execute(createRequest("report", Arrays.asList(reportType, reportParameters))));
    }
    catch (final ReportException | DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeBlob(final Key primaryKey, final Attribute<byte[]> blobAttribute, final byte[] blobData)
          throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobAttribute, "blobAttribute");
    Objects.requireNonNull(blobData, "blobData");
    try {
      handleResponse(execute(createRequest("writeBlob", Arrays.asList(primaryKey, blobAttribute, blobData))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] readBlob(final Key primaryKey, final Attribute<byte[]> blobAttribute) throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobAttribute, "blobAttribute");
    try {
      return handleResponse(execute(createRequest("readBlob", Arrays.asList(primaryKey, blobAttribute))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private Entities initializeEntities() {
    try {
      return handleResponse(execute(createRequest("getEntities")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private HttpResponse execute(final HttpRequest operation) throws Exception {
    synchronized (httpClient) {
      return httpClient.send(operation, HttpResponse.BodyHandlers.ofByteArray());
    }
  }

  private HttpRequest createRequest(final String path) throws IOException {
    return createRequest(path, null);
  }

  private HttpRequest createRequest(final String path, final Object data) throws IOException {
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

  private static <T> T handleResponse(final HttpResponse response) throws Exception {
    if (response.statusCode() != HTTP_STATUS_OK) {
      throw (Exception) Serializer.deserialize((byte[]) response.body());
    }

    return Serializer.deserialize((byte[]) response.body());
  }

  private static final class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(final Runnable runnable) {
      final Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  }
}
