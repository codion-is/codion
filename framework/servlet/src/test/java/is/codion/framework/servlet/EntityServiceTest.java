/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.json.db.DatabaseObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.TestDomain.Department;
import is.codion.framework.servlet.TestDomain.Employee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static is.codion.framework.domain.entity.condition.Condition.keys;
import static is.codion.framework.json.db.DatabaseObjectMapper.databaseObjectMapper;
import static is.codion.framework.json.domain.EntityObjectMapper.entityObjectMapper;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServiceTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  private static final DatabaseObjectMapper OBJECT_MAPPER = databaseObjectMapper(entityObjectMapper(ENTITIES));

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));
  private static final int OK = 200;

  private static String HOSTNAME;
  private static String SERVER_BASEURL;
  private static String SERVER_JSON_BASEURL;

  private static final String CLIENT_ID_STRING = UUID.randomUUID().toString();

  private static EntityServer server;

  private static final HttpClient HTTP_CLIENT = createHttpClient();

  @BeforeAll
  public static void setUp() throws Exception {
    EntityServerConfiguration configuration = configure();
    HOSTNAME = Clients.SERVER_HOSTNAME.get();
    SERVER_BASEURL = "http://" + HOSTNAME + ":" + EntityService.HTTP_SERVER_PORT.get() + "/entities/ser/";
    SERVER_JSON_BASEURL = "http://" + HOSTNAME + ":" + EntityService.HTTP_SERVER_PORT.get() + "/entities/json/";
    server = EntityServer.startServer(configuration);
  }

  @AfterAll
  public static void tearDown() {
    if (server != null) {
      server.shutdown();
    }
    deconfigure();
  }

  @Test
  void entities() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("entities"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    Entities entities = Serializer.deserialize(response.body());
    assertNotNull(entities);

    response = HTTP_CLIENT.send(createJsonRequest("entities"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    entities = Serializer.deserialize(response.body());
    assertNotNull(entities);
  }

  @Test
  void close() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("close"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("close"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
  }

  @Test
  void isTransactionOpen() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("isTransactionOpen"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    boolean value = Serializer.deserialize(response.body());
    assertFalse(value);

    response = HTTP_CLIENT.send(createJsonRequest("isTransactionOpen"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    value = OBJECT_MAPPER.readValue(new String((byte[]) response.body(), UTF_8), Boolean.class);
    assertFalse(value);
  }

  @Test
  void beginRollbackTransaction() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("beginTransaction"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    response = HTTP_CLIENT.send(createRequest("rollbackTransaction"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("beginTransaction"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    response = HTTP_CLIENT.send(createJsonRequest("rollbackTransaction"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
  }

  @Test
  void beginCommitTransaction() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("beginTransaction"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    response = HTTP_CLIENT.send(createRequest("commitTransaction"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("beginTransaction"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    response = HTTP_CLIENT.send(createJsonRequest("commitTransaction"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
  }

  @Test
  void setQueryCacheEnabled() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("setQueryCacheEnabled",
            BodyPublishers.ofByteArray(Serializer.serialize(true))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createRequest("isQueryCacheEnabled"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    boolean value = Serializer.deserialize(response.body());
    assertTrue(value);

    response = HTTP_CLIENT.send(createRequest("setQueryCacheEnabled",
            BodyPublishers.ofByteArray(Serializer.serialize(false))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createRequest("isQueryCacheEnabled"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    value = Serializer.deserialize(response.body());
    assertFalse(value);

    response = HTTP_CLIENT.send(createJsonRequest("setQueryCacheEnabled",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(true))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("isQueryCacheEnabled"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    value = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Boolean.class);
    assertTrue(value);

    response = HTTP_CLIENT.send(createJsonRequest("setQueryCacheEnabled",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(false))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("isQueryCacheEnabled"), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    value = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Boolean.class);
    assertFalse(value);
  }

  @Test
  void procedure() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("procedure",
            BodyPublishers.ofByteArray(Serializer.serialize(singletonList(TestDomain.PROCEDURE_ID)))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("procedure",
            BodyPublishers.ofByteArray(Serializer.serialize(singletonList(TestDomain.PROCEDURE_ID)))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
  }

  @Test
  void function() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("function",
            BodyPublishers.ofByteArray(Serializer.serialize(singletonList(TestDomain.FUNCTION_ID)))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("function",
            BodyPublishers.ofByteArray(Serializer.serialize(singletonList(TestDomain.FUNCTION_ID)))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
  }

  @Test
  void report() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("report",
            BodyPublishers.ofByteArray(Serializer.serialize(asList(TestDomain.REPORT_TYPE, "Parameter")))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("report",
            BodyPublishers.ofByteArray(Serializer.serialize(asList(TestDomain.REPORT_TYPE, "Parameter")))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
  }

  @Test
  void dependencies() throws Exception {
    Entity.Key key1 = ENTITIES.primaryKey(Department.TYPE, 10);
    Entity.Key key2 = ENTITIES.primaryKey(Department.TYPE, 20);
    List<Entity> entitiesDep = Arrays.asList(Entity.entity(key1), Entity.entity(key2));

    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("dependencies",
            BodyPublishers.ofByteArray(Serializer.serialize(entitiesDep))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    Map<String, Collection<Entity>> dependencies = Serializer.deserialize(response.body());
    assertEquals(1, dependencies.size());
    assertEquals(12, dependencies.get(Employee.TYPE).size());

    response = HTTP_CLIENT.send(createJsonRequest("dependencies",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(entitiesDep))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    String content = new String(response.body(), UTF_8);
    Map<EntityType, Collection<Entity>> collectionMap = OBJECT_MAPPER.readValue(content,
            new TypeReference<Map<EntityType, Collection<Entity>>>() {});
    assertEquals(1, collectionMap.size());
    assertEquals(12, collectionMap.get(Employee.TYPE).size());
  }

  @Test
  void count() throws Exception {
    Count where = Count.where(Department.ID.equalTo(10));
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("count",
            BodyPublishers.ofByteArray(Serializer.serialize(where))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    int count = Serializer.deserialize(response.body());
    assertEquals(1, count);

    response = HTTP_CLIENT.send(createJsonRequest("count",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(where))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    count = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Integer.class);
    assertEquals(1, count);
  }

  @Test
  void values() throws Exception {
    Select select = Select.where(Department.ID.equalTo(10)).build();
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("values",
            BodyPublishers.ofByteArray(Serializer.serialize(asList(Department.ID, select)))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    List<Integer> values = Serializer.deserialize(response.body());
    assertEquals(10, values.get(0));

    ObjectNode node = OBJECT_MAPPER.createObjectNode();
    node.set("column", OBJECT_MAPPER.valueToTree(Department.ID.name()));
    node.set("entityType", OBJECT_MAPPER.valueToTree(Department.ID.entityType().name()));
    node.set("condition", OBJECT_MAPPER.valueToTree(select));
    response = HTTP_CLIENT.send(createJsonRequest("values",
            BodyPublishers.ofString(node.toString())), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), new TypeReference<List<Integer>>() {});
    assertEquals(10, values.get(0));
  }

  @Test
  void selectByKey() throws Exception {
    List<Entity.Key> keys = new ArrayList<>();
    keys.add(ENTITIES.primaryKey(Department.TYPE, 10));
    keys.add(ENTITIES.primaryKey(Department.TYPE, 20));

    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("selectByKey",
            BodyPublishers.ofByteArray(Serializer.serialize(keys))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    List<Entity> values = Serializer.deserialize(response.body());
    assertEquals(2, values.size());

    response = HTTP_CLIENT.send(createJsonRequest("selectByKey",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(keys))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), EntityObjectMapper.ENTITY_LIST_REFERENCE);
    assertEquals(2, values.size());
  }

  @Test
  void select() throws Exception {
    List<Entity.Key> keys = new ArrayList<>();
    keys.add(ENTITIES.primaryKey(Department.TYPE, 10));
    keys.add(ENTITIES.primaryKey(Department.TYPE, 20));
    Select select = Select.where(keys(keys)).build();

    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("select",
            BodyPublishers.ofByteArray(Serializer.serialize(select))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    List<Entity> values = Serializer.deserialize(response.body());
    assertEquals(2, values.size());

    response = HTTP_CLIENT.send(createJsonRequest("select",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(select))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), EntityObjectMapper.ENTITY_LIST_REFERENCE);
    assertEquals(2, values.size());
  }

  @Test
  void insert() throws Exception {
    List<Entity> entities = new ArrayList<>();
    entities.add(ENTITIES.builder(Department.TYPE)
            .with(Department.ID, -10)
            .with(Department.NAME, "A name")
            .with(Department.LOCATION, "loc")
            .build());
    entities.add(ENTITIES.builder(Department.TYPE)
            .with(Department.ID, -20)
            .with(Department.NAME, "Another name")
            .with(Department.LOCATION, "locat")
            .build());

    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("insertSelect",
            BodyPublishers.ofByteArray(Serializer.serialize(entities))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    List<Entity> values = Serializer.deserialize(response.body());
    assertEquals(2, values.size());

    entities.forEach(entity -> entity.put(Department.ID, entity.get(Department.ID) + 1));
    response = HTTP_CLIENT.send(createJsonRequest("insert",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(entities))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), EntityObjectMapper.ENTITY_LIST_REFERENCE);
    assertEquals(2, values.size());
  }

  @Test
  void update() throws Exception {
    List<Entity> entities = new ArrayList<>();
    entities.add(ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .with(Department.NAME, "ACCOUNTING")
            .with(Department.LOCATION, "NEW YORK")
            .build());
    entities.add(ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 20)
            .with(Department.NAME, "RESEARCH")
            .with(Department.LOCATION, "DALLAS")
            .build());
    entities.get(0).put(Department.LOCATION, "NEW YORK2");
    entities.get(1).put(Department.LOCATION, "DALLAS2");

    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("updateSelect",
            BodyPublishers.ofByteArray(Serializer.serialize(entities))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    List<Entity> values = Serializer.deserialize(response.body());
    assertEquals(2, values.size());
    assertTrue(values.containsAll(entities));
    assertEquals("NEW YORK2", values.stream().filter(entity -> entity.get(Department.ID).equals(10))
            .findFirst().orElse(null).get(Department.LOCATION));
    assertEquals("DALLAS2", values.stream().filter(entity -> entity.get(Department.ID).equals(20))
            .findFirst().orElse(null).get(Department.LOCATION));

    entities.get(0).save(Department.LOCATION);
    entities.get(0).put(Department.LOCATION, "NEW YORK");
    entities.get(1).save(Department.LOCATION);
    entities.get(1).put(Department.LOCATION, "DALLAS");
    response = HTTP_CLIENT.send(createJsonRequest("updateSelect",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(entities))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    values = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), EntityObjectMapper.ENTITY_LIST_REFERENCE);
    assertEquals(2, values.size());
    assertTrue(values.containsAll(entities));
    assertEquals("NEW YORK", values.stream().filter(entity -> entity.get(Department.ID).equals(10))
            .findFirst().orElse(null).get(Department.LOCATION));
    assertEquals("DALLAS", values.stream().filter(entity -> entity.get(Department.ID).equals(20))
            .findFirst().orElse(null).get(Department.LOCATION));
  }

  @Test
  void updateCondition() throws Exception {
    Update update = Update.where(Department.ID.between(10, 20))
            .set(Department.LOCATION, "aloc").build();
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("updateByCondition",
            BodyPublishers.ofByteArray(Serializer.serialize(update))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    int count = Serializer.deserialize(response.body());
    assertEquals(2, count);

    response = HTTP_CLIENT.send(createJsonRequest("updateByCondition",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(update))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    count = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Integer.class);
    assertEquals(2, count);
  }

  @Test
  void delete() throws Exception {
    Condition deleteCondition = Department.ID.equalTo(40);
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("delete",
            BodyPublishers.ofByteArray(Serializer.serialize(deleteCondition))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    int count = Serializer.deserialize(response.body());
    assertEquals(1, count);

    response = HTTP_CLIENT.send(createJsonRequest("delete",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(deleteCondition))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
    count = OBJECT_MAPPER.readValue(new String(response.body(), UTF_8), Integer.class);
    assertEquals(0, count);
  }

  @Test
  void deleteByKey() throws Exception {
    HttpResponse<byte[]> response = HTTP_CLIENT.send(createRequest("deleteByKey",
            BodyPublishers.ofByteArray(Serializer.serialize(singletonList(ENTITIES.primaryKey(Department.TYPE, 50))))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());

    response = HTTP_CLIENT.send(createJsonRequest("deleteByKey",
            BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(singletonList(ENTITIES.primaryKey(Department.TYPE, 60))))), BodyHandlers.ofByteArray());
    assertEquals(OK, response.statusCode());
  }

  private static HttpRequest createRequest(String path) {
    return createRequest(SERVER_BASEURL, path, BodyPublishers.noBody());
  }

  private static HttpRequest createRequest(String path, BodyPublisher bodyPublisher) {
    return createRequest(SERVER_BASEURL, path, bodyPublisher);
  }

  private static HttpRequest createJsonRequest(String path) {
    return createJsonRequest(path, BodyPublishers.noBody());
  }

  private static HttpRequest createJsonRequest(String path, BodyPublisher bodyPublisher) {
    return createRequest(SERVER_JSON_BASEURL, path, bodyPublisher);
  }

  private static HttpRequest createRequest(String baseUrl, String path, BodyPublisher bodyPublisher) {
    return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .POST(bodyPublisher)
            .headers(new String[] {
                    EntityService.DOMAIN_TYPE_NAME, TestDomain.DOMAIN.name(),
                    EntityService.CLIENT_TYPE_ID, "EntityJavalinTest",
                    EntityService.CLIENT_ID, CLIENT_ID_STRING,
                    "Authorization", createAuthorizationHeader()
            })
            .build();
  }

  private static String createAuthorizationHeader() {
    return "Basic " + Base64.getEncoder().encodeToString((UNIT_TEST_USER.username() +
            ":" + String.valueOf(UNIT_TEST_USER.password())).getBytes());
  }

  private static HttpClient createHttpClient() {
    return HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .build();
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOSTNAME.set("localhost");
    Clients.resolveTrustStore();
    EntityService.HTTP_SERVER_SECURE.set(false);

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .adminUser(User.parse("scott:tiger"))
            .domainClassNames(singletonList(TestDomain.class.getName()))
            .database(Database.instance())
            .sslEnabled(false)
            .auxiliaryServerFactoryClassNames(singletonList(EntityServiceFactory.class.getName()))
            .build();
  }

  private static void deconfigure() {
    System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.FALSE.toString());
    Clients.SERVER_HOSTNAME.set(null);
    ServerConfiguration.AUXILIARY_SERVER_FACTORY_CLASS_NAMES.set(null);
    EntityService.HTTP_SERVER_SECURE.set(true);
  }
}
