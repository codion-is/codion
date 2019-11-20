/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.ConditionType;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.Server;
import org.jminor.common.remote.http.HttpServer;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.rmi.registry.Registry;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServletServerTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = new User("scott", "tiger".toCharArray());
  private static String SERVER_BASEURL;

  private static DefaultEntityConnectionServer server;
  private static EntityConnectionServerAdmin admin;

  private static final String AUTHORIZATION_HEADER = "Basic " + Base64.getEncoder().encodeToString(
          (UNIT_TEST_USER.getUsername() + ":" + String.valueOf(UNIT_TEST_USER.getPassword())).getBytes());
  private static final String TEST_CLIENT_TYPE_ID = "EntityServletServerTest";

  private static final String[] HEADERS = new String[] {
          EntityService.DOMAIN_ID, new TestDomain().getDomainId(),
          EntityService.CLIENT_TYPE_ID, TEST_CLIENT_TYPE_ID,
          EntityService.CLIENT_ID, UUID.randomUUID().toString(),
          "Content-Type", MediaType.APPLICATION_OCTET_STREAM,
          "Authorization", AUTHORIZATION_HEADER
  };

  private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).connectTimeout(Duration.ofSeconds(2)).build();

  @BeforeAll
  public static void setUp() throws Exception {
    configure();
    SERVER_BASEURL = "https://" + Server.SERVER_HOST_NAME.get() + ":" + WEB_SERVER_PORT_NUMBER + "/entities";
    server = DefaultEntityConnectionServer.startServer();
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    server.shutdown();
    deconfigure();
  }

  @Test
  public void isTransactionOpen() throws IOException, InterruptedException, ClassNotFoundException {
    final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/isTransactionOpen"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.noBody()).build();

    final HttpResponse<byte[]> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    final Boolean result = Util.deserialize(httpResponse.body());
    assertFalse(result);
  }

  @Test
  public void test() throws URISyntaxException, IOException, InterruptedException,
          ClassNotFoundException {
    //test with missing authentication info
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(new String[] {
                    EntityService.DOMAIN_ID, new TestDomain().getDomainId(),
                    EntityService.CLIENT_TYPE_ID, "EntityServletServerTest",
                    EntityService.CLIENT_ID, UUID.randomUUID().toString(),
                    "Content-Type", MediaType.APPLICATION_OCTET_STREAM
            })
            .POST(HttpRequest.BodyPublishers.noBody()).build();

    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(401, response.statusCode());

    //test with missing clientId header
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(new String[] {
                    EntityService.DOMAIN_ID, new TestDomain().getDomainId(),
                    EntityService.CLIENT_TYPE_ID, "EntityServletServerTest",
                    "Content-Type", MediaType.APPLICATION_OCTET_STREAM,
                    "Authorization", AUTHORIZATION_HEADER
            })
            .POST(HttpRequest.BodyPublishers.noBody()).build();
    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(401, response.statusCode());

    //test with unknown user authentication
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(new String[] {
                    EntityService.DOMAIN_ID, new TestDomain().getDomainId(),
                    EntityService.CLIENT_TYPE_ID, "EntityServletServerTest",
                    EntityService.CLIENT_ID, UUID.randomUUID().toString(),
                    "Content-Type", MediaType.APPLICATION_OCTET_STREAM,
                    "Authorization", "Basic " + Base64.getEncoder().encodeToString("who:areu".getBytes())
            })
            .POST(HttpRequest.BodyPublishers.noBody()).build();
    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(401, response.statusCode());

    //select all
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    List<Entity> queryEntities = Util.deserialize(response.body());
    assertEquals(4, queryEntities.size());

    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, null);
    department.put(TestDomain.DEPARTMENT_ID, -42);
    department.put(TestDomain.DEPARTMENT_NAME, "Test");
    department.put(TestDomain.DEPARTMENT_LOCATION, "Location");

    //insert
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/insert"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(singletonList(department)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    List<Entity.Key> queryKeys = Util.deserialize(response.body());
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));

    //delete
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/delete"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(Conditions.entitySelectCondition(department.getKey())))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    //insert
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/insert"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(singletonList(department)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryKeys = Util.deserialize(response.body());
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));

    //update
    department.saveAll();
    department.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    department.put(TestDomain.DEPARTMENT_NAME, "New name");

    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/update"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(singletonList(department)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryEntities = Util.deserialize(response.body());
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));

    //select by condition
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT,
                    TestDomain.DEPARTMENT_NAME, ConditionType.LIKE, "New name")))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryEntities = Util.deserialize(response.body());
    assertEquals(1, queryEntities.size());

    //select by condition
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(Conditions.entitySelectCondition(department.getKey())))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryEntities = Util.deserialize(response.body());
    assertEquals(1, queryEntities.size());

    //delete
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/delete"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT,
                    TestDomain.DEPARTMENT_ID, ConditionType.LIKE, -42)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    //function
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/function?functionId=" + TestDomain.FUNCTION_ID))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(emptyList()))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    //procedure
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/procedure?procedureId=" + TestDomain.PROCEDURE_ID))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Util.serialize(emptyList()))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    Collection<RemoteClient> clients = admin.getClients(TEST_CLIENT_TYPE_ID);
    assertEquals(1, clients.size());

    //disconnect
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/disconnect"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.noBody()).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    clients = admin.getClients(TEST_CLIENT_TYPE_ID);
    assertTrue(clients.isEmpty());
  }

  private static void configure() {
    System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
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
    System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.FALSE.toString());
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
