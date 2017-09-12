/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.db.http;

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

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/**
 * A Http based {@link EntityConnection} implementation based on EntityRESTService
 */
final class HttpEntityConnection implements EntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnection.class);

  private static final String DOMAIN_ID_PARAM = "domainId";
  private static final String PROPERTY_ID_PARAM = "propertyId";
  private static final String ENTITIES_PARAM = "entities";
  private static final String CONDITION_PARAM = "condition";
  private static final String FUNCTION_ID_PARAM = "functionId";
  private static final String PROCEDURE_ID_PARAM = "procedureId";
  private static final String PARAMETERS_PARAM = "parameters";
  private static final String REPORT_WRAPPER_PARAM = "reportWrapper";
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

  private final Entities domain;
  private final EntityConditions conditions;
  private final User user;
  private final String baseurl;

  private CloseableHttpClient httpClient;

  /**
   * Instantiates a new {@link HttpEntityConnection} instance
   * @param domain the domain entities
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   */
  HttpEntityConnection(final Entities domain, final String serverHostName, final int serverPort,
                       final User user, final String clientTypeId, final UUID clientId) {
    this.domain = domain;
    this.conditions = new EntityConditions(domain);
    this.user = user;
    this.baseurl =  serverHostName + ":" + serverPort + "/entities/";
    this.httpClient = createHttpClient(user, clientTypeId, clientId);
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
    return httpClient != null;
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    try {
      executeGet("disconnect");
      httpClient.close();
      httpClient = null;
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
      final HttpResponse response = executeGet("isTransactionOpen");
      final List result = Util.base64DecodeAndDeserialize(getContentStream(response.getEntity()));

      return (boolean) result.get(0);
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
      executeGet("beginTransaction");
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
      executeGet("rollbackTransaction");
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
      executeGet("commitTransaction");
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
    try {
      final HttpResponse response = executeOperation("function", FUNCTION_ID_PARAM, functionId, arguments);

      return Util.base64DecodeAndDeserialize(getContentStream(response.getEntity()));
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    try {
      executeOperation("procedure", PROCEDURE_ID_PARAM, procedureId, arguments);
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    try {
      final URIBuilder builder = createURIBuilder();
      builder.addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(ENTITIES_PARAM, Util.serializeAndBase64Encode(entities));
      final HttpResponse response = httpClient.execute(new HttpPost(builder.build()));
      ifExceptionThrow(response);

      return Util.base64DecodeAndDeserialize(getContentStream(response.getEntity()));
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    try {
      final URIBuilder builder = createURIBuilder();
      builder.addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(ENTITIES_PARAM, Util.serializeAndBase64Encode(entities));
      final HttpResponse response = httpClient.execute(new HttpPut(builder.build()));
      ifExceptionThrow(response);

      return Util.base64DecodeAndDeserialize(getContentStream(response.getEntity()));
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    delete(conditions.condition(keys));
  }

  /** {@inheritDoc} */
  @Override
  public void delete(final EntityCondition condition) throws DatabaseException {
    try {
      final URIBuilder builder = createURIBuilder();
      builder.addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(CONDITION_PARAM, Util.serializeAndBase64Encode(Collections.singletonList(condition)));
      final HttpResponse response  = httpClient.execute(new HttpDelete(builder.build()));
      ifExceptionThrow(response);
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    try {
      final URIBuilder builder = createURIBuilder();
      builder.setPath("values")
              .addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(PROPERTY_ID_PARAM, propertyId)
              .addParameter(CONDITION_PARAM, Util.serializeAndBase64Encode(Collections.singletonList(condition)));
      final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
      ifExceptionThrow(response);

      return Util.base64DecodeAndDeserialize(getContentStream(response.getEntity()));
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    return selectSingle(conditions.selectCondition(entityId, propertyId, Condition.Type.LIKE, value));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final Entity.Key key) throws DatabaseException {
    return selectSingle(conditions.selectCondition(key));
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final EntitySelectCondition condition) throws DatabaseException {
    final List<Entity> entities = selectMany(condition);
    if (entities.isEmpty()) {
      throw new RecordNotFoundException(FrameworkMessages.get(FrameworkMessages.RECORD_NOT_FOUND));
    }
    if (entities.size() > 1) {
      throw new DatabaseException(FrameworkMessages.get(FrameworkMessages.MANY_RECORDS_FOUND));
    }

    return entities.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final List<Entity.Key> keys) throws DatabaseException {
    return selectMany(conditions.selectCondition(keys));
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final EntitySelectCondition condition) throws DatabaseException {
    try {
      final URIBuilder builder = createURIBuilder();
      builder.addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(CONDITION_PARAM, Util.serializeAndBase64Encode(Collections.singletonList(condition)));
      final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
      ifExceptionThrow(response);

      return Util.base64DecodeAndDeserialize(getContentStream(response.getEntity()));
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    return selectMany(conditions.selectCondition(entityId, propertyId, Condition.Type.LIKE, Arrays.asList(values)));
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws DatabaseException {
    try {
      final URIBuilder builder = createURIBuilder();
      builder.setPath("dependencies")
              .addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(ENTITIES_PARAM, Util.serializeAndBase64Encode(new ArrayList<>(entities)));
      final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
      ifExceptionThrow(response);

      final List<Map<String, Collection<Entity>>> dependencies =
              Util.<Map<String, Collection<Entity>>>base64DecodeAndDeserialize(getContentStream(response.getEntity()));

      if (dependencies.isEmpty()) {
        return Collections.emptyMap();
      }

      return dependencies.get(0);
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    try {
      final URIBuilder builder = createURIBuilder();
      builder.setPath("count")
              .addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(CONDITION_PARAM, Util.serializeAndBase64Encode(Collections.singletonList(condition)));
      final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
      ifExceptionThrow(response);

      return Util.<Integer>base64DecodeAndDeserialize(getContentStream(response.getEntity())).get(0);
    }
    catch (final DatabaseException e) {
      LOG.error(e.getMessage(), e);
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
    try {
      final URIBuilder builder = createURIBuilder();
      builder.setPath("report")
              .addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(REPORT_WRAPPER_PARAM, Util.serializeAndBase64Encode(Collections.singletonList(reportWrapper)));
      final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
      ifExceptionThrow(response);

      return Util.<ReportResult>base64DecodeAndDeserialize(getContentStream(response.getEntity())).get(0);
    }
    catch (final ReportException | DatabaseException e) {
      LOG.error(e.getMessage(), e);
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

  private HttpResponse executeGet(final String path) throws Exception {
    final URIBuilder builder = createURIBuilder();
    builder.setPath(path)
            .addParameter(DOMAIN_ID_PARAM, domain.getDomainId());
    final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
    ifExceptionThrow(response);

    return response;
  }

  private HttpResponse executeOperation(final String path, final String operationIdParam, final String operationId,
                                        final Object... arguments) throws Exception {
    final URIBuilder builder = createURIBuilder();
    builder.setPath(path)
            .addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
            .addParameter(operationIdParam, operationId)
            .addParameter(PARAMETERS_PARAM, Util.serializeAndBase64Encode(
                    Util.notNull(arguments) ? Arrays.asList(arguments) : Collections.emptyList()));
    final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
    ifExceptionThrow(response);

    return response;
  }

  private static void ifExceptionThrow(final HttpResponse response) throws Exception {
    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      final List<Exception> exceptionList = Util.base64DecodeAndDeserialize(
              getContentStream(response.getEntity()));
      if (!exceptionList.isEmpty()) {
        throw exceptionList.get(0);
      }

      throw new Exception("Error from server: " + getContentStream(response.getEntity()));
    }
  }

  private static String getContentStream(final HttpEntity entity) throws IOException {
    Scanner scanner = null;
    try (final InputStream stream = entity.getContent()) {
      scanner = new Scanner(stream).useDelimiter("\\A");

      return scanner.hasNext() ? scanner.next() : "";
    }
    finally {
      if (scanner != null) {
        scanner.close();
      }
      EntityUtils.consume(entity);
    }
  }

  private static CloseableHttpClient createHttpClient(final User user, final String clientTypeId, final UUID clientId) {
    final String authorizationHeader = BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + user.getPassword()).getBytes());

    return HttpClientBuilder.create()
            .setDefaultRequestConfig(REQUEST_CONFIG)
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(CLIENT_ID, clientId.toString());
              request.setHeader(AUTHORIZATION, authorizationHeader);
              request.setHeader(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
            })
            .build();
  }

  private URIBuilder createURIBuilder() {
    return new URIBuilder().setScheme(HTTP).setHost(baseurl);
  }
}
