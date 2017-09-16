/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.rmi.registry.Registry;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityServletServerTest {

  private static final Entities ENTITIES = new TestDomain();
  private static final EntityConditions CONDITIONS = new EntityConditions(ENTITIES);

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = new User("scott", "tiger");
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
  public void testREST() throws URISyntaxException, IOException, InterruptedException,
          Serializer.SerializeException, ClassNotFoundException {
    final RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(2000)
            .setConnectTimeout(2000)
            .build();
    CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();

    //test with missing authentication info
    URIBuilder uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", ENTITIES.getDomainId());
    CloseableHttpResponse response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    final String clientTypeId = "EntityServletServerTest";
    //test with missing clientId header
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = UNIT_TEST_USER;
              request.setHeader(EntityServlet.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityServlet.AUTHORIZATION,
                      BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + user.getPassword()).getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();
    final String domainId = new TestDomain().getDomainId();
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("condition",
                    Util.serializeAndBase64Encode(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT)));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    final Value<UUID> clientIdValue = Values.value(UUID.randomUUID());
    //test with unknown user authentication
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = new User("who", "areu");
              request.setHeader(EntityServlet.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityServlet.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader(EntityServlet.AUTHORIZATION,
                      BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + user.getPassword()).getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("condition",
                    Util.serializeAndBase64Encode(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT)));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = UNIT_TEST_USER;
              request.setHeader(EntityServlet.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityServlet.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader(EntityServlet.AUTHORIZATION,
                      BASIC + Base64.getEncoder().encodeToString((user.getUsername() + ":" + user.getPassword())
                              .getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();

    //select all/GET
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("condition",
                    Util.serializeAndBase64Encode(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT)));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    String queryResult = getStringContent(response.getEntity());
    List<Entity> queryEntities = Util.base64DecodeAndDeserialize(queryResult);
    assertEquals(4, queryEntities.size());
    response.close();

    Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, null);
    department.put(TestDomain.DEPARTMENT_ID, -42);
    department.put(TestDomain.DEPARTMENT_NAME, "Test");
    department.put(TestDomain.DEPARTMENT_LOCATION, "Location");

    //insert/POST
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("entities", Util.serializeAndBase64Encode(Collections.singletonList(department)));
    response = client.execute(new HttpPost(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getStringContent(response.getEntity());
    final List<Entity.Key> queryKeys = Util.base64DecodeAndDeserialize(queryResult);
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));
    response.close();

    //delete/DELETE by condition
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("condition",
                    Util.serializeAndBase64Encode(CONDITIONS.selectCondition(department.getKey())));
    response = client.execute(new HttpDelete(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    //insert/PUT
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("entities", Util.serializeAndBase64Encode(Collections.singletonList(department)));
    response = client.execute(new HttpPut(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getStringContent(response.getEntity());
    queryEntities = Util.base64DecodeAndDeserialize(queryResult);
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));
    department = queryEntities.get(0);
    response.close();

    //update/PUT
    department.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    department.put(TestDomain.DEPARTMENT_NAME, "New name");
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("entities", Util.serializeAndBase64Encode(Collections.singletonList(department)));
    response = client.execute(new HttpPut(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getStringContent(response.getEntity());
    queryEntities = Util.base64DecodeAndDeserialize(queryResult);
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));
    response.close();

    //select/GET by condition
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("condition",
                    Util.serializeAndBase64Encode(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT,
                            TestDomain.DEPARTMENT_NAME, Condition.Type.LIKE, "New name")));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getStringContent(response.getEntity());
    queryEntities = Util.base64DecodeAndDeserialize(queryResult);
    assertEquals(1, queryEntities.size());
    response.close();

    //select/GET by condition
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("condition",
                    Util.serializeAndBase64Encode(CONDITIONS.selectCondition(department.getKey())));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getStringContent(response.getEntity());
    queryEntities = Util.base64DecodeAndDeserialize(queryResult);
    assertEquals(1, queryEntities.size());
    response.close();

    //delete/DELETE by condition
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("domainId", domainId)
            .addParameter("condition",
                    Util.serializeAndBase64Encode(CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT,
                            TestDomain.DEPARTMENT_ID, Condition.Type.LIKE, -42)));
    response = client.execute(new HttpDelete(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("function")
            .addParameter("domainId", domainId)
            .addParameter("functionId", TestDomain.FUNCTION_ID)
            .addParameter("parameters", Util.serializeAndBase64Encode(Collections.emptyList()));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure")
            .addParameter("domainId", domainId)
            .addParameter("procedureId", TestDomain.PROCEDURE_ID)
            .addParameter("parameters", Util.serializeAndBase64Encode(Collections.emptyList()));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    Collection<RemoteClient> clients = admin.getClients(clientTypeId);
    assertEquals(1, clients.size());

    //try to change the clientId
    final UUID originalClientId = clientIdValue.get();
    clientIdValue.set(UUID.randomUUID());

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure")
            .addParameter("domainId", domainId)
            .addParameter("procedureId", TestDomain.PROCEDURE_ID)
            .addParameter("parameters", Util.serializeAndBase64Encode(Collections.emptyList()));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();

    clientIdValue.set(originalClientId);

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("disconnect")
            .addParameter("domainId", domainId);
    response = client.execute(new HttpGet(uriBuilder.build()));
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

  private static String getStringContent(final HttpEntity entity) throws IOException {
    Scanner scanner = null;
    try (final InputStream stream = entity.getContent()) {
      scanner = new Scanner(stream).useDelimiter("\\A");

      return scanner.hasNext() ? scanner.next() : null;
    }
    finally {
      Util.closeSilently(scanner);
      EntityUtils.consumeQuietly(entity);
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
