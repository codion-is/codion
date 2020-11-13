/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.Serializer;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entities;

import org.apache.http.HttpEntity;
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
import java.util.Objects;
import java.util.UUID;

abstract class AbstractHttpEntityConnection implements EntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractHttpEntityConnection.class);

  private static final String DOMAIN_TYPE_NAME = "domainTypeName";
  private static final String CLIENT_TYPE_ID = "clientTypeId";
  private static final String CLIENT_ID = "clientId";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String HTTP = "http";
  private static final String HTTPS = "https";

  private final RequestConfig requestConfig = RequestConfig.custom()
          .setSocketTimeout(2000)
          .setConnectTimeout(2000)
          .build();

  private final String domainTypeName;
  private final User user;
  private final boolean httpsEnabled;
  private final String baseurl;
  private final HttpClientConnectionManager connectionManager;
  private final CloseableHttpClient httpClient;
  private final HttpHost targetHost;
  private final HttpClientContext httpContext;

  protected final Entities entities;

  private boolean closed;

  /**
   * Instantiates a new {@link HttpJsonEntityConnection} instance
   * @param domainTypeName the name of the domain model type
   * @param serverHostName the http server host name
   * @param serverPort the http server port
   * @param httpsEnabled if true then https is used
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   * @param contentType the content type string
   * @param path the path
   * @param connectionManager the connection manager
   */
  AbstractHttpEntityConnection(final String domainTypeName, final String serverHostName, final int serverPort,
                               final ClientHttps httpsEnabled, final User user, final String clientTypeId, final UUID clientId,
                               final String contentType, final String path, final HttpClientConnectionManager connectionManager) {
    this.domainTypeName = Objects.requireNonNull(domainTypeName, DOMAIN_TYPE_NAME);
    this.user = Objects.requireNonNull(user, "user");
    this.httpsEnabled = ClientHttps.TRUE.equals(httpsEnabled);
    this.baseurl = Objects.requireNonNull(serverHostName, "serverHostName") + ":" + serverPort + path;
    this.connectionManager = Objects.requireNonNull(connectionManager, "connectionManager");
    this.httpClient = createHttpClient(clientTypeId, clientId, contentType);
    this.targetHost = new HttpHost(serverHostName, serverPort, this.httpsEnabled ? HTTPS : HTTP);
    this.httpContext = createHttpContext(user, targetHost);
    this.entities = initializeEntities();
  }

  @Override
  public final Entities getEntities() {
    return entities;
  }

  @Override
  public final User getUser() {
    return user;
  }

  @Override
  public final boolean isConnected() {
    return !closed;
  }

  @Override
  public final void close() {
    try {
      onResponse(execute(createHttpPost("close")));
      connectionManager.shutdown();
      httpClient.close();
      closed = true;
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  protected final CloseableHttpResponse execute(final HttpUriRequest operation) throws IOException {
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

  protected final HttpPost createHttpPost(final String path) throws URISyntaxException {
    return createHttpPost(path, null);
  }

  protected final HttpPost createHttpPost(final String path, final HttpEntity data) throws URISyntaxException {
    final HttpPost post = new HttpPost(createURIBuilder(path).build());
    if (data != null) {
      post.setEntity(data);
    }

    return post;
  }

  protected final URIBuilder createURIBuilder(final String path) {
    return new URIBuilder().setScheme(httpsEnabled ? HTTPS : HTTP).setHost(baseurl).setPath(path);
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

  protected static RuntimeException logAndWrap(final Exception e) {
    LOG.error(e.getMessage(), e);

    return new RuntimeException(e);
  }

  protected static <T> T onResponse(final CloseableHttpResponse closeableHttpResponse) throws Exception {
    try (final CloseableHttpResponse response = closeableHttpResponse) {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      response.getEntity().writeTo(outputStream);
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw Serializer.<Exception>deserialize(outputStream.toByteArray());
      }

      return Serializer.deserialize(outputStream.toByteArray());
    }
  }

  private CloseableHttpClient createHttpClient(final String clientTypeId, final UUID clientId, final String contentType) {
    final String clientIdString = clientId.toString();

    return HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
              request.setHeader(DOMAIN_TYPE_NAME, domainTypeName);
              request.setHeader(CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(CLIENT_ID, clientIdString);
              request.setHeader(CONTENT_TYPE, contentType);
            })
            .build();
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
