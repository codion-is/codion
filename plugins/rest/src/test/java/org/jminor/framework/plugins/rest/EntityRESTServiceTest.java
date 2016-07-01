/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.rest;

import org.jminor.common.User;
import org.jminor.common.model.ConditionType;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.plugins.json.EntityJSONParser;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.registry.Registry;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityRESTServiceTest {

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = new User("scott", "tiger");
  private static final String BASIC = "Basic ";
  private static final String HTTP = "http";
  private static String HOSTNAME;
  private static String REST_BASEURL;

  private static DefaultEntityConnectionServer server;
  private static EntityConnectionServerAdmin admin;

  static {
    TestDomain.init();
  }

  @BeforeClass
  public static synchronized void setUp() throws Exception {
    configure();
    HOSTNAME = Configuration.getStringValue(Configuration.SERVER_HOST_NAME);
    REST_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities/";
    server = DefaultEntityConnectionServer.startServer();
    admin = (EntityConnectionServerAdmin) server.getServerAdmin(ADMIN_USER);
  }

  @AfterClass
  public static synchronized void tearDown() throws Exception {
    server.shutdown();
    deconfigure();
  }

  @Test
  public void testREST() throws URISyntaxException, IOException, JSONException, ParseException, InterruptedException {
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
    uriBuilder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT);
    HttpResponse response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    client.close();

    //test with unknown user authentication
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new BasicHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = new User("who", "areu");
              request.setHeader(EntityRESTService.AUTHORIZATION,
                      BASIC + DatatypeConverter.printBase64Binary((user.getUsername() + ":" + user.getPassword()).getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            })
            .build();
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT);
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    client.close();

    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(new BasicHttpClientConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              final User user = User.UNIT_TEST_USER;
              request.setHeader(EntityRESTService.AUTHORIZATION,
                      BASIC + DatatypeConverter.printBase64Binary((user.getUsername() + ":" + user.getPassword()).getBytes()));
              request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            })
            .build();

    //select all/GET
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT);
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    String queryResult = getContentStream(response.getEntity());
    List<Entity> queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(4, queryEntities.size());

    Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, null);
    department.put(TestDomain.DEPARTMENT_ID, -42);
    department.put(TestDomain.DEPARTMENT_NAME, "Test");
    department.put(TestDomain.DEPARTMENT_LOCATION, "Location");

    //insert/POST
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("entities", EntityJSONParser.serializeEntities(Collections.singletonList(department), false));
    response = client.execute(new HttpPost(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    final List<Entity.Key> queryKeys = EntityJSONParser.deserializeKeys(queryResult);
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));

    //delete/DELETE by key
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_KEY_PATH).addParameter("keys", EntityJSONParser.serializeKeys(Collections.singletonList(department.getKey())));
    response = client.execute(new HttpDelete(uriBuilder.build()));
    queryResult = getContentStream(response.getEntity());

    //insert/PUT
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("entities", EntityJSONParser.serializeEntities(Collections.singletonList(department), false));
    response = client.execute(new HttpPut(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));
    department = queryEntities.get(0);

    //update/PUT
    department.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    department.put(TestDomain.DEPARTMENT_NAME, "New name");
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("entities", EntityJSONParser.serializeEntities(Collections.singletonList(department), false));
    response = client.execute(new HttpPut(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));

    //select/GET by value
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT)
            .addParameter("searchType", ConditionType.LIKE.toString())
            .addParameter("values", "{\"dname\":\"New name\"}");
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());

    //select/GET by key
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_KEY_PATH).addParameter("keys", EntityJSONParser.serializeKeys(Collections.singletonList(department.getKey())));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());

    //delete/DELETE by value
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT)
            .addParameter("searchType", ConditionType.LIKE.toString())
            .addParameter("values", "{\"deptno\":\"-42\"}");
    response = client.execute(new HttpDelete(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    client.close();

    final Collection<ClientInfo> clients = admin.getClients(EntityRESTService.class.getName());
    assertEquals(1, clients.size());

    admin.disconnect(clients.iterator().next().getClientID());
  }

  @Test
  public void testWebServer() throws Exception {
    try (final InputStream input = new URL("http://localhost:" + WEB_SERVER_PORT_NUMBER + "/ivy.xml").openStream()) {
      assertTrue(input.read() > 0);
    }
  }

  private static URIBuilder createURIBuilder() {
    final URIBuilder builder = new URIBuilder();
    builder.setScheme(HTTP).setHost(REST_BASEURL);

    return builder;
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

  private static void configure() {
    Configuration.setValue(Configuration.REGISTRY_PORT, 2221);
    Configuration.setValue(Configuration.SERVER_CONNECTION_SSL_ENABLED, false);
    Configuration.setValue(Configuration.SERVER_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_USER, "scott:tiger");
    Configuration.setValue(Configuration.SERVER_HOST_NAME, "localhost");
    Configuration.setValue("java.rmi.server.hostname", "localhost");
    Configuration.setValue("java.security.policy", "resources/security/all_permissions.policy");
    Configuration.setValue(Configuration.WEB_SERVER_DOCUMENT_ROOT, System.getProperty("user.dir"));
    Configuration.setValue(Configuration.WEB_SERVER_PORT, WEB_SERVER_PORT_NUMBER);
  }

  private static void deconfigure() {
    Configuration.setValue(Configuration.REGISTRY_PORT, Registry.REGISTRY_PORT);
    Configuration.setValue(Configuration.SERVER_CONNECTION_SSL_ENABLED, true);
    Configuration.clearValue(Configuration.SERVER_PORT);
    Configuration.clearValue(Configuration.SERVER_ADMIN_PORT);
    Configuration.clearValue(Configuration.SERVER_ADMIN_USER);
    Configuration.clearValue(Configuration.SERVER_HOST_NAME);
    Configuration.clearValue("java.rmi.server.hostname");
    Configuration.clearValue("java.security.policy");
    Configuration.clearValue(Configuration.WEB_SERVER_DOCUMENT_ROOT);
    Configuration.clearValue(Configuration.WEB_SERVER_PORT);
  }
}
