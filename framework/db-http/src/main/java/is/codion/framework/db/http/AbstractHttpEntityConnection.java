/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.http;

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
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.common.Serializer.deserialize;
import static is.codion.common.Serializer.serialize;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newFixedThreadPool;

abstract class AbstractHttpEntityConnection implements HttpEntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractHttpEntityConnection.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(), Locale.getDefault());

  static final Executor DEFAULT_EXECUTOR = newFixedThreadPool(getRuntime().availableProcessors() + 1, new DaemonThreadFactory());

  private static final String AUTHORIZATION = "Authorization";
  private static final String BASIC = "Basic ";
  private static final String DOMAIN_TYPE_NAME = "domainTypeName";
  private static final String CLIENT_TYPE_ID = "clientTypeId";
  private static final String CLIENT_ID = "clientId";
  private static final String HTTP = "http://";
  private static final String HTTPS = "https://";
  private static final int HTTP_STATUS_OK = 200;

  private final User user;
  protected final String baseurl;
  protected final HttpClient httpClient;
  protected final Entities entities;
  protected final String[] headers;
  protected final Duration socketTimeout;

  private boolean closed;

  /**
   * Instantiates a new {@link DefaultHttpEntityConnection} instance
   * @param builder the builder
   * @param path the path
   */
  AbstractHttpEntityConnection(DefaultBuilder builder, String path) {
    this.user = requireNonNull(builder.user, "user");
    this.baseurl = createBaseUrl(builder, path);
    this.socketTimeout = Duration.ofMillis(builder.socketTimeout);
    this.httpClient = createHttpClient(builder.connectTimeout, builder.executor);
    this.headers = new String[] {
            DOMAIN_TYPE_NAME, requireNonNull(builder.domainType, "domainType").name(),
            CLIENT_TYPE_ID, requireNonNull(builder.clientTypeId, CLIENT_TYPE_ID),
            CLIENT_ID, requireNonNull(builder.clientId, CLIENT_ID).toString(),
            AUTHORIZATION, createAuthorizationHeader(user)
    };
    this.entities = initializeEntities();
  }

  @Override
  public final Entities entities() {
    return entities;
  }

  @Override
  public final User user() {
    return user;
  }

  @Override
  public final boolean connected() {
    synchronized (httpClient) {
      return !closed;
    }
  }

  @Override
  public final void close() {
    try {
      synchronized (httpClient) {
        handleResponse(execute(createRequest("close")));
        closed = true;
      }
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  @Override
  public final void beginTransaction() {
    try {
      synchronized (httpClient) {
        handleResponse(execute(createRequest("beginTransaction")));
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
  public final void rollbackTransaction() {
    try {
      synchronized (httpClient) {
        handleResponse(execute(createRequest("rollbackTransaction")));
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
  public final void commitTransaction() {
    try {
      synchronized (httpClient) {
        handleResponse(execute(createRequest("commitTransaction")));
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
  public final Entity select(Entity.Key key) throws DatabaseException {
    return selectSingle(key(key));
  }

  @Override
  public final Entity selectSingle(Condition condition) throws DatabaseException {
    return selectSingle(Select.where(condition).build());
  }

  @Override
  public final Entity selectSingle(Select select) throws DatabaseException {
    List<Entity> selected = select(select);
    if (nullOrEmpty(selected)) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (selected.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("many_records_found"));
    }

    return selected.get(0);
  }

  @Override
  public final <T> List<T> select(Column<T> column) throws DatabaseException {
    return select(requireNonNull(column), Select.all(column.entityType())
            .orderBy(ascending(column))
            .build());
  }

  @Override
  public final <T> List<T> select(Column<T> column, Condition condition) throws DatabaseException {
    return select(column, Select.where(condition)
            .orderBy(ascending(column))
            .build());
  }

  @Override
  public final List<Entity> select(Condition condition) throws DatabaseException {
    return select(Select.where(condition).build());
  }

  @Override
  public final Entity.Key insert(Entity entity) throws DatabaseException {
    return insert(singletonList(entity)).iterator().next();
  }

  @Override
  public final Entity insertSelect(Entity entity) throws DatabaseException {
    return insertSelect(singletonList(entity)).iterator().next();
  }

  @Override
  public final void update(Entity entity) throws DatabaseException {
    update(singletonList(requireNonNull(entity, "entity")));
  }

  @Override
  public final Entity updateSelect(Entity entity) throws DatabaseException {
    return updateSelect(singletonList(entity)).iterator().next();
  }

  @Override
  public final void delete(Entity.Key entityKey) throws DatabaseException {
    delete(singletonList(entityKey));
  }

  @Override
  public final <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType) throws DatabaseException {
    return execute(functionType, null);
  }

  @Override
  public final <C extends EntityConnection, T, R> R execute(FunctionType<C, T, R> functionType, T argument) throws DatabaseException {
    requireNonNull(functionType);
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("function", serialize(asList(functionType, argument)))));
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
  public final <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType) throws DatabaseException {
    execute(procedureType, null);
  }

  @Override
  public final <C extends EntityConnection, T> void execute(ProcedureType<C, T> procedureType, T argument) throws DatabaseException {
    requireNonNull(procedureType);
    try {
      synchronized (httpClient) {
        handleResponse(execute(createRequest("procedure", serialize(asList(procedureType, argument)))));
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
  public final <T, R, P> R report(ReportType<T, R, P> reportType, P reportParameters) throws DatabaseException, ReportException {
    requireNonNull(reportType, "reportType");
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("report", serialize(asList(reportType, reportParameters)))));
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
  public final void writeBlob(Entity.Key primaryKey, Column<byte[]> blobColumn, byte[] blobData)
          throws DatabaseException {
    requireNonNull(primaryKey, "primaryKey");
    requireNonNull(blobColumn, "blobColumn");
    requireNonNull(blobData, "blobData");
    try {
      synchronized (httpClient) {
        handleResponse(execute(createRequest("writeBlob", serialize(asList(primaryKey, blobColumn, blobData)))));
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
  public final byte[] readBlob(Entity.Key primaryKey, Column<byte[]> blobColumn) throws DatabaseException {
    requireNonNull(primaryKey, "primaryKey");
    requireNonNull(blobColumn, "blobColumn");
    try {
      synchronized (httpClient) {
        return handleResponse(execute(createRequest("readBlob", serialize(asList(primaryKey, blobColumn)))));
      }
    }
    catch (DatabaseException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  private Entities initializeEntities() {
    try {
      return handleResponse(execute(createRequest("entities")));
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw logAndWrap(e);
    }
  }

  protected final <T> HttpResponse<T> execute(HttpRequest operation) throws IOException, InterruptedException {
    synchronized (httpClient) {
      return (HttpResponse<T>) httpClient.send(operation, BodyHandlers.ofByteArray());
    }
  }

  protected final HttpRequest createRequest(String path) {
    return HttpRequest.newBuilder()
            .uri(URI.create(baseurl + path))
            .POST(BodyPublishers.noBody())
            .headers(headers)
            .build();
  }

  protected final HttpRequest createRequest(String path, byte[] data) {
    return HttpRequest.newBuilder()
            .uri(URI.create(baseurl + path))
            .POST(BodyPublishers.ofByteArray(data))
            .headers(headers)
            .build();
  }

  private static HttpClient createHttpClient(int connectTimeout, Executor executor) {
    return HttpClient.newBuilder()
            .executor(executor)
            .cookieHandler(new CookieManager())
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .build();
  }

  protected static <T> T handleResponse(HttpResponse<T> response) throws Exception {
    throwIfError(response);

    return deserialize((byte[]) response.body());
  }

  protected static void throwIfError(HttpResponse<?> response) throws Exception {
    if (response.statusCode() != HTTP_STATUS_OK) {
      throw (Exception) deserialize((byte[]) response.body());
    }
  }

  protected static RuntimeException logAndWrap(Exception e) {
    LOG.error(e.getMessage(), e);

    return new RuntimeException(e);
  }

  private static String createBaseUrl(DefaultBuilder builder, String path) {
    return (builder.https ? HTTPS : HTTP) + requireNonNull(builder.hostName, "hostName") + ":" + (builder.https ? builder.securePort : builder.port) + path;
  }

  private static String createAuthorizationHeader(User user) {
    return BASIC + Base64.getEncoder().encodeToString((user.username() + ":" + String.valueOf(user.password())).getBytes());
  }

  private static class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  }

  static final class DefaultBuilder implements Builder {

    private DomainType domainType;
    private String hostName = HOSTNAME.get();
    private int port = PORT.get();
    private int securePort = SECURE_PORT.get();
    private boolean https = SECURE.get();
    private boolean json = JSON.get();
    private int socketTimeout = SOCKET_TIMEOUT.get();
    private int connectTimeout = CONNECT_TIMEOUT.get();
    private User user;
    private String clientTypeId;
    private UUID clientId;
    private Executor executor = DEFAULT_EXECUTOR;

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
    public Builder executor(Executor executor) {
      this.executor = requireNonNull(executor);
      return this;
    }

    @Override
    public EntityConnection build() {
      return json ? new JsonHttpEntityConnection(this) : new DefaultHttpEntityConnection(this);
    }
  }
}
