/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Condition;
import is.codion.framework.json.db.ConditionObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.TestDomain.Department;
import is.codion.framework.servlet.TestDomain.Employee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.ContentType;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static is.codion.framework.domain.entity.attribute.Condition.keys;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServiceTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  private static final EntityObjectMapper ENTITY_OBJECT_MAPPER = EntityObjectMapper.entityObjectMapper(ENTITIES);
  private static final ConditionObjectMapper CONDITION_OBJECT_MAPPER = ConditionObjectMapper.conditionObjectMapper(ENTITY_OBJECT_MAPPER);

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));
  private static String HOSTNAME;
  private static HttpHost TARGET_HOST;
  private static String SERVER_BASEURL;
  private static String SERVER_JSON_BASEURL;

  private static EntityServer server;

  @BeforeAll
  public static void setUp() throws Exception {
    EntityServerConfiguration configuration = configure();
    HOSTNAME = Clients.SERVER_HOSTNAME.get();
    TARGET_HOST = new HttpHost(HOSTNAME, EntityService.HTTP_SERVER_PORT.get(), "http");
    SERVER_BASEURL = HOSTNAME + ":" + EntityService.HTTP_SERVER_PORT.get() + "/entities/ser";
    SERVER_JSON_BASEURL = HOSTNAME + ":" + EntityService.HTTP_SERVER_PORT.get() + "/entities/json";
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
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createSerURI("entities")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        try (ObjectInputStream in = new ObjectInputStream(response.getEntity().getContent())) {
          Entities entities = (Entities) in.readObject();
          assertNotNull(entities);
        }
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createJsonURI("entities")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        try (ObjectInputStream in = new ObjectInputStream(response.getEntity().getContent())) {
          Entities entities = (Entities) in.readObject();
          assertNotNull(entities);
        }
      }
    }
  }

  @Test
  void close() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createSerURI("close")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createJsonURI("close")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
    }
  }

  @Test
  void isTransactionOpen() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createSerURI("isTransactionOpen")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        try (ObjectInputStream in = new ObjectInputStream(response.getEntity().getContent())) {
          assertFalse((Boolean) in.readObject());
        }
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createJsonURI("isTransactionOpen")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertFalse(ENTITY_OBJECT_MAPPER.readValue(response.getEntity().getContent(), Boolean.class));
      }
    }
  }

  @Test
  void beginRollbackTransaction() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createSerURI("beginTransaction")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createSerURI("rollbackTransaction")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createJsonURI("beginTransaction")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createJsonURI("rollbackTransaction")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
    }
  }

  @Test
  void beginCommitTransaction() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createSerURI("beginTransaction")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createSerURI("commitTransaction")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createJsonURI("beginTransaction")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createJsonURI("commitTransaction")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
    }
  }

  @Test
  void setQueryCacheEnabled() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("setQueryCacheEnabled"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(true)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createSerURI("isQueryCacheEnabled")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        try (ObjectInputStream in = new ObjectInputStream(response.getEntity().getContent())) {
          assertTrue((Boolean) in.readObject());
        }
      }

      post = new HttpPost(createJsonURI("setQueryCacheEnabled"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(false)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(createJsonURI("isQueryCacheEnabled")), context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertFalse(ENTITY_OBJECT_MAPPER.readValue(response.getEntity().getContent(), Boolean.class));
      }
    }
  }

  @Test
  void procedure() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("procedure"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(TestDomain.PROCEDURE_ID))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      post = new HttpPost(createJsonURI("procedure"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(TestDomain.PROCEDURE_ID))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
    }
  }

  @Test
  void function() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("function"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(TestDomain.FUNCTION_ID))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      post = new HttpPost(createJsonURI("function"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(TestDomain.FUNCTION_ID))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
    }
  }

  @Test
  void report() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("report"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(asList(TestDomain.REPORT_TYPE, "Parameter"))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      post = new HttpPost(createJsonURI("report"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(asList(TestDomain.REPORT_TYPE, "Parameter"))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
    }
  }

  @Test
  void dependencies() throws Exception {
    Entity.Key key1 = ENTITIES.primaryKey(Department.TYPE, 10);
    Entity.Key key2 = ENTITIES.primaryKey(Department.TYPE, 20);
    List<Entity> entitiesDep = Arrays.asList(Entity.entity(key1), Entity.entity(key2));
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("dependencies"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(entitiesDep)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        Map<String, Collection<Entity>> dependencies = deserialize(response.getEntity().getContent());
        assertEquals(1, dependencies.size());
        assertEquals(12, dependencies.get(Employee.TYPE).size());
      }
      post = new HttpPost(createJsonURI("dependencies"));
      post.setEntity(new StringEntity(ENTITY_OBJECT_MAPPER.writeValueAsString(entitiesDep)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        Map<EntityType, Collection<Entity>> dependencies = ENTITY_OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                new TypeReference<Map<EntityType, Collection<Entity>>>() {});
        assertEquals(1, dependencies.size());
        assertEquals(12, dependencies.get(Employee.TYPE).size());
      }
    }
  }

  @Test
  void count() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("count"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(Department.ID.equalTo(10))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(Integer.valueOf(1), deserialize(response.getEntity().getContent()));
      }
      post = new HttpPost(createJsonURI("count"));
      post.setEntity(new StringEntity(CONDITION_OBJECT_MAPPER.writeValueAsString(Department.ID.equalTo(10))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(1, CONDITION_OBJECT_MAPPER.readValue(response.getEntity().getContent(), Integer.class));
      }
    }
  }

  @Test
  void values() throws Exception {
    Select select = Select.where(Department.ID.equalTo(10)).build();
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("values"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(asList(Department.ID, select))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(10, EntityServiceTest.<List<Integer>>deserialize(response.getEntity().getContent()).get(0));
      }
      ObjectNode node = ENTITY_OBJECT_MAPPER.createObjectNode();
      node.set("column", CONDITION_OBJECT_MAPPER.valueToTree(Department.ID.name()));
      node.set("entityType", CONDITION_OBJECT_MAPPER.valueToTree(Department.ID.entityType().name()));
      node.set("condition", CONDITION_OBJECT_MAPPER.valueToTree(select));
      post = new HttpPost(createJsonURI("values"));
      post.setEntity(new StringEntity(node.toString()));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(10, ENTITY_OBJECT_MAPPER.readValue(response.getEntity().getContent(),
                new TypeReference<List<Integer>>() {}).get(0));
      }
    }
  }

  @Test
  void selectByKey() throws Exception {
    List<Entity.Key> keys = new ArrayList<>();
    keys.add(ENTITIES.primaryKey(Department.TYPE, 10));
    keys.add(ENTITIES.primaryKey(Department.TYPE, 20));

    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("selectByKey"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(keys)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(2, EntityServiceTest.<List<Entity>>deserialize(response.getEntity().getContent()).size());
      }
      post = new HttpPost(createJsonURI("selectByKey"));
      post.setEntity(new StringEntity(ENTITY_OBJECT_MAPPER.writeValueAsString(keys)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(2, ENTITY_OBJECT_MAPPER.deserializeEntities(response.getEntity().getContent()).size());
      }
    }
  }

  @Test
  void select() throws Exception {
    List<Entity.Key> keys = new ArrayList<>();
    keys.add(ENTITIES.primaryKey(Department.TYPE, 10));
    keys.add(ENTITIES.primaryKey(Department.TYPE, 20));
    Select select = Select.where(keys(keys)).build();
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("select"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(select)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(2, EntityServiceTest.<List<Entity>>deserialize(response.getEntity().getContent()).size());
      }
      post = new HttpPost(createJsonURI("select"));
      post.setEntity(new StringEntity(CONDITION_OBJECT_MAPPER.writeValueAsString(select)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(2, ENTITY_OBJECT_MAPPER.deserializeEntities(response.getEntity().getContent()).size());
      }
    }
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
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("insert"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(entities)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(2, EntityServiceTest.<List<Entity.Key>>deserialize(response.getEntity().getContent()).size());
      }
      entities.forEach(entity -> entity.put(Department.ID, entity.get(Department.ID) + 1));
      post = new HttpPost(createJsonURI("insertSelect"));
      post.setEntity(new StringEntity(ENTITY_OBJECT_MAPPER.writeValueAsString(entities)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(2, ENTITY_OBJECT_MAPPER.deserializeEntities(response.getEntity().getContent()).size());
      }
    }
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

    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("updateSelect"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(entities)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        List<Entity> updated = EntityServiceTest.<List<Entity>>deserialize(response.getEntity().getContent());
        assertEquals(2, updated.size());
        assertEquals(2, updated.size());
        assertTrue(updated.containsAll(entities));
        assertEquals("NEW YORK2", updated.stream().filter(entity -> entity.get(Department.ID).equals(10))
                .findFirst().orElse(null).get(Department.LOCATION));
        assertEquals("DALLAS2", updated.stream().filter(entity -> entity.get(Department.ID).equals(20))
                .findFirst().orElse(null).get(Department.LOCATION));
      }
      entities.get(0).save(Department.LOCATION);
      entities.get(0).put(Department.LOCATION, "NEW YORK");
      entities.get(1).save(Department.LOCATION);
      entities.get(1).put(Department.LOCATION, "DALLAS");
      post = new HttpPost(createJsonURI("updateSelect"));
      post.setEntity(new StringEntity(ENTITY_OBJECT_MAPPER.writeValueAsString(entities)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        List<Entity> updated = ENTITY_OBJECT_MAPPER.deserializeEntities(response.getEntity().getContent());
        assertEquals(2, updated.size());
        assertEquals(2, updated.size());
        assertEquals(2, updated.size());
        assertTrue(updated.containsAll(entities));
        assertEquals("NEW YORK", updated.stream().filter(entity -> entity.get(Department.ID).equals(10))
                .findFirst().orElse(null).get(Department.LOCATION));
        assertEquals("DALLAS", updated.stream().filter(entity -> entity.get(Department.ID).equals(20))
                .findFirst().orElse(null).get(Department.LOCATION));
      }
    }
  }

  @Test
  void updateCondition() throws Exception {
    Update update = Update.where(Department.ID.between(10, 20))
            .set(Department.LOCATION, "aloc").build();
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("updateByCondition"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(update)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(Integer.valueOf(2), deserialize(response.getEntity().getContent()));
      }
      post = new HttpPost(createJsonURI("updateByCondition"));
      post.setEntity(new StringEntity(CONDITION_OBJECT_MAPPER.writeValueAsString(update)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        Integer updateCount = ENTITY_OBJECT_MAPPER.readValue(response.getEntity().getContent(), Integer.class);
        assertEquals(2, updateCount);
      }
    }
  }

  @Test
  void delete() throws Exception {
    Condition deleteCondition = Department.ID.equalTo(40);
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("delete"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(deleteCondition)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(Integer.valueOf(1), deserialize(response.getEntity().getContent()));
      }
      post = new HttpPost(createJsonURI("delete"));
      post.setEntity(new StringEntity(CONDITION_OBJECT_MAPPER.writeValueAsString(deleteCondition)));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        Integer deleteCount = ENTITY_OBJECT_MAPPER.readValue(response.getEntity().getContent(), Integer.class);
        assertEquals(0, deleteCount);
      }
    }
  }

  @Test
  void deleteByKey() throws Exception {
    try (CloseableHttpClient client = createClient()) {
      HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
      HttpPost post = new HttpPost(createSerURI("deleteByKey"));
      post.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(ENTITIES.primaryKey(Department.TYPE, 50)))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
      post = new HttpPost(createJsonURI("deleteByKey"));
      post.setEntity(new StringEntity(ENTITY_OBJECT_MAPPER.writeValueAsString(singletonList(ENTITIES.primaryKey(Department.TYPE, 60)))));
      try (CloseableHttpResponse response = client.execute(TARGET_HOST, post, context)) {
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      }
    }
  }

  private static CloseableHttpClient createClient() {
    String clientIdString = UUID.randomUUID().toString();

    return HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(2000)
                    .setConnectTimeout(2000)
                    .build())
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, TestDomain.DOMAIN.name());
              request.setHeader(EntityService.CLIENT_TYPE_ID, "EntityJavalinTest");
              request.setHeader(EntityService.CLIENT_ID, clientIdString);
              request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
            })
            .build();
  }

  private static URI createSerURI(String path) throws URISyntaxException {
    return new URIBuilder().setScheme("http").setHost(SERVER_BASEURL).setPath(path).build();
  }

  private static URI createJsonURI(String path) throws URISyntaxException {
    return new URIBuilder().setScheme("http").setHost(SERVER_JSON_BASEURL).setPath(path).build();
  }

  private static HttpClientContext createHttpContext(User user, HttpHost targetHost) {
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
            new AuthScope(targetHost.getHostName(), targetHost.getPort()),
            new UsernamePasswordCredentials(user.username(), String.valueOf(user.password())));

    AuthCache authCache = new BasicAuthCache();
    authCache.put(targetHost, new BasicScheme());

    HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider(credentialsProvider);
    context.setAuthCache(authCache);

    return context;
  }

  private static BasicHttpClientConnectionManager createConnectionManager() {
    return new BasicHttpClientConnectionManager();
//    try {
//      SSLContext sslContext = SSLContext.getDefault();
//
//      return new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register("https",
//                      new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
//              .build());
//    }
//    catch (NoSuchAlgorithmException e) {
//      throw new RuntimeException(e);
//    }
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOSTNAME.set("localhost");
    Clients.TRUSTSTORE.set("../../framework/server/src/main/config/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    Clients.resolveTrustStore();
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    EntityService.HTTP_SERVER_KEYSTORE_PATH.set("../../framework/server/src/main/config/keystore.jks");
    EntityService.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");
    EntityService.HTTP_SERVER_SECURE.set(false);
    System.setProperty("java.security.policy", "../../framework/server/src/main/config/all_permissions.policy");

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
    Clients.SERVER_HOSTNAME.set(null);
    Clients.TRUSTSTORE.set(null);
    Clients.TRUSTSTORE_PASSWORD.set(null);
    System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE);
    System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE_PASSWORD);
    ServerConfiguration.RMI_SERVER_HOSTNAME.set(null);
    ServerConfiguration.AUXILIARY_SERVER_FACTORY_CLASS_NAMES.set(null);
    EntityService.HTTP_SERVER_KEYSTORE_PATH.set(null);
    EntityService.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    EntityService.HTTP_SERVER_SECURE.set(false);
    System.clearProperty("java.security.policy");
  }

  private static <T> T deserialize(InputStream inputStream) throws IOException, ClassNotFoundException {
    return (T) new ObjectInputStream(inputStream).readObject();
  }
}
