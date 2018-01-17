/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.Serializer;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.server.RemoteClient;
import org.jminor.common.server.Server;
import org.jminor.common.server.http.HttpServer;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.rmi.registry.Registry;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class EntityServletServerTest {

  private static final Entities ENTITIES = new TestDomain();
  private static final EntityConditions CONDITIONS = new EntityConditions(ENTITIES);

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = new User("scott", "tiger".toCharArray());
  private static final String BASIC = "Basic ";
  private static final String HTTP = "http";
  private static String HOSTNAME;
  private static String SERVER_BASEURL;

  private static DefaultEntityConnectionServer server;
  private static EntityConnectionServerAdmin admin;

  @BeforeClass
  public static void setUp() throws Exception {
    configure();
    HOSTNAME = Server.SERVER_HOST_NAME.get();
    SERVER_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities/";
    server = DefaultEntityConnectionServer.startServer();
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterClass
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
            .setConnectionManager(new BasicHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = UNIT_TEST_USER;
              request.setHeader(EntityService.DOMAIN_ID, domainId);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientId.toString());
              request.setHeader(EntityService.AUTHORIZATION,
                      BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + String.valueOf(user.getPassword())).getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();

    final URIBuilder uriBuilder = createURIBuilder();
    uriBuilder.setPath("isTransactionOpen");

    final HttpPost httpPost = new HttpPost(uriBuilder.build());
    final CloseableHttpResponse response = client.execute(httpPost);
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
            .setConnectionManager(new BasicHttpClientConnectionManager())
            .build();

    //test with missing authentication info
    URIBuilder uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    uriBuilder.addParameter("domainId", ENTITIES.getDomainId());
    CloseableHttpResponse response = client.execute(new HttpPost(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    final String domainId = new TestDomain().getDomainId();
    final String clientTypeId = "EntityServletServerTest";
    //test with missing clientId header
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new BasicHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = UNIT_TEST_USER;
              request.setHeader(EntityService.DOMAIN_ID, domainId);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.AUTHORIZATION,
                      BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + String.valueOf(user.getPassword())).getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    response = client.execute(new HttpPost(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    final Value<UUID> clientIdValue = Values.value(UUID.randomUUID());
    //test with unknown user authentication
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new BasicHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = new User("who", "areu".toCharArray());
              request.setHeader(EntityService.DOMAIN_ID, domainId);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader(EntityService.AUTHORIZATION,
                      BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + String.valueOf(user.getPassword())).getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    response = client.execute(new HttpPost(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new BasicHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = UNIT_TEST_USER;
              request.setHeader(EntityService.DOMAIN_ID, domainId);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader(EntityService.AUTHORIZATION,
                      BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + String.valueOf(user.getPassword()))
                              .getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();

    //select all/GET
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    HttpPost httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT))));
    response = client.execute(httpPost);
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
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(Collections.singletonList(department))));
    response = client.execute(httpPost);
    assertEquals(200, response.getStatusLine().getStatusCode());
    final List<Entity.Key> queryKeys = deserializeResponse(response);
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));
    response.close();

    //delete
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(CONDITIONS.selectCondition(department.getKey()))));
    response = client.execute(httpPost);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    //insert
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("insert");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(Collections.singletonList(department))));
    response = client.execute(httpPost);
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
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(Collections.singletonList(department))));
    response = client.execute(httpPost);
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
                    TestDomain.DEPARTMENT_NAME, Condition.Type.LIKE, "New name"))));
    response = client.execute(httpPost);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    response.close();

    //select by condition
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(CONDITIONS.selectCondition(department.getKey()))));
    response = client.execute(httpPost);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    response.close();

    //delete
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT,
                    TestDomain.DEPARTMENT_ID, Condition.Type.LIKE, -42))));
    response = client.execute(httpPost);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("function")
            .addParameter("functionId", TestDomain.FUNCTION_ID);
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(Collections.emptyList())));
    response = client.execute(httpPost);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure")
            .addParameter("procedureId", TestDomain.PROCEDURE_ID);
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(Collections.emptyList())));
    response = client.execute(httpPost);
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
    httpPost.setEntity(new ByteArrayEntity(Util.serialize(Collections.emptyList())));
    response = client.execute(httpPost);
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();

    clientIdValue.set(originalClientId);

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("disconnect");
    response = client.execute(new HttpPost(uriBuilder.build()));
    response.close();

    client.close();

    clients = admin.getClients(clientTypeId);
    assertTrue(clients.isEmpty());
  }

  private static URIBuilder createURIBuilder() {
    final URIBuilder builder = new URIBuilder();
    builder.setScheme(HTTP).setHost(SERVER_BASEURL);

    return builder;
  }

  private static <T> T deserializeResponse(final CloseableHttpResponse response) throws IOException, ClassNotFoundException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    response.getEntity().writeTo(outputStream);

    return Util.deserialize(outputStream.toByteArray());
  }

  private static void configure() {
    Server.REGISTRY_PORT.set(2221);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(false);
    Server.SERVER_PORT.set(2223);
    Server.SERVER_ADMIN_PORT.set(2223);
    Server.SERVER_ADMIN_USER.set("scott:tiger");
    Server.SERVER_HOST_NAME.set("localhost");
    Server.RMI_SERVER_HOSTNAME.set("localhost");
    System.setProperty("java.security.policy", "resources/security/all_permissions.policy");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set(TestDomain.class.getName());
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(EntityServletServer.class.getName());
    HttpServer.HTTP_SERVER_PORT.set(WEB_SERVER_PORT_NUMBER);
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
  }
}
