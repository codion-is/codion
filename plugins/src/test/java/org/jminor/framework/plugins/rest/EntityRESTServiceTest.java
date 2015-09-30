/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.rest;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.plugins.json.EntityJSONParser;
import org.jminor.framework.server.DefaultEntityConnectionServerAdmin;
import org.jminor.framework.server.EntityConnectionServerAdmin;
import org.jminor.framework.server.EntityConnectionServerTest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
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
import org.apache.http.protocol.HttpContext;
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
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityRESTServiceTest {

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final String BASIC = "Basic ";
  private static final String HTTP = "http";
  private static String HOSTNAME;
  private static String REST_BASEURL ;

  @BeforeClass
  public static synchronized void setUp() throws Exception {
    Configuration.setValue(Configuration.WEB_SERVER_DOCUMENT_ROOT, System.getProperty("user.dir") + System.getProperty("file.separator") + "resources");
    Configuration.setValue(Configuration.WEB_SERVER_PORT, WEB_SERVER_PORT_NUMBER);
    EntityConnectionServerTest.setUp();
    HOSTNAME = Configuration.getStringValue(Configuration.SERVER_HOST_NAME);
    REST_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities/";
  }

  @AfterClass
  public static synchronized void tearDown() throws Exception {
    EntityConnectionServerTest.tearDown();
    Configuration.clearValue(Configuration.WEB_SERVER_DOCUMENT_ROOT);
    Configuration.clearValue(Configuration.WEB_SERVER_PORT);
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
            .addInterceptorFirst(new HttpRequestInterceptor() {
              @Override
              public void process(final HttpRequest request, final HttpContext httpContext) throws HttpException, IOException {
                final User user = new User("who", "areu");
                request.setHeader(EntityRESTService.AUTHORIZATION, BASIC + DatatypeConverter.printBase64Binary((user.getUsername() + ":" + user.getPassword()).getBytes()));
                request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
              }
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
            .addInterceptorFirst(new HttpRequestInterceptor() {
              @Override
              public void process(final HttpRequest request, final HttpContext httpContext) throws HttpException, IOException {
                final User user = User.UNIT_TEST_USER;
                request.setHeader(EntityRESTService.AUTHORIZATION, BASIC + DatatypeConverter.printBase64Binary((user.getUsername() + ":" + user.getPassword()).getBytes()));
                request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
              }
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
    department.setValue(TestDomain.DEPARTMENT_ID, null);
    department.setValue(TestDomain.DEPARTMENT_ID, -42);
    department.setValue(TestDomain.DEPARTMENT_NAME, "Test");
    department.setValue(TestDomain.DEPARTMENT_LOCATION, "Location");

    //insert/POST
    uriBuilder = createURIBuilder();
    uriBuilder.addParameter("entities", EntityJSONParser.serializeEntities(Collections.singletonList(department), false));
    response = client.execute(new HttpPost(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    final List<Entity.Key> queryKeys = EntityJSONParser.deserializeKeys(queryResult);
    assertEquals(1, queryKeys.size());
    assertEquals(department.getPrimaryKey(), queryKeys.get(0));

    //delete/DELETE by key
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_KEY_PATH).addParameter("primaryKeys", EntityJSONParser.serializeKeys(Collections.singletonList(department.getPrimaryKey())));
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
    department.setValue(TestDomain.DEPARTMENT_LOCATION, "New location");
    department.setValue(TestDomain.DEPARTMENT_NAME, "New name");
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
            .addParameter("searchType", SearchType.LIKE.toString())
            .addParameter("values", "{\"dname\":\"New name\"}");
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());

    //select/GET by key
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_KEY_PATH).addParameter("primaryKeys", EntityJSONParser.serializeKeys(Collections.singletonList(department.getPrimaryKey())));
    response = client.execute(new HttpGet(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());

    //delete/DELETE by value
    uriBuilder = createURIBuilder();
    uriBuilder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT)
            .addParameter("searchType", SearchType.LIKE.toString())
            .addParameter("values", "{\"deptno\":\"-42\"}");
    response = client.execute(new HttpDelete(uriBuilder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    client.close();

    final EntityConnectionServerAdmin admin = EntityConnectionServerTest.getServerAdmin();
    final Collection<ClientInfo> clients = admin.getClients(EntityRESTService.class.getName());
    assertEquals(1, clients.size());

    admin.disconnect(clients.iterator().next().getClientID());
  }

  @Test
  public void testWebServer() throws Exception {
    try (final InputStream input = new URL("http://localhost:" + WEB_SERVER_PORT_NUMBER + "/db/scripts/create_h2_db.sql").openStream()) {
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
}
