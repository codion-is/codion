/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.http;

import dev.codion.common.Serializer;
import dev.codion.common.Util;
import dev.codion.common.db.Operator;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.db.exception.MultipleRecordsFoundException;
import dev.codion.common.db.exception.RecordNotFoundException;
import dev.codion.common.db.reports.ReportException;
import dev.codion.common.db.reports.ReportWrapper;
import dev.codion.common.user.User;
import dev.codion.framework.db.EntityConnection;
import dev.codion.framework.db.condition.EntityCondition;
import dev.codion.framework.db.condition.EntitySelectCondition;
import dev.codion.framework.db.condition.EntityUpdateCondition;
import dev.codion.framework.domain.entity.Entities;
import dev.codion.framework.domain.entity.Entity;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

import static dev.codion.framework.db.condition.Conditions.selectCondition;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService
 */
final class HttpEntityConnection implements EntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(),
          Locale.getDefault());

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnection.class);

  private static final String DOMAIN_ID = "domainId";
  private static final String CLIENT_TYPE_ID = "clientTypeId";
  private static final String CLIENT_ID = "clientId";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  private static final String HTTP = "http";
  private static final String HTTPS = "https";

  private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
          .setSocketTimeout(2000)
          .setConnectTimeout(2000)
          .build();

  private final String domainId;
  private final User user;
  private final boolean httpsEnabled;
  private final String baseurl;
  private final HttpClientConnectionManager connectionManager;
  private final CloseableHttpClient httpClient;
  private final HttpHost targetHost;
  private final HttpClientContext httpContext;

  private final Entities entities;

  private boolean closed;

  /**
   * Instantiates a new {@link HttpEntityConnection} instance
   * @param domainId the id of the domain model
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param httpsEnabled if true then https is used
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   */
  HttpEntityConnection(final String domainId, final String serverHostName, final int serverPort,
                       final boolean httpsEnabled, final User user, final String clientTypeId, final UUID clientId,
                       final HttpClientConnectionManager connectionManager) {
    this.domainId = Objects.requireNonNull(domainId, DOMAIN_ID);
    this.user = Objects.requireNonNull(user, "user");
    this.httpsEnabled = httpsEnabled;
    this.baseurl = Objects.requireNonNull(serverHostName, "serverHostName") + ":" + serverPort + "/entities";
    this.connectionManager = Objects.requireNonNull(connectionManager, "connectionManager");
    this.httpClient = createHttpClient(clientTypeId, clientId);
    this.targetHost = new HttpHost(serverHostName, serverPort, httpsEnabled ? HTTPS : HTTP);
    this.httpContext = createHttpContext(user, targetHost);
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
  public void disconnect() {
    try {
      onResponse(execute(createHttpPost("disconnect")));
      connectionManager.shutdown();
      httpClient.close();
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
      return onResponse(execute(createHttpPost("isTransactionOpen")));
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
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
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T executeFunction(final String functionId, final Object... arguments) throws DatabaseException {
    Objects.requireNonNull(functionId);
    try {
      return executeOperation("function", "functionId", functionId, arguments);
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
  public void executeProcedure(final String procedureId, final Object... arguments) throws DatabaseException {
    Objects.requireNonNull(procedureId);
    try {
      executeOperation("procedure", "procedureId", procedureId, arguments);
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
  public Entity.Key insert(final Entity entity) throws DatabaseException {
    return insert(singletonList(entity)).get(0);
  }

  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      return onResponse(execute(createHttpPost("insert", entities)));
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
  public List<Entity> update(final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      return onResponse(execute(createHttpPost("update", entities)));
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
  public int update(final EntityUpdateCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onResponse(execute(createHttpPost("updateByCondition", condition)));
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
  public boolean delete(final Entity.Key entityKey) throws DatabaseException {
    return delete(singletonList(entityKey)) == 1;
  }

  @Override
  public int delete(final List<Entity.Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys);
    try {
      return onResponse(execute(createHttpPost("deleteByKey", keys)));
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
  public int delete(final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onResponse(execute(createHttpPost("delete", condition)));
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
  public <T> List<T> selectValues(final String propertyId, final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(propertyId);
    Objects.requireNonNull(condition);
    try {
      return onResponse(execute(createHttpPost(createURIBuilder("values")
              .addParameter("propertyId", propertyId), condition)));
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
  public Entity selectSingle(final String entityId, final String propertyId, final Object value) throws DatabaseException {
    return selectSingle(selectCondition(entityId, propertyId, Operator.LIKE, value));
  }

  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(selectCondition(key));
  }

  @Override
  public Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException {
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
  public List<Entity> select(final List<Entity.Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys, "keys");
    try {
      return onResponse(execute(createHttpPost("selectByKey", keys)));
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
  public List<Entity> select(final EntitySelectCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition, "condition");
    try {
      return onResponse(execute(createHttpPost("select", condition)));
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
  public List<Entity> select(final String entityId, final String propertyId, final Object... values)
          throws DatabaseException {
    return select(selectCondition(entityId, propertyId, Operator.LIKE, asList(values)));
  }

  @Override
  public Map<String, Collection<Entity>> selectDependencies(final Collection<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      return onResponse(execute(createHttpPost("dependencies", entities)));
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
  public int selectRowCount(final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return onResponse(execute(createHttpPost("count", condition)));
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
  public <T, R, P> R fillReport(final ReportWrapper<T, R, P> reportWrapper, final P reportParameters) throws DatabaseException, ReportException {
    Objects.requireNonNull(reportWrapper, "reportWrapper");
    try {
      return onResponse(execute(createHttpPost("report", asList(reportWrapper, reportParameters))));
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
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyId, final byte[] blobData)
          throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobPropertyId, "blobPropertyId");
    Objects.requireNonNull(blobData, "blobData");
    try {
      onResponse(execute(createHttpPost("writeBlob", asList(primaryKey, blobPropertyId, blobData))));
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
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobPropertyId, "blobPropertyId");
    try {
      return onResponse(execute(createHttpPost("readBlob", asList(primaryKey, blobPropertyId))));
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
      return onResponse(execute(createHttpPost("getEntities")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private <T> T executeOperation(final String path, final String operationIdParam, final String operationId,
                                 final Object... arguments) throws Exception {
    return onResponse(execute(createHttpPost(createURIBuilder(path)
                    .addParameter(operationIdParam, operationId),
            Util.notNull(arguments) ? asList(arguments) : emptyList())));
  }

  private CloseableHttpResponse execute(final HttpUriRequest operation) throws IOException {
    synchronized (httpClient) {
      try {
        return httpClient.execute(targetHost, operation, httpContext);
      }
      catch (final NoHttpResponseException e) {
        LOG.debug(e.getMessage(), e);
        //retry once, todo fix server side if possible
        return httpClient.execute(targetHost, operation, httpContext);
      }
    }
  }

  private CloseableHttpClient createHttpClient(final String clientTypeId, final UUID clientId) {
    final String clientIdString = clientId.toString();

    return HttpClientBuilder.create()
            .setDefaultRequestConfig(REQUEST_CONFIG)
            .setConnectionManager(connectionManager)
            .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
              request.setHeader(DOMAIN_ID, domainId);
              request.setHeader(CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(CLIENT_ID, clientIdString);
              request.setHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
            })
            .build();
  }

  private HttpPost createHttpPost(final String path) throws URISyntaxException, IOException {
    return createHttpPost(path, null);
  }

  private HttpPost createHttpPost(final String path, final Object data) throws URISyntaxException, IOException {
    return createHttpPost(createURIBuilder(path), data);
  }

  private URIBuilder createURIBuilder(final String path) {
    return new URIBuilder().setScheme(httpsEnabled ? HTTPS : HTTP).setHost(baseurl).setPath(path);
  }

  private static HttpPost createHttpPost(final URIBuilder uriBuilder, final Object data) throws URISyntaxException, IOException {
    final HttpPost post = new HttpPost(uriBuilder.build());
    if (data != null) {
      post.setEntity(new ByteArrayEntity(Serializer.serialize(data)));
    }

    return post;
  }

  private static <T> T onResponse(final CloseableHttpResponse closeableHttpResponse) throws Exception {
    try (final CloseableHttpResponse response = closeableHttpResponse) {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      response.getEntity().writeTo(outputStream);
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw Serializer.<Exception>deserialize(outputStream.toByteArray());
      }

      return Serializer.deserialize(outputStream.toByteArray());
    }
  }

  private static HttpClientContext createHttpContext(final User user, final HttpHost targetHost) {
    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
            new AuthScope(targetHost.getHostName(), targetHost.getPort()),
            new UsernamePasswordCredentials(user.getUsername(), String.valueOf(user.getPassword())));

    final AuthCache authCache = new BasicAuthCache();
    authCache.put(targetHost, new BasicScheme());

    final HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider(credentialsProvider);
    context.setAuthCache(authCache);

    return context;
  }
}
