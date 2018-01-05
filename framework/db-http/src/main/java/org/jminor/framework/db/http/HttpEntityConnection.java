/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.http;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A Http based {@link EntityConnection} implementation based on EntityService
 */
final class HttpEntityConnection implements EntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnection.class);

  private static final String PROPERTY_ID_PARAM = "propertyId";
  private static final String FUNCTION_ID_PARAM = "functionId";
  private static final String PROCEDURE_ID_PARAM = "procedureId";
  private static final String DOMAIN_ID = "domainId";
  private static final String CLIENT_TYPE_ID = "clientTypeId";
  private static final String CLIENT_ID = "clientId";
  private static final String AUTHORIZATION = "Authorization";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  private static final String BASIC = "Basic ";
  private static final String HTTP = "http";

  private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
          .setSocketTimeout(2000)
          .setConnectTimeout(2000)
          .build();

  private final String domainId;
  private final User user;
  private final String baseurl;
  private final BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager();
  private final CloseableHttpClient httpClient;

  private Entities entities;
  private EntityConditions conditions;

  private boolean closed;

  /**
   * Instantiates a new {@link HttpEntityConnection} instance
   * @param domain the entities entities
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   */
  HttpEntityConnection(final String domainId, final String serverHostName, final int serverPort,
                       final User user, final String clientTypeId, final UUID clientId) {
    this.domainId = Objects.requireNonNull(domainId, DOMAIN_ID);
    this.user = Objects.requireNonNull(user, "user");
    this.baseurl =  Objects.requireNonNull(serverHostName, "serverHostName") + ":" + serverPort + "/entities/";
    this.httpClient = createHttpClient(clientTypeId, clientId);
  }

  /** {@inheritDoc} */
  @Override
  public Entities getEntities() {
    if (entities == null) {
      entities = initializeEntities();
    }

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public void setMethodLogger(final MethodLogger methodLogger) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnection.Type getType() {
    return EntityConnection.Type.HTTP;
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
    delete(getConditions().condition(Objects.requireNonNull(keys)));
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
    return selectSingle(getConditions().selectCondition(entityId, propertyId, Condition.Type.LIKE, value));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(getConditions().selectCondition(key));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException {
    final List<Entity> selected = selectMany(condition);
    if (Util.nullOrEmpty(selected)) {
      throw new RecordNotFoundException(FrameworkMessages.get(FrameworkMessages.RECORD_NOT_FOUND));
    }
    if (selected.size() > 1) {
      throw new DatabaseException(FrameworkMessages.get(FrameworkMessages.MANY_RECORDS_FOUND));
    }

    return selected.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException {
    return selectMany(getConditions().selectCondition(keys));
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
    return selectMany(getConditions().selectCondition(entityId, propertyId, Condition.Type.LIKE, Arrays.asList(values)));
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
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseConnection getDatabaseConnection() {
    throw new UnsupportedOperationException();
  }

  private EntityConditions getConditions() {
    if (conditions == null) {
      conditions = new EntityConditions(getEntities());
    }

    return conditions;
  }

  private Entities initializeEntities() {
    try {
      return handleResponse(execute(createHttpPost("getEntities")));
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
            Util.notNull(arguments) ? Arrays.asList(arguments) : Collections.emptyList())));
  }

  private CloseableHttpResponse execute(final HttpUriRequest operation) throws IOException {
    synchronized (httpClient) {
      try {
        return httpClient.execute(operation);
      }
      catch (final NoHttpResponseException e) {
        LOG.debug(e.getMessage(), e);
        //retry once, todo fix server side if possible
        return httpClient.execute(operation);
      }
    }
  }

  private CloseableHttpClient createHttpClient(final String clientTypeId, final UUID clientId) {
    final String authorizationHeader = BASIC + Base64.getEncoder().encodeToString((
            user.getUsername() + ":" + user.getPassword()).getBytes());
    final String clientIdString = clientId.toString();

    return HttpClientBuilder.create()
            .setDefaultRequestConfig(REQUEST_CONFIG)
            .setConnectionManager(connectionManager)
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(DOMAIN_ID, domainId);
              request.setHeader(CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(CLIENT_ID, clientIdString);
              request.setHeader(AUTHORIZATION, authorizationHeader);
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

  private HttpPost createHttpPost(final URIBuilder uriBuilder, final Object data) throws URISyntaxException, IOException {
    final HttpPost post = new HttpPost(uriBuilder.build());
    if (data != null) {
      post.setEntity(new ByteArrayEntity(Util.serialize(data)));
    }

    return post;
  }

  private URIBuilder createURIBuilder(final String path) {
    return new URIBuilder().setScheme(HTTP).setHost(baseurl).setPath(path);
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
}
