/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.db.database.Databases;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.common.http.server.ServerHttps;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;

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
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static is.codion.framework.db.condition.Conditions.condition;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServletServerTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = Users.parseUser("scott:tiger");
  private static String SERVER_BASEURL;

  private static EntityServer server;
  private static EntityServerAdmin admin;

  private static final String AUTHORIZATION_HEADER = "Basic " + Base64.getEncoder().encodeToString(
          (UNIT_TEST_USER.getUsername() + ":" + String.valueOf(UNIT_TEST_USER.getPassword())).getBytes());
  private static final String TEST_CLIENT_TYPE_ID = "EntityServletServerTest";

  private static final String[] HEADERS = new String[] {
          EntityService.DOMAIN_TYPE_NAME, TestDomain.DOMAIN.getName(),
          EntityService.CLIENT_TYPE_ID, TEST_CLIENT_TYPE_ID,
          EntityService.CLIENT_ID, UUID.randomUUID().toString(),
          "Content-Type", MediaType.APPLICATION_OCTET_STREAM,
          "Authorization", AUTHORIZATION_HEADER
  };

  private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).connectTimeout(Duration.ofSeconds(2)).build();

  @BeforeAll
  public static void setUp() throws Exception {
    final EntityServerConfiguration configuration = configure();
    SERVER_BASEURL = "https://" + ServerConfiguration.SERVER_HOST_NAME.get() + ":" + WEB_SERVER_PORT_NUMBER + "/entities";
    server = EntityServer.startServer(configuration);
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
                    EntityService.DOMAIN_TYPE_NAME, TestDomain.DOMAIN.getName(),
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
                    EntityService.DOMAIN_TYPE_NAME, TestDomain.DOMAIN.getName(),
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
                    EntityService.DOMAIN_TYPE_NAME, TestDomain.DOMAIN.getName(),
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
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(condition(TestDomain.T_DEPARTMENT).selectCondition()))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    List<Entity> queryEntities = Serializer.deserialize(response.body());
    assertEquals(4, queryEntities.size());

    final Entity department = ENTITIES.entity(TestDomain.T_DEPARTMENT);
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
    List<Key> queryKeys = Serializer.deserialize(response.body());
    assertEquals(1, queryKeys.size());
    assertEquals(department.getKey(), queryKeys.get(0));

    //delete
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/delete"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(condition(department.getKey())))).build();

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
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(condition(TestDomain.DEPARTMENT_NAME).equalTo("New name").selectCondition()))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryEntities = Serializer.deserialize(response.body());
    assertEquals(1, queryEntities.size());

    //select by condition
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/select"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(condition(department.getKey()).selectCondition()))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());
    queryEntities = Serializer.deserialize(response.body());
    assertEquals(1, queryEntities.size());

    //delete
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/delete"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(condition(TestDomain.DEPARTMENT_ID).equalTo(-42)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    //function
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/function"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(singletonList(TestDomain.FUNCTION_ID)))).build();

    response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertEquals(200, response.statusCode());

    //procedure
    request = HttpRequest.newBuilder()
            .uri(URI.create(SERVER_BASEURL + "/procedure"))
            .headers(HEADERS)
            .POST(HttpRequest.BodyPublishers.ofByteArray(Serializer.serialize(singletonList(TestDomain.PROCEDURE_ID)))).build();

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

  private static EntityServerConfiguration configure() {
    System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
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
    System.clearProperty("jdk.internal.httpclient.disableHostnameVerification");
  }
}
