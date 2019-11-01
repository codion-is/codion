/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.http;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.ConditionType;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.MultipleRecordsFoundException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService
 */
final class HttpEntityConnection implements EntityConnection {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(HttpEntityConnection.class.getName(), Locale.getDefault());

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnection.class);

  private static final String PROPERTY_ID_PARAM = "propertyId";
  private static final String FUNCTION_ID_PARAM = "functionId";
  private static final String PROCEDURE_ID_PARAM = "procedureId";
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

  private final Domain domain;

  private boolean closed;

  /**
   * Instantiates a new {@link HttpEntityConnection} instance
   * @param domain the entities entities
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
    this.domain = initializeDomain();
  }

  /** {@inheritDoc} */
  @Override
  public Domain getDomain() {
    return domain;
  }

  /** {@inheritDoc} */
  @Override
  public User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConnected() {
    return !closed;
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    try {
      handleResponse(execute(createHttpPost("disconnect")));
      connectionManager.shutdown();
      httpClient.close();
      closed = true;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTransactionOpen() {
    try {
      return handleResponse(execute(createHttpPost("isTransactionOpen")));
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void beginTransaction() {
    try {
      handleResponse(execute(createHttpPost("beginTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void rollbackTransaction() {
    try {
      handleResponse(execute(createHttpPost("rollbackTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void commitTransaction() {
    try {
      handleResponse(execute(createHttpPost("commitTransaction")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List executeFunction(final String functionId, final Object... arguments) throws DatabaseException {
    Objects.requireNonNull(functionId);
    try {
      return executeOperation("function", FUNCTION_ID_PARAM, functionId, arguments);
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void executeProcedure(final String procedureId, final Object... arguments) throws DatabaseException {
    Objects.requireNonNull(procedureId);
    try {
      executeOperation("procedure", PROCEDURE_ID_PARAM, procedureId, arguments);
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      return handleResponse(execute(createHttpPost("insert", entities)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> update(final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    try {
      return handleResponse(execute(createHttpPost("update", entities)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final List<Entity.Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys);
    try {
      handleResponse(execute(createHttpPost("deleteByKey", keys)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      handleResponse(execute(createHttpPost("delete", condition)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Object> selectValues(final String propertyId, final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(propertyId);
    Objects.requireNonNull(condition);
    try {
      return handleResponse(execute(createHttpPost(createURIBuilder("values")
              .addParameter(PROPERTY_ID_PARAM, propertyId), condition)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final String entityId, final String propertyId, final Object value) throws DatabaseException {
    return selectSingle(entitySelectCondition(entityId, propertyId, ConditionType.LIKE, value));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(entitySelectCondition(key));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException {
    final List<Entity> selected = selectMany(condition);
    if (Util.nullOrEmpty(selected)) {
      throw new RecordNotFoundException(MESSAGES.getString("record_not_found"));
    }
    if (selected.size() > 1) {
      throw new MultipleRecordsFoundException(MESSAGES.getString("many_records_found"));
    }

    return selected.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException {
    Objects.requireNonNull(keys, "keys");
    try {
      return handleResponse(execute(createHttpPost("selectByKey", keys)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final EntitySelectCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition, "condition");
    try {
      return handleResponse(execute(createHttpPost("select", condition)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final String entityId, final String propertyId, final Object... values)
          throws DatabaseException {
    return selectMany(entitySelectCondition(entityId, propertyId, ConditionType.LIKE, asList(values)));
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, "entities");
    try {
      return handleResponse(execute(createHttpPost("dependencies", entities)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int selectRowCount(final EntityCondition condition) throws DatabaseException {
    Objects.requireNonNull(condition);
    try {
      return handleResponse(execute(createHttpPost("count", condition)));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws DatabaseException, ReportException {
    Objects.requireNonNull(reportWrapper, "reportWrapper");
    try {
      return handleResponse(execute(createHttpPost("report", reportWrapper)));
    }
    catch (final ReportException | DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyId, final byte[] blobData)
          throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobPropertyId, "blobPropertyId");
    Objects.requireNonNull(blobData, "blobData");
    try {
      handleResponse(execute(createHttpPost("writeBlob", asList(primaryKey, blobPropertyId, blobData))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws DatabaseException {
    Objects.requireNonNull(primaryKey, "primaryKey");
    Objects.requireNonNull(blobPropertyId, "blobPropertyId");
    try {
      return handleResponse(execute(createHttpPost("readBlob", asList(primaryKey, blobPropertyId))));
    }
    catch (final DatabaseException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private Domain initializeDomain() {
    try {
      return handleResponse(execute(createHttpPost("getDomain")));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private List executeOperation(final String path, final String operationIdParam, final String operationId,
                                final Object... arguments) throws Exception {
    return handleResponse(execute(createHttpPost(createURIBuilder(path)
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
      post.setEntity(new ByteArrayEntity(Util.serialize(data)));
    }

    return post;
  }

  private static <T> T handleResponse(final CloseableHttpResponse closeableHttpResponse) throws Exception {
    try (final CloseableHttpResponse response = closeableHttpResponse) {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      response.getEntity().writeTo(outputStream);
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw Util.<Exception>deserialize(outputStream.toByteArray());
      }

      return Util.deserialize(outputStream.toByteArray());
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
