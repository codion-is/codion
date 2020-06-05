/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.db.Operator;
import is.codion.common.db.database.Databases;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.common.http.server.ServerHttps;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static is.codion.framework.db.condition.Conditions.selectCondition;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServletServerTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = Users.parseUser("scott:tiger");
  private static final String HTTPS = "https";
  private static String HOSTNAME;
  private static HttpHost TARGET_HOST;
  private static String SERVER_BASEURL;

  private static EntityServer server;
  private static EntityServerAdmin admin;

  @BeforeAll
  public static void setUp() throws Exception {
    final EntityServerConfiguration configuration = configure();
    HOSTNAME = ServerConfiguration.SERVER_HOST_NAME.get();
    TARGET_HOST = new HttpHost(HOSTNAME, WEB_SERVER_PORT_NUMBER, HTTPS);
    SERVER_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities";
    server = EntityServer.startServer(configuration);
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    server.shutdown();
    deconfigure();
  }

  @Test
  public void isTransactionOpen() throws URISyntaxException, IOException, ClassNotFoundException {
    final RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(2000)
            .setConnectTimeout(2000)
            .build();
    final DomainType<?> domainType = new TestDomain().getDomainType();
    final String clientTypeId = "EntityServletServerTest";
    final UUID clientId = UUID.randomUUID();
    final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainType.getName());
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientId.toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();

    final URIBuilder uriBuilder = createURIBuilder();
    uriBuilder.setPath("isTransactionOpen");

    final HttpPost httpPost = new HttpPost(uriBuilder.build());
    final HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
    final CloseableHttpResponse response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    final Boolean result = deserializeResponse(response);
    assertFalse(result);
    response.close();
  }

  @Test
  public void test() throws URISyntaxException, IOException, InterruptedException, ClassNotFoundException {
    final RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(2000)
            .setConnectTimeout(2000)
            .build();
    CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .build();

    //test with missing authentication info
    URIBuilder uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    uriBuilder.addParameter("domainTypeName", ENTITIES.getDomainName());
    CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    final DomainType<?> domainType = new TestDomain().getDomainType();
    final String clientTypeId = "EntityServletServerTest";
    //test with missing clientId header
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainType.getName());
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
    response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()), context);
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    final Value<UUID> clientIdValue = Values.value(UUID.randomUUID());
    //test with unknown user authentication
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainType.getName());
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    context = createHttpContext(Users.user("who", "areu".toCharArray()), TARGET_HOST);
    response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()), context);
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainType.getName());
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();

    //select all/GET
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    HttpPost httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(selectCondition(TestDomain.T_DEPARTMENT))));
    context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    List<Entity> queryEntities = deserializeResponse(response);
    assertEquals(4, queryEntities.size());
    response.close();

    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, null);
    department.put(TestDomain.DEPARTMENT_ID, -42);
    department.put(TestDomain.DEPARTMENT_NAME, "Test");
    department.put(TestDomain.DEPARTMENT_LOCATION, "Location");

    //insert
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("insert");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(department))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    final List<Key> queryKeys = deserializeResponse(response);
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));
    response.close();

    //delete
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(selectCondition(department.getKey()))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    //insert
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("insert");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(department))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    final List<Key> keys = deserializeResponse(response);
    assertEquals(1, keys.size());
    assertEquals(department.getKey(), keys.get(0));
    response.close();

    //update
    department.saveAll();
    department.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    department.put(TestDomain.DEPARTMENT_NAME, "New name");
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("update");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(department))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));
    response.close();

    //select by condition
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(selectCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, Operator.LIKE, "New name"))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    response.close();

    //select by condition
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(selectCondition(department.getKey()))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    response.close();

    //delete
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(selectCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_ID, Operator.LIKE, -42))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("function");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(asList(TestDomain.FUNCTION_ID, new Object[0]))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(asList(TestDomain.PROCEDURE_ID, new Object[0]))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    Collection<RemoteClient> clients = admin.getClients(clientTypeId);
    assertEquals(1, clients.size());

    //try to change the clientId
    final UUID originalClientId = clientIdValue.get();
    clientIdValue.set(UUID.randomUUID());

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(asList(TestDomain.PROCEDURE_ID, new Object[0]))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();

    clientIdValue.set(originalClientId);

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("disconnect");
    response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()), context);
    response.close();

    client.close();

    clients = admin.getClients(clientTypeId);
    assertTrue(clients.isEmpty());
  }

  private static URIBuilder createURIBuilder() {
    final URIBuilder builder = new URIBuilder();
    builder.setScheme(HTTPS).setHost(SERVER_BASEURL);

    return builder;
  }

  private static <T> T deserializeResponse(final CloseableHttpResponse response) throws IOException, ClassNotFoundException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    response.getEntity().writeTo(outputStream);

    return Serializer.deserialize(outputStream.toByteArray());
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

  private static BasicHttpClientConnectionManager createConnectionManager() {
    try {
      final SSLContext sslContext = SSLContext.getDefault();

      return new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register(HTTPS,
              new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
              .build());
    }
    catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

  }

  private static EntityServerConfiguration configure() {
    ServerConfiguration.SERVER_HOST_NAME.set("localhost");
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    ServerConfiguration.TRUSTSTORE.set("../../framework/server/src/main/security/truststore.jks");
    ServerConfiguration.TRUSTSTORE_PASSWORD.set("crappypass");
    HttpServerConfiguration.HTTP_SERVER_PORT.set(WEB_SERVER_PORT_NUMBER);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set("../../framework/server/src/main/security/keystore.jks");
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(ServerHttps.TRUE);
    System.setProperty("java.security.policy", "../../framework/server/src/main/security/all_permissions.policy");
    final EntityServerConfiguration configuration = EntityServerConfiguration.configuration(2223, 2221);
    configuration.setAdminPort(2223);
    configuration.setAdminUser(Users.parseUser("scott:tiger"));
    configuration.setDomainModelClassNames(singletonList(TestDomain.class.getName()));
    configuration.setDatabase(Databases.getInstance());
    configuration.setSslEnabled(false);
    configuration.setAuxiliaryServerFactoryClassNames(singletonList(EntityServletServerFactory.class.getName()));

    return configuration;
  }

  private static void deconfigure() {
    ServerConfiguration.SERVER_HOST_NAME.set(null);
    ServerConfiguration.RMI_SERVER_HOSTNAME.set(null);
    ServerConfiguration.TRUSTSTORE.set(null);
    ServerConfiguration.TRUSTSTORE_PASSWORD.set(null);
    ServerConfiguration.AUXILIARY_SERVER_FACTORY_CLASS_NAMES.set(null);
    HttpServerConfiguration.HTTP_SERVER_PORT.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(ServerHttps.FALSE);
    System.clearProperty("java.security.policy");
  }
}
