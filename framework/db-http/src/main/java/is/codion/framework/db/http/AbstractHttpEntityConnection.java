/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.Serializer;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;
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
import org.apache.http.client.HttpRequestRetryHandler;
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
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

abstract class AbstractHttpEntityConnection implements HttpEntityConnection {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractHttpEntityConnection.class);

  private static final String DOMAIN_TYPE_NAME = "domainTypeName";
  private static final String CLIENT_TYPE_ID = "clientTypeId";
  private static final String CLIENT_ID = "clientId";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String HTTP = "http";
  private static final String HTTPS = "https";

  private final RequestConfig requestConfig;
  private final HttpClientConnectionManager connectionManager;

  private final DomainType domainType;
  private final User user;
  private final boolean httpsEnabled;
  private final String baseurl;
  private final CloseableHttpClient httpClient;
  private final HttpHost targetHost;
  private final HttpClientContext httpContext;

  protected final Entities entities;

  private boolean closed;

  /**
   * Instantiates a new {@link JsonHttpEntityConnection} instance
   * @param domainType the domain model type
   * @param hostName the http server host name
   * @param user the user
   * @param clientTypeId the client type id
   * @param clientId the client id
   * @param contentType the content type string
   * @param path the path
   * @param port the http server port
   * @param securePort the https server port
   * @param httpsEnabled if true then https is used
   * @param socketTimeout the socket timeout
   * @param connectTimeout the connect timeout
   * @param connectionManager the connection manager
   */
  AbstractHttpEntityConnection(DefaultBuilder builder, String contentType, String path, HttpClientConnectionManager connectionManager) {
    this.domainType = requireNonNull(builder.domainType, "domainType");
    this.baseurl = requireNonNull(builder.hostName, "hostName") + ":" + (builder.https ? builder.securePort : builder.port) + path;
    this.connectionManager = requireNonNull(connectionManager);
    this.user = requireNonNull(builder.user, "user");
    this.requestConfig = RequestConfig.custom()
          .setSocketTimeout(builder.socketTimeout)
          .setConnectTimeout(builder.connectTimeout)
          .build();
    this.httpClient = createHttpClient(requireNonNull(builder.clientTypeId), requireNonNull(builder.clientId).toString(), requireNonNull(contentType));
    this.targetHost = new HttpHost(builder.hostName, (builder.https ? builder.securePort : builder.port), builder.https ? HTTPS : HTTP);
    this.httpContext = createHttpContext(builder.user, targetHost);
    this.httpsEnabled = builder.https;
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
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  protected final CloseableHttpResponse execute(HttpUriRequest operation) throws IOException {
    return httpClient.execute(targetHost, operation, httpContext);
  }

  protected final HttpPost createHttpPost(String path) throws URISyntaxException {
    return createHttpPost(path, null);
  }

  protected final HttpPost createHttpPost(String path, HttpEntity data) throws URISyntaxException {
    HttpPost post = new HttpPost(createURIBuilder(path).build());
    if (data != null) {
      post.setEntity(data);
    }

    return post;
  }

  protected final URIBuilder createURIBuilder(String path) {
    return new URIBuilder().setScheme(httpsEnabled ? HTTPS : HTTP).setHost(baseurl).setPath(path);
  }

  private Entities initializeEntities() {
    try {
      return onResponse(execute(createHttpPost("entities")));
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  protected static RuntimeException logAndWrap(Exception e) {
    LOG.error(e.getMessage(), e);

    return new RuntimeException(e);
  }

  protected static <T> T onResponse(CloseableHttpResponse closeableHttpResponse) throws Exception {
    try (CloseableHttpResponse response = closeableHttpResponse) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      response.getEntity().writeTo(outputStream);
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        throw Serializer.<Exception>deserialize(outputStream.toByteArray());
      }

      return Serializer.deserialize(outputStream.toByteArray());
    }
  }

  private CloseableHttpClient createHttpClient(String clientTypeId, String clientId, String contentType) {
    return HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .setRetryHandler(new RetryHandler())
            .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
              request.setHeader(DOMAIN_TYPE_NAME, domainType.name());
              request.setHeader(CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(CLIENT_ID, clientId);
              request.setHeader(CONTENT_TYPE, contentType);
            })
            .build();
  }

  private static HttpClientContext createHttpContext(User user, HttpHost targetHost) {
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
            new AuthScope(targetHost.getHostName(), targetHost.getPort()),
            new UsernamePasswordCredentials(user.username(), String.valueOf(user.password())));

    AuthCache authCache = new BasicAuthCache();
    authCache.put(targetHost, new BasicScheme());

    HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider(credentialsProvider);
    context.setAuthCache(authCache);

    return context;
  }

  /**
   * A single automatic retry for a NoHttpResponseException.
   */
  private static final class RetryHandler implements HttpRequestRetryHandler {

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
      return executionCount < 2 && exception instanceof NoHttpResponseException;
    }
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
        return new JsonHttpEntityConnection(this, new BasicHttpClientConnectionManager());
      }

      return new DefaultHttpEntityConnection(this, new BasicHttpClientConnectionManager());
    }
  }
}
