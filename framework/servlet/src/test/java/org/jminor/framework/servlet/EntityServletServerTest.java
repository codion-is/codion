/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.Serializer;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.db.ConditionType;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.Server;
import org.jminor.common.remote.http.HttpServer;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;

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
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServletServerTest {

  private static final Domain DOMAIN = new TestDomain();
  private static final EntityConditions CONDITIONS = EntityConditions.using(DOMAIN);

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = new User("scott", "tiger".toCharArray());
  private static final String HTTPS = "https";
  private static String HOSTNAME;
  private static HttpHost TARGET_HOST;
  private static String SERVER_BASEURL;

  private static DefaultEntityConnectionServer server;
  private static EntityConnectionServerAdmin admin;

  @BeforeAll
  public static void setUp() throws Exception {
    configure();
    HOSTNAME = Server.SERVER_HOST_NAME.get();
    TARGET_HOST = new HttpHost(HOSTNAME, WEB_SERVER_PORT_NUMBER, HTTPS);
    SERVER_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities";
    server = DefaultEntityConnectionServer.startServer();
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
    final String domainId = new TestDomain().getDomainId();
    final String clientTypeId = "EntityServletServerTest";
    final UUID clientId = UUID.randomUUID();
    final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_ID, domainId);
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
  public void test() throws URISyntaxException, IOException, InterruptedException,
          Serializer.SerializeException, ClassNotFoundException {
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
    uriBuilder.addParameter("domainId", DOMAIN.getDomainId());
    CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    final String domainId = new TestDomain().getDomainId();
    final String clientTypeId = "EntityServletServerTest";
    //test with missing clientId header
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_ID, domainId);
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
              request.setHeader(EntityService.DOMAIN_ID, domainId);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    context = createHttpContext(new User("who", "areu".toCharArray()), TARGET_HOST);
    response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()), context);
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_ID, domainId);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();

    //select all/GET
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    HttpPost httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(EntityConditions.selectCondition(TestDomain.T_DEPARTMENT))));
    context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    List<Entity> queryEntities = deserializeResponse(response);
    assertEquals(4, queryEntities.size());
    response.close();

    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, null);
    department.put(TestDomain.DEPARTMENT_ID, -42);
    department.put(TestDomain.DEPARTMENT_NAME, "Test");
    department.put(TestDomain.DEPARTMENT_LOCATION, "Location");

    //insert
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("insert");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(singletonList(department))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    final List<Entity.Key> queryKeys = deserializeResponse(response);
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));
    response.close();

    //delete
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(EntityConditions.selectCondition(department.getKey()))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    //insert
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("insert");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(singletonList(department))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    final List<Entity.Key> keys = deserializeResponse(response);
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
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(singletonList(department))));
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
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, ConditionType.LIKE, "New name"))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    response.close();

    //select by condition
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(EntityConditions.selectCondition(department.getKey()))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    response.close();

    //delete
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_ID, ConditionType.LIKE, -42))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("function")
            .addParameter("functionId", TestDomain.FUNCTION_ID);
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(emptyList())));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure")
            .addParameter("procedureId", TestDomain.PROCEDURE_ID);
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(emptyList())));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    Collection<RemoteClient> clients = admin.getClients(clientTypeId);
    assertEquals(1, clients.size());

    //try to change the clientId
    final UUID originalClientId = clientIdValue.get();
    clientIdValue.set(UUID.randomUUID());

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure")
            .addParameter("procedureId", TestDomain.PROCEDURE_ID);
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(emptyList())));
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

    return Util.deserialize(outputStream.toByteArray());
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

  private static void configure() {
    Server.REGISTRY_PORT.set(2221);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(false);
    Server.SERVER_PORT.set(2223);
    Server.SERVER_ADMIN_PORT.set(2223);
    Server.SERVER_ADMIN_USER.set("scott:tiger");
    Server.SERVER_HOST_NAME.set("localhost");
    Server.RMI_SERVER_HOSTNAME.set("localhost");
    System.setProperty("java.security.policy", "../../framework/server/src/main/security/all_permissions.policy");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set(TestDomain.class.getName());
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(EntityServletServer.class.getName());
    HttpServer.HTTP_SERVER_PORT.set(WEB_SERVER_PORT_NUMBER);
    HttpServer.HTTP_SERVER_KEYSTORE_PATH.set("../../framework/server/src/main/security/jminor_keystore.jks");
    Server.TRUSTSTORE.set("../../framework/server/src/main/security/jminor_truststore.jks");
    Server.TRUSTSTORE_PASSWORD.set("crappypass");
    HttpServer.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");
    HttpServer.HTTP_SERVER_SECURE.set(true);
  }

  private static void deconfigure() {
    Server.REGISTRY_PORT.set(Registry.REGISTRY_PORT);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(true);
    Server.SERVER_PORT.set(null);
    Server.SERVER_ADMIN_PORT.set(null);
    Server.SERVER_ADMIN_USER.set(null);
    Server.SERVER_HOST_NAME.set(null);
    Server.RMI_SERVER_HOSTNAME.set(null);
    System.clearProperty("java.security.policy");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set(null);
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(null);
    HttpServer.HTTP_SERVER_PORT.set(null);
    HttpServer.HTTP_SERVER_KEYSTORE_PATH.set(null);
    Server.TRUSTSTORE.set(null);
    Server.TRUSTSTORE_PASSWORD.set(null);
    HttpServer.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    HttpServer.HTTP_SERVER_SECURE.set(false);
  }
}
