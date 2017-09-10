/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.db.http;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.RecordNotFoundException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.common.server.Server;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.plugins.json.EntityJSONParser;

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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * A Http based {@link EntityConnection} implementation based on EntityRESTService
 */
public final class DefaultHttpEntityConnection implements HttpEntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEntityConnection.class);

  private static final String DOMAIN_ID_PARAM = "domainId";
  private static final String ENTITIES_PARAM = "entities";
  private static final String KEYS_PARAM = "keys";
  private static final String ENTITY_ID_PARAM = "entityId";
  private static final String CONDITION_TYPE_PARAM = "conditionType";
  private static final String VALUES_PARAM = "values";

  private static final String BY_KEY_PATH = "key";
  private static final String BY_VALUE_PATH = "value";
  private static final String AUTHORIZATION = "Authorization";

  private static final String BASIC = "Basic ";
  private static final String HTTP = "http";
  private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
          .setSocketTimeout(2000)
          .setConnectTimeout(2000)
          .build();

  private final String baseurl;
  private final Entities domain;
  private final User user;

  private CloseableHttpClient httpClient;

  /**
   * Instantiates a new {@link DefaultHttpEntityConnection} instance
   * @param domain the domain entities
   * @param user the user
   */
  public DefaultHttpEntityConnection(final Entities domain, final User user) {
    this.domain = domain;
    this.user = user;
    this.baseurl = Server.SERVER_HOST_NAME.get() + ":" + WEB_SERVER_PORT.get() + "/entities/";
    final HttpRequestInterceptor requestInterceptor = (request, httpContext) -> {
      request.setHeader(AUTHORIZATION, BASIC +
              Base64.getEncoder().encodeToString((user.getUsername() + ":" + user.getPassword()).getBytes()));
      request.setHeader("Content-Type", "application/json");
    };
    this.httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(REQUEST_CONFIG)
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .addInterceptorFirst(requestInterceptor)
            .build();
  }

  /** {@inheritDoc} */
  @Override
  public void setMethodLogger(final MethodLogger methodLogger) throws IOException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public EntityConnection.Type getType() throws IOException {
    return EntityConnection.Type.HTTP;
  }

  /** {@inheritDoc} */
  @Override
  public User getUser() throws IOException {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConnected() throws IOException {
    return httpClient != null;
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() throws IOException {
    httpClient.close();
    httpClient = null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTransactionOpen() throws IOException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void beginTransaction() throws IOException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void rollbackTransaction() throws IOException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void commitTransaction() throws IOException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public List executeFunction(final String functionId, final Object... arguments) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void executeProcedure(final String procedureId, final Object... arguments) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity.Key> insert(final List<Entity> entities) throws IOException, DatabaseException {
    try {
      final EntityJSONParser parser = new EntityJSONParser(domain);
      final URIBuilder builder = createURIBuilder();
      builder.addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(ENTITIES_PARAM, parser.serialize(entities));
      final HttpResponse response = httpClient.execute(new HttpPost(builder.build()));
      checkResponse(response);

      return parser.deserializeKeys(getContentStream(response.getEntity()));
    }
    catch (final IOException e) {
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
  public List<Entity> update(final List<Entity> entities) throws IOException, DatabaseException {
    try {
      final EntityJSONParser parser = new EntityJSONParser(domain);
      final URIBuilder builder = createURIBuilder();
      builder.addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(ENTITIES_PARAM, parser.serialize(entities));
      final HttpResponse response = httpClient.execute(new HttpPut(builder.build()));
      checkResponse(response);

      return parser.deserializeEntities(getContentStream(response.getEntity()));
    }
    catch (final IOException e) {
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
  public void delete(final List<Entity.Key> keys) throws IOException, DatabaseException {
    try {
      final EntityJSONParser parser = new EntityJSONParser(domain);
      final URIBuilder builder = createURIBuilder();
      builder.setPath(BY_KEY_PATH)
              .addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(KEYS_PARAM, parser.serializeKeys(keys));
      final HttpResponse response  = httpClient.execute(new HttpDelete(builder.build()));
      checkResponse(response);
    }
    catch (final IOException e) {
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
  public void delete(final EntityCondition condition) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public List<Object> selectValues(final String propertyId, final EntityCondition condition) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Entity selectSingle(final String entityId, final String propertyId, final Object value) throws IOException, DatabaseException {
    final List<Entity> entities = selectMany(entityId, propertyId, value);
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
  public Entity selectSingle(final Entity.Key key) throws IOException, DatabaseException {
    final List<Entity> entities = selectMany(Collections.singletonList(key));
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
  public Entity selectSingle(final EntitySelectCondition condition) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final List<Entity.Key> keys) throws IOException, DatabaseException {
    try {
      final EntityJSONParser parser = new EntityJSONParser(domain);
      final URIBuilder builder = createURIBuilder();
      builder.setPath(BY_KEY_PATH)
              .addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(KEYS_PARAM, parser.serializeKeys(keys));
      final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
      checkResponse(response);

      return parser.deserializeEntities(getContentStream(response.getEntity()));
    }
    catch (final IOException e) {
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
  public List<Entity> selectMany(final EntitySelectCondition condition) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> selectMany(final String entityId, final String propertyId, final Object... values) throws
          IOException, DatabaseException {
    try {
      final EntityJSONParser parser = new EntityJSONParser(domain);
      final URIBuilder builder = createURIBuilder();
      builder.setPath(BY_VALUE_PATH)
              .addParameter(DOMAIN_ID_PARAM, domain.getDomainId())
              .addParameter(ENTITY_ID_PARAM, entityId)
              .addParameter(CONDITION_TYPE_PARAM, Condition.Type.LIKE.toString());
      if (propertyId != null) {
        final Property property = domain.getProperty(entityId, propertyId);

        final JSONObject jsonObject = new JSONObject();
        for (final Object value : values) {
          jsonObject.put(property.getPropertyId(), parser.serializeValue(value, property));
        }
        builder.addParameter(VALUES_PARAM, jsonObject.toString());
      }

      final HttpResponse response = httpClient.execute(new HttpGet(builder.build()));
      checkResponse(response);

      final String queryResult = getContentStream(response.getEntity());

      return parser.deserializeEntities(queryResult);
    }
    catch (final IOException e) {
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
  public Map<String, Collection<Entity>> selectDependentEntities(final Collection<Entity> entities) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public int selectRowCount(final EntityCondition condition) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public ReportResult fillReport(final ReportWrapper reportWrapper) throws IOException, DatabaseException, ReportException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void writeBlob(final Entity.Key primaryKey, final String blobPropertyId, final byte[] blobData)
          throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public byte[] readBlob(final Entity.Key primaryKey, final String blobPropertyId) throws IOException, DatabaseException {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseConnection getDatabaseConnection() throws IOException {
    throw new UnsupportedOperationException();
  }

  private URIBuilder createURIBuilder() {
    final URIBuilder builder = new URIBuilder();
    builder.setScheme(HTTP).setHost(baseurl);

    return builder;
  }

  private static void checkResponse(final HttpResponse response) throws Exception {
    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
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
}
