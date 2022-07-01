/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.Serializer;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.plugin.jackson.json.db.ConditionObjectMapper;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.MediaType;
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

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.condition.Conditions.where;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServletServerTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final User ADMIN_USER = User.parse("scott:tiger");
  private static final String HTTPS = "https";
  private static String HOSTNAME;
  private static HttpHost TARGET_HOST;
  private static String SERVER_BASEURL;
  private static String SERVER_JSON_BASEURL;

  private static EntityServer server;
  private static EntityServerAdmin admin;

  @BeforeAll
  public static void setUp() throws Exception {
    EntityServerConfiguration configuration = configure();
    HOSTNAME = Clients.SERVER_HOST_NAME.get();
    TARGET_HOST = new HttpHost(HOSTNAME, WEB_SERVER_PORT_NUMBER, HTTPS);
    SERVER_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities/ser";
    SERVER_JSON_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities/json";
    server = EntityServer.startServer(configuration);
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    server.shutdown();
    deconfigure();
  }

  @Test
  void isTransactionOpen() throws URISyntaxException, IOException, ClassNotFoundException {
    RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(2000)
            .setConnectTimeout(2000)
            .build();
    String domainName = TestDomain.DOMAIN.getName();
    final String clientTypeId = "EntityServletServerTest";
    UUID clientId = UUID.randomUUID();
    CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainName);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientId.toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();

    URIBuilder uriBuilder = createURIBuilder();
    uriBuilder.setPath("isTransactionOpen");

    HttpPost httpPost = new HttpPost(uriBuilder.build());
    HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
    CloseableHttpResponse response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    Boolean result = deserializeResponse(response);
    assertFalse(result);
    response.close();
  }

  @Test
  void testJson() throws URISyntaxException, IOException {
    RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(2000)
            .setConnectTimeout(2000)
            .build();

    String domainName = new TestDomain().getDomainType().getName();
    final String clientTypeId = "EntityServletServerTest";
    Value<UUID> clientIdValue = Value.value(UUID.randomUUID());
    HttpClientContext context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);

    CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainName);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            })
            .build();

    EntityObjectMapper entityObjectMapper = EntityObjectMapper.createEntityObjectMapper(ENTITIES);
    ConditionObjectMapper conditionObjectMapper = new ConditionObjectMapper(entityObjectMapper);
    URIBuilder uriBuilder = createJsonURIBuilder();

    //count
    uriBuilder.setPath("count");
    HttpPost httpPost = new HttpPost(uriBuilder.build());
    AttributeCondition<Integer> condition = where(TestDomain.DEPARTMENT_ID).equalTo(10);
    String conditionJson = conditionObjectMapper.writeValueAsString(condition);

    httpPost.setEntity(new StringEntity(conditionJson));

    CloseableHttpResponse response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertEquals(1, conditionObjectMapper.readValue(response.getEntity().getContent(), Integer.class));
    response.close();

    //select by key
    List<Key> keys = new ArrayList<>();
    Key key = ENTITIES.primaryKey(TestDomain.T_DEPARTMENT, 10);
    keys.add(key);
    key = ENTITIES.primaryKey(TestDomain.T_DEPARTMENT, 20);
    keys.add(key);

    uriBuilder.setPath("selectByKey");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(entityObjectMapper.writeValueAsString(keys)));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertEquals(2, entityObjectMapper.deserializeEntities(response.getEntity().getContent()).size());
    response.close();

    //select by condition
    Condition selectCondition = Conditions.condition(keys);

    uriBuilder.setPath("select");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(conditionObjectMapper.writeValueAsString(selectCondition)));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertEquals(2, entityObjectMapper.deserializeEntities(response.getEntity().getContent()).size());
    response.close();

    //insert
    List<Entity> entities = new ArrayList<>();
    entities.add(ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -10)
            .with(TestDomain.DEPARTMENT_NAME, "A name")
            .with(TestDomain.DEPARTMENT_LOCATION, "loc")
            .build());
    entities.add(ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -20)
            .with(TestDomain.DEPARTMENT_NAME, "Another name")
            .with(TestDomain.DEPARTMENT_LOCATION, "locat")
            .build());

    uriBuilder.setPath("insert");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(entityObjectMapper.writeValueAsString(entities)));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());

    keys = entityObjectMapper.deserializeKeys(response.getEntity().getContent());
    assertEquals(2, keys.size());
    assertTrue(entities.stream().map(Entity::getPrimaryKey).collect(Collectors.toList()).containsAll(keys));
    response.close();

    //update entities
    entities.get(0).put(TestDomain.DEPARTMENT_NAME, "newname");
    entities.get(1).put(TestDomain.DEPARTMENT_LOCATION, "newloc");

    uriBuilder.setPath("update");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(entityObjectMapper.writeValueAsString(entities)));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());

    List<Entity> updated = entityObjectMapper.deserializeEntities(response.getEntity().getContent());
    assertEquals(2, updated.size());
    assertTrue(updated.containsAll(entities));

    assertEquals("newname", updated.stream().filter(entity -> entity.get(TestDomain.DEPARTMENT_ID).equals(-10)).findFirst().get().get(TestDomain.DEPARTMENT_NAME));
    assertEquals("newloc", updated.stream().filter(entity -> entity.get(TestDomain.DEPARTMENT_ID).equals(-20)).findFirst().get().get(TestDomain.DEPARTMENT_LOCATION));
    response.close();

    //update condition
    UpdateCondition updateCondition = where(TestDomain.DEPARTMENT_ID)
            .between(-20, -10).toUpdateCondition().set(TestDomain.DEPARTMENT_LOCATION, "aloc");
    uriBuilder.setPath("updateByCondition");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(conditionObjectMapper.writeValueAsString(updateCondition)));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());

    Integer updateCount = entityObjectMapper.readValue(response.getEntity().getContent(), Integer.class);
    assertEquals(2, updateCount);
    response.close();

    //delete condition
    Condition deleteCondition = where(TestDomain.DEPARTMENT_ID).equalTo(-10);
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(conditionObjectMapper.writeValueAsString(deleteCondition)));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());

    Integer deleteCount = entityObjectMapper.readValue(response.getEntity().getContent(), Integer.class);
    assertEquals(1, deleteCount);
    response.close();

    //delete key
    uriBuilder.setPath("deleteByKey");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(entityObjectMapper.writeValueAsString(singletonList(keys.get(1)))));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    //select values
    condition = where(TestDomain.DEPARTMENT_ID).equalTo(10);
    ObjectNode node = entityObjectMapper.createObjectNode();
    node.set("attribute", conditionObjectMapper.valueToTree(TestDomain.DEPARTMENT_ID.getName()));
    node.set("entityType", conditionObjectMapper.valueToTree(TestDomain.DEPARTMENT_ID.getEntityType().getName()));
    node.set("condition", conditionObjectMapper.valueToTree(condition));

    uriBuilder.setPath("values");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(node.toString()));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertEquals(10, entityObjectMapper.readValue(response.getEntity().getContent(), new TypeReference<List<Integer>>() {}).get(0));
    response.close();

    //dependencies
    Key key1 = ENTITIES.primaryKey(TestDomain.T_DEPARTMENT, 10);
    Key key2 = ENTITIES.primaryKey(TestDomain.T_DEPARTMENT, 20);

    List<Entity> entitiesDep = Arrays.asList(Entity.entity(key1), Entity.entity(key2));

    uriBuilder.setPath("dependencies");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new StringEntity(entityObjectMapper.writeValueAsString(entitiesDep)));

    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    Map<String, Collection<Entity>> dependencies = entityObjectMapper.readValue(response.getEntity().getContent(),
            new TypeReference<Map<String, Collection<Entity>>>() {});
    assertEquals(1, dependencies.size());
    assertEquals(12, dependencies.get(TestDomain.T_EMP.getName()).size());
    response.close();

    //transactions
    uriBuilder.setPath("isTransactionOpen");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertFalse(entityObjectMapper.readValue(response.getEntity().getContent(), Boolean.class));

    uriBuilder.setPath("beginTransaction");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());

    uriBuilder.setPath("isTransactionOpen");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertTrue(entityObjectMapper.readValue(response.getEntity().getContent(), Boolean.class));

    uriBuilder.setPath("rollbackTransaction");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());

    uriBuilder.setPath("isTransactionOpen");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertFalse(entityObjectMapper.readValue(response.getEntity().getContent(), Boolean.class));

    uriBuilder.setPath("beginTransaction");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());

    uriBuilder.setPath("isTransactionOpen");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertTrue(entityObjectMapper.readValue(response.getEntity().getContent(), Boolean.class));

    uriBuilder.setPath("commitTransaction");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());

    uriBuilder.setPath("isTransactionOpen");
    httpPost = new HttpPost(uriBuilder.build());
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    assertFalse(entityObjectMapper.readValue(response.getEntity().getContent(), Boolean.class));

    //close
    uriBuilder.setPath("close");
    response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()), context);
    response.close();

    client.close();
  }

  @Test
  void test() throws URISyntaxException, IOException, InterruptedException, ClassNotFoundException {
    RequestConfig requestConfig = RequestConfig.custom()
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
    uriBuilder.addParameter("domainTypeName", TestDomain.DOMAIN.getName());
    CloseableHttpResponse response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    String domainName = new TestDomain().getDomainType().getName();
    final String clientTypeId = "EntityServletServerTest";
    //test with missing clientId header
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainName);
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

    Value<UUID> clientIdValue = Value.value(UUID.randomUUID());
    //test with unknown user authentication
    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainName);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    context = createHttpContext(User.user("who", "areu".toCharArray()), TARGET_HOST);
    response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()), context);
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();
    client.close();

    client = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(createConnectionManager())
            .addInterceptorFirst((HttpRequestInterceptor) (request, httpContext) -> {
              request.setHeader(EntityService.DOMAIN_TYPE_NAME, domainName);
              request.setHeader(EntityService.CLIENT_TYPE_ID, clientTypeId);
              request.setHeader(EntityService.CLIENT_ID, clientIdValue.get().toString());
              request.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            })
            .build();

    //select all/GET
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    HttpPost httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(condition(TestDomain.T_DEPARTMENT))));
    context = createHttpContext(UNIT_TEST_USER, TARGET_HOST);
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    List<Entity> queryEntities = deserializeResponse(response);
    assertEquals(4, queryEntities.size());
    response.close();

    Entity department = ENTITIES.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, null)
            .with(TestDomain.DEPARTMENT_ID, -42)
            .with(TestDomain.DEPARTMENT_NAME, "Test")
            .with(TestDomain.DEPARTMENT_LOCATION, "Location")
            .build();

    //insert
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("insert");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(department))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    List<Key> queryKeys = deserializeResponse(response);
    assertEquals(1, queryKeys.size());
    assertEquals(department.getPrimaryKey(), queryKeys.get(0));
    response.close();

    //delete
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(condition(department.getPrimaryKey()))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    //insert
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("insert");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(department))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    List<Key> keys = deserializeResponse(response);
    assertEquals(1, keys.size());
    assertEquals(department.getPrimaryKey(), keys.get(0));
    response.close();

    //update
    department.saveAll();
    department.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    department.put(TestDomain.DEPARTMENT_NAME, "New name");
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("update");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(department))));
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
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(where(TestDomain.DEPARTMENT_NAME).equalTo("New name"))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    response.close();

    //select by condition
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("select");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(condition(department.getPrimaryKey()))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryEntities = deserializeResponse(response);
    assertEquals(1, queryEntities.size());
    response.close();

    //delete
    uriBuilder = createURIBuilder();
    uriBuilder.setPath("delete");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(where(TestDomain.DEPARTMENT_ID).equalTo(-42))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("function");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(TestDomain.FUNCTION_ID))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(TestDomain.PROCEDURE_ID))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(200, response.getStatusLine().getStatusCode());
    response.close();

    Collection<RemoteClient> clients = admin.getClients(clientTypeId);
    assertEquals(1, clients.size());

    //try to change the clientId
    UUID originalClientId = clientIdValue.get();
    clientIdValue.set(UUID.randomUUID());

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("procedure");
    httpPost = new HttpPost(uriBuilder.build());
    httpPost.setEntity(new ByteArrayEntity(Serializer.serialize(singletonList(TestDomain.PROCEDURE_ID))));
    response = client.execute(TARGET_HOST, httpPost, context);
    assertEquals(401, response.getStatusLine().getStatusCode());
    response.close();

    clientIdValue.set(originalClientId);

    uriBuilder = createURIBuilder();
    uriBuilder.setPath("close");
    response = client.execute(TARGET_HOST, new HttpPost(uriBuilder.build()), context);
    response.close();

    client.close();

    clients = admin.getClients(clientTypeId);
    assertTrue(clients.isEmpty());
  }

  private static URIBuilder createURIBuilder() {
    URIBuilder builder = new URIBuilder();
    builder.setScheme(HTTPS).setHost(SERVER_BASEURL);

    return builder;
  }

  private static URIBuilder createJsonURIBuilder() {
    URIBuilder builder = new URIBuilder();
    builder.setScheme(HTTPS).setHost(SERVER_JSON_BASEURL);

    return builder;
  }

  private static <T> T deserializeResponse(CloseableHttpResponse response) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    response.getEntity().writeTo(outputStream);

    return Serializer.deserialize(outputStream.toByteArray());
  }

  private static HttpClientContext createHttpContext(User user, HttpHost targetHost) {
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
            new AuthScope(targetHost.getHostName(), targetHost.getPort()),
            new UsernamePasswordCredentials(user.getUsername(), String.valueOf(user.getPassword())));

    AuthCache authCache = new BasicAuthCache();
    authCache.put(targetHost, new BasicScheme());

    HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider(credentialsProvider);
    context.setAuthCache(authCache);

    return context;
  }

  private static BasicHttpClientConnectionManager createConnectionManager() {
    try {
      SSLContext sslContext = SSLContext.getDefault();

      return new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register(HTTPS,
                      new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
              .build());
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOST_NAME.set("localhost");
    Clients.TRUSTSTORE.set("../../framework/server/src/main/config/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    Clients.resolveTrustStore();
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    HttpServerConfiguration.HTTP_SERVER_PORT.set(WEB_SERVER_PORT_NUMBER);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set("../../framework/server/src/main/config/keystore.jks");
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(true);
    System.setProperty("java.security.policy", "../../framework/server/src/main/config/all_permissions.policy");

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .adminUser(User.parse("scott:tiger"))
            .domainModelClassNames(singletonList(TestDomain.class.getName()))
            .database(DatabaseFactory.getDatabase())
            .sslEnabled(false)
            .auxiliaryServerFactoryClassNames(singletonList(EntityServletServerFactory.class.getName()))
            .build();
  }

  private static void deconfigure() {
    Clients.SERVER_HOST_NAME.set(null);
    Clients.TRUSTSTORE.set(null);
    Clients.TRUSTSTORE_PASSWORD.set(null);
    System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE);
    System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE_PASSWORD);
    ServerConfiguration.RMI_SERVER_HOSTNAME.set(null);
    ServerConfiguration.AUXILIARY_SERVER_FACTORY_CLASS_NAMES.set(null);
    HttpServerConfiguration.HTTP_SERVER_PORT.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(false);
    System.clearProperty("java.security.policy");
  }
}
