/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.Serializer;
import org.jminor.common.db.Operator;
import org.jminor.common.db.database.Databases;
import org.jminor.common.remote.server.RemoteClient;
import org.jminor.common.remote.server.ServerConfiguration;
import org.jminor.common.remote.server.http.HttpServerConfiguration;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.server.EntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;
import org.jminor.framework.server.EntityConnectionServerConfiguration;

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
import static org.jminor.framework.db.condition.Conditions.selectCondition;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServletServerTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = Users.parseUser("scott:tiger");
  private static String SERVER_BASEURL;

  private static EntityConnectionServer server;
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
    final EntityConnectionServerConfiguration configuration = configure();
    HOSTNAME = ServerConfiguration.SERVER_HOST_NAME.get();
    TARGET_HOST = new HttpHost(HOSTNAME, WEB_SERVER_PORT_NUMBER, HTTPS);
    SERVER_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities";
    server = EntityConnectionServer.startServer(configuration);
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
    final Boolean result = Serializer.deserialize(httpResponse.body());
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
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(selectCondition(TestDomain.T_DEPARTMENT)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    List<Entity> queryEntities = Serializer.deserialize(response.body());
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
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(singletonList(department)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    List<Entity.Key> queryKeys = Serializer.deserialize(response.body());
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));

    //delete
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/delete"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(selectCondition(department.getKey())))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    //insert
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/insert"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(singletonList(department)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryKeys = Serializer.deserialize(response.body());
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));

    //update
    department.saveAll();
    department.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    department.put(TestDomain.DEPARTMENT_NAME, "New name");

    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/update"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(singletonList(department)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryEntities = Serializer.deserialize(response.body());
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));

    //select by condition
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(selectCondition(TestDomain.T_DEPARTMENT,
                    TestDomain.DEPARTMENT_NAME, Operator.LIKE, "New name")))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryEntities = Serializer.deserialize(response.body());
    assertEquals(1, queryEntities.size());

    //select by condition
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(selectCondition(department.getKey())))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryEntities = Serializer.deserialize(response.body());
    assertEquals(1, queryEntities.size());

    //delete
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/delete"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(selectCondition(TestDomain.T_DEPARTMENT,
                    TestDomain.DEPARTMENT_ID, Operator.LIKE, -42)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    //function
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/function?functionId=" + TestDomain.FUNCTION_ID))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(emptyList()))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    //procedure
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/procedure?procedureId=" + TestDomain.PROCEDURE_ID))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(emptyList()))).build();

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
    final ServerConfiguration serverConfiguration = ServerConfiguration.configuration(2223);
    final EntityConnectionServerConfiguration configuration = EntityConnectionServerConfiguration.configuration(serverConfiguration, 2221);
    configuration.setSslEnabled(false);
    configuration.setAdminPort(2223);
    configuration.setAdminUser(Users.parseUser("scott:tiger"));
    configuration.setDomainModelClassNames(singletonList(TestDomain.class.getName()));
    configuration.setAuxiliaryServerClassNames(singletonList(EntityServletServer.class.getName()));
    configuration.setDatabase(Databases.getInstance());

    return configuration;
  }

  private static void deconfigure() {
    System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.FALSE.toString());
    ServerConfiguration.SERVER_HOST_NAME.set(null);
    ServerConfiguration.RMI_SERVER_HOSTNAME.set(null);
    ServerConfiguration.TRUSTSTORE.set(null);
    ServerConfiguration.TRUSTSTORE_PASSWORD.set(null);
    ServerConfiguration.AUXILIARY_SERVER_CLASS_NAMES.set(null);
    HttpServerConfiguration.HTTP_SERVER_PORT.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(false);
    System.clearProperty("java.security.policy");
  }
}
