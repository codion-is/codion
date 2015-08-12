/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.ClientUtil;
import org.jminor.common.server.ConnectionInfo;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.ServerUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.RemoteEntityConnection;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.plugins.json.EntityJSONParser;
import org.jminor.framework.plugins.rest.EntityRESTService;

import ch.qos.logback.classic.Level;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.RequestDefaultHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
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
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.Assert.*;

public class EntityConnectionServerTest {

  private static final int WEB_SERVER_PORT_NUMBER = 8089;
  private static final String HOSTNAME = Configuration.getStringValue(Configuration.SERVER_HOST_NAME);
  private static final String REST_BASEURL = HOSTNAME + ":" + WEB_SERVER_PORT_NUMBER + "/entities/";
  private static final String BASIC = "Basic ";
  private static final String HTTP = "http";

  private static EntityConnectionServer server;
  private static DefaultEntityConnectionServerAdmin admin;

  public static EntityConnectionServerAdmin getServerAdmin() {
    return admin;
  }

  @BeforeClass
  public static synchronized void setUp() throws Exception {
    configure();
    DefaultEntityConnectionServerAdmin.startServer();
    EntityConnectionServerTest.admin = DefaultEntityConnectionServerAdmin.getInstance();
    EntityConnectionServerTest.server = admin.getServer();
  }

  @AfterClass
  public static synchronized void tearDown() throws Exception {
    DefaultEntityConnectionServerAdmin.shutdownServer();
    deconfigure();
    admin = null;
    server = null;
  }

  @Test(expected = RuntimeException.class)
  public void testWrongPassword() throws Exception {
    new RemoteEntityConnectionProvider(new User(User.UNIT_TEST_USER.getUsername(), "foobar"), UUID.randomUUID(), getClass().getSimpleName()).getConnection();
  }

  @Test
  public void test() throws Exception {
    final RemoteEntityConnectionProvider providerOne = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER,
            UUID.randomUUID(), getClass().getSimpleName());
    final EntityConnection remoteConnectionOne = providerOne.getConnection();
    assertTrue(remoteConnectionOne.isValid());
    assertEquals(1, admin.getConnectionCount());
    admin.setPoolConnectionThreshold(User.UNIT_TEST_USER, 505);
    assertEquals(505, admin.getPoolConnectionThreshold(User.UNIT_TEST_USER));
    admin.setPooledConnectionTimeout(User.UNIT_TEST_USER, 60005);
    assertEquals(60005, admin.getPooledConnectionTimeout(User.UNIT_TEST_USER));
    admin.setMaximumPoolCheckOutTime(User.UNIT_TEST_USER, 2005);
    assertEquals(2005, admin.getMaximumPoolCheckOutTime(User.UNIT_TEST_USER));

    final RemoteEntityConnectionProvider providerTwo = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER,
            UUID.randomUUID(), getClass().getSimpleName());
    final EntityConnection remoteConnectionTwo = providerTwo.getConnection();
    admin.setLoggingEnabled(providerOne.getClientID(), true);
    assertTrue(admin.isLoggingEnabled(providerOne.getClientID()));
    assertFalse(admin.isLoggingEnabled(UUID.randomUUID()));
    admin.setLoggingEnabled(UUID.randomUUID(), true);
    assertTrue(remoteConnectionTwo.isValid());
    assertEquals(2, admin.getConnectionCount());

    Collection<ClientInfo> clients = admin.getClients(new User(User.UNIT_TEST_USER.getUsername(), null));
    assertEquals(2, clients.size());
    clients = admin.getClients(getClass().getSimpleName());
    assertEquals(2, clients.size());
    final Collection<String> clientTypes = admin.getClientTypes();
    assertEquals(1, clientTypes.size());
    assertTrue(clientTypes.contains(getClass().getSimpleName()));

    final Collection<User> users = admin.getUsers();
    assertEquals(1, users.size());
    assertEquals(User.UNIT_TEST_USER, users.iterator().next());

    providerTwo.getConnection().selectMany(EntityCriteriaUtil.selectCriteria(TestDomain.T_EMP));

    final Database.Statistics stats = admin.getDatabaseStatistics();
    assertNotNull(stats.getTimestamp());
    assertNotNull(stats.getQueriesPerSecond());

    final ClientLog log = admin.getClientLog(providerTwo.getClientID());
    assertNotNull(log.getConnectionCreationDate());
    assertNull(admin.getClientLog(UUID.randomUUID()));

    final MethodLogger.Entry entry = log.getEntries().get(0);
    assertEquals("getConnection", entry.getMethod());
    assertTrue(entry.getDelta() >= 0);

    admin.removeConnections(true);

    providerOne.disconnect();
    assertEquals(1, admin.getConnectionCount());

    providerTwo.disconnect();
    assertEquals(0, admin.getConnectionCount());

    server.setConnectionLimit(1);
    providerOne.getConnection();
    try {
      providerTwo.getConnection();
      fail("Server should be full");
    }
    catch (final RuntimeException ignored) {/*ignored*/}

    assertEquals(1, admin.getConnectionCount());
    admin.setConnectionLimit(2);
    providerTwo.getConnection();
    assertEquals(2, admin.getConnectionCount());

    admin.getServer().getServerLoad();

    providerOne.disconnect();
    assertEquals(1, admin.getConnectionCount());
    providerTwo.disconnect();
    assertEquals(0, admin.getConnectionCount());

    //testing with the EmpDeptLoginProxy
    admin.setConnectionLimit(3);
    assertEquals(3, admin.getConnectionLimit());
    final String empDeptClientTypeID = "TestLoginProxy";
    final RemoteEntityConnectionProvider empDeptProviderJohn = new RemoteEntityConnectionProvider(new User("john", "hello"),
            UUID.randomUUID(), empDeptClientTypeID);
    final RemoteEntityConnectionProvider empDeptProviderHelen = new RemoteEntityConnectionProvider(new User("helen", "juno"),
            UUID.randomUUID(), empDeptClientTypeID);
    final RemoteEntityConnectionProvider empDeptProviderInvalid = new RemoteEntityConnectionProvider(new User("foo", "bar"),
            UUID.randomUUID(), empDeptClientTypeID);
    empDeptProviderJohn.getConnection();
    empDeptProviderHelen.getConnection();
    try {
      empDeptProviderInvalid.getConnection();
      fail("Should not be able to connect with an invalid user");
    }
    catch (final Exception ignored) {/*ignored*/}
    final Collection<ClientInfo> empDeptClients = admin.getClients(empDeptClientTypeID);
    assertEquals(2, empDeptClients.size());
    for (final ClientInfo empDeptClient : empDeptClients) {
      assertEquals(User.UNIT_TEST_USER, empDeptClient.getDatabaseUser());
    }
    empDeptProviderJohn.disconnect();
    assertEquals(1, admin.getConnectionCount());
    empDeptProviderHelen.disconnect();
    assertEquals(0, admin.getConnectionCount());

    try {
      admin.setConnectionTimeout(-1);
      fail();
    }
    catch (final IllegalArgumentException ignored) {/*ignored*/}
  }

  @Test
  public void testWebServer() throws Exception {
    try (final InputStream input = new URL("http://localhost:" + WEB_SERVER_PORT_NUMBER + "/db/scripts/create_h2_db.sql").openStream()) {
      assertTrue(input.read() > 0);
    }
  }

  @Test
  public void testLoginProxy() throws ServerException.ServerFullException, ServerException.LoginException, RemoteException {
    final String clientTypeID = "loginProxyTestClient";
    //create login proxy which returns a ClientInfo with databaseUser scott:tiger for authenticated users
    final LoginProxy proxy = new LoginProxy() {
      @Override
      public String getClientTypeID() {
        return clientTypeID;
      }
      @Override
      public ClientInfo doLogin(final ClientInfo clientInfo) {
        return ServerUtil.clientInfo(clientInfo.getConnectionInfo(), User.UNIT_TEST_USER);
      }
      @Override
      public void doLogout(final ClientInfo clientInfo) {}
      @Override
      public void close() {}
    };

    server.setLoginProxy(clientTypeID, proxy);

    final User userOne = new User("foo", "bar");
    final ConnectionInfo clientOne = ClientUtil.connectionInfo(userOne, UUID.randomUUID(), clientTypeID);

    final User userTwo = new User("bar", "foo");
    final ConnectionInfo clientTwo = ClientUtil.connectionInfo(userTwo, UUID.randomUUID(), clientTypeID);

    final RemoteEntityConnection connectionOne = server.connect(clientOne);
    assertEquals(userOne, connectionOne.getUser());

    Collection<ClientInfo> clients = server.getClients(clientTypeID);
    assertEquals(1, clients.size());
    final ClientInfo clientOneFromServer = clients.iterator().next();
    assertEquals(userOne, clientOneFromServer.getUser());
    assertEquals(User.UNIT_TEST_USER, clientOneFromServer.getDatabaseUser());

    final RemoteEntityConnection connectionTwo = server.connect(clientTwo);
    assertEquals(userTwo, connectionTwo.getUser());

    clients = server.getClients(clientTypeID);
    assertEquals(2, clients.size());

    boolean found = false;
    for (final ClientInfo clientInfo : server.getClients(clientTypeID)) {
      if (clientInfo.getClientID().equals(clientTwo.getClientID())) {
        found = true;
        assertEquals(User.UNIT_TEST_USER, clientInfo.getDatabaseUser());
      }
    }
    assertTrue("Client two should have been returned from server", found);

    server.disconnect(clientOne.getClientID());
    server.disconnect(clientTwo.getClientID());
  }

  @Test
  public void coverAdmin() throws RemoteException {
    final EntityConnectionServerAdmin admin = getServerAdmin();
    admin.setWarningTimeThreshold(300);
    assertEquals(300, admin.getWarningTimeThreshold());
    admin.getActiveConnectionCount();
    admin.getAllocatedMemory();
    admin.setConnectionTimeout(30);
    assertEquals(30, admin.getConnectionTimeout());
    admin.getDatabaseStatistics();
    admin.getDatabaseURL();
    admin.getConnectionPools();
    admin.getEntityDefinitions();
    admin.setLoggingLevel(Level.INFO);
    assertEquals(Level.INFO, admin.getLoggingLevel());
    admin.setMaintenanceInterval(500);
    assertEquals(500, admin.getMaintenanceInterval());
    admin.getMaxMemory();
    admin.getMemoryUsage();
    admin.getRequestsPerSecond();
    admin.getServerInfo();
    admin.getSystemProperties();
    admin.getUsedMemory();
    admin.getUsers();
    admin.getWarningTimeExceededPerSecond();
    admin.getWarningTimeThreshold();
  }

  @Test
  public void testREST() throws URISyntaxException, IOException, JSONException, ParseException, InterruptedException {
    final SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme(HTTP, WEB_SERVER_PORT_NUMBER, PlainSocketFactory.getSocketFactory()));
    final HttpParams params = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(params, 2000);
    HttpConnectionParams.setSoTimeout(params, 2000);
    DefaultHttpClient client = new DefaultHttpClient(new PoolingClientConnectionManager(schemeRegistry), params);

    //test with missing authentication info
    URIBuilder builder = createURIBuilder();
    builder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT);
    HttpResponse response = client.execute(new HttpGet(builder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());

    //test with unknown user authentication
    client.addRequestInterceptor(new RequestDefaultHeaders() {
      @Override
      public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
        final User user = new User("who", "areu");
        request.setHeader(EntityRESTService.AUTHORIZATION, BASIC + DatatypeConverter.printBase64Binary((user.getUsername() + ":" + user.getPassword()).getBytes()));
        request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
      }
    });
    builder = createURIBuilder();
    builder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT);
    response = client.execute(new HttpGet(builder.build()));
    assertEquals(401, response.getStatusLine().getStatusCode());

    client = new DefaultHttpClient(new PoolingClientConnectionManager(schemeRegistry), params);
    client.addRequestInterceptor(new RequestDefaultHeaders() {
      @Override
      public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
        final User user = User.UNIT_TEST_USER;
        request.setHeader(EntityRESTService.AUTHORIZATION, BASIC + DatatypeConverter.printBase64Binary((user.getUsername() + ":" + user.getPassword()).getBytes()));
        request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
      }
    });

    //select all/GET
    builder = createURIBuilder();
    builder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT);
    response = client.execute(new HttpGet(builder.build()));
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
    builder = createURIBuilder();
    builder.addParameter("entities", EntityJSONParser.serializeEntities(Collections.singletonList(department), false));
    response = client.execute(new HttpPost(builder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    final List<Entity.Key> queryKeys = EntityJSONParser.deserializeKeys(queryResult);
    assertEquals(1, queryKeys.size());
    assertEquals(department.getPrimaryKey(), queryKeys.get(0));

    //delete/DELETE by key
    builder = createURIBuilder();
    builder.setPath(EntityRESTService.BY_KEY_PATH).addParameter("primaryKeys", EntityJSONParser.serializeKeys(Collections.singletonList(department.getPrimaryKey())));
    response = client.execute(new HttpDelete(builder.build()));
    queryResult = getContentStream(response.getEntity());

    //insert/PUT
    builder = createURIBuilder();
    builder.addParameter("entities", EntityJSONParser.serializeEntities(Collections.singletonList(department), false));
    response = client.execute(new HttpPut(builder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));
    department = queryEntities.get(0);

    //update/PUT
    department.setValue(TestDomain.DEPARTMENT_LOCATION, "New location");
    department.setValue(TestDomain.DEPARTMENT_NAME, "New name");
    builder = createURIBuilder();
    builder.addParameter("entities", EntityJSONParser.serializeEntities(Collections.singletonList(department), false));
    response = client.execute(new HttpPut(builder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());
    assertEquals(department, queryEntities.get(0));

    //select/GET by value
    builder = createURIBuilder();
    builder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT)
            .addParameter("searchType", SearchType.LIKE.toString())
            .addParameter("values", "{\"dname\":\"New name\"}");
    response = client.execute(new HttpGet(builder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());

    //select/GET by key
    builder = createURIBuilder();
    builder.setPath(EntityRESTService.BY_KEY_PATH).addParameter("primaryKeys", EntityJSONParser.serializeKeys(Collections.singletonList(department.getPrimaryKey())));
    response = client.execute(new HttpGet(builder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());
    queryResult = getContentStream(response.getEntity());
    queryEntities = EntityJSONParser.deserializeEntities(queryResult);
    assertEquals(1, queryEntities.size());

    //delete/DELETE by value
    builder = createURIBuilder();
    builder.setPath(EntityRESTService.BY_VALUE_PATH)
            .addParameter("entityID", TestDomain.T_DEPARTMENT)
            .addParameter("searchType", SearchType.LIKE.toString())
            .addParameter("values", "{\"deptno\":\"-42\"}");
    response = client.execute(new HttpDelete(builder.build()));
    assertEquals(200, response.getStatusLine().getStatusCode());

    final EntityConnectionServerAdmin admin = getServerAdmin();
    final Collection<ClientInfo> clients = admin.getClients(EntityRESTService.class.getName());
    assertEquals(1, clients.size());

    admin.disconnect(clients.iterator().next().getClientID());
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
    Configuration.setValue(Configuration.SERVER_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_HOST_NAME, "localhost");
    Configuration.setValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL, User.UNIT_TEST_USER.getUsername() + ":" + User.UNIT_TEST_USER.getPassword());
    Configuration.setValue(Configuration.SERVER_CLIENT_CONNECTION_TIMEOUT, "ClientTypeID:10000");
    Configuration.setValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES, "org.jminor.framework.domain.TestDomain");
    Configuration.setValue(Configuration.SERVER_LOGIN_PROXY_CLASSES, "org.jminor.framework.server.TestLoginProxy");
    Configuration.setValue(Configuration.WEB_SERVER_DOCUMENT_ROOT, System.getProperty("user.dir") + System.getProperty("file.separator") + "resources");
    Configuration.setValue(Configuration.WEB_SERVER_PORT, WEB_SERVER_PORT_NUMBER);
    Configuration.setValue("java.rmi.server.hostname", "localhost");
    Configuration.setValue("java.security.policy", "resources/security/all_permissions.policy");
    Configuration.setValue("javax.net.ssl.trustStore", "resources/security/JMinorClientTruststore");
    Configuration.setValue("javax.net.ssl.keyStore", "resources/security/JMinorServerKeystore");
    Configuration.setValue("javax.net.ssl.keyStorePassword", "crappypass");
  }

  private static void deconfigure() {
    Configuration.setValue(Configuration.REGISTRY_PORT, Registry.REGISTRY_PORT);
    Configuration.clearValue(Configuration.SERVER_PORT);
    Configuration.clearValue(Configuration.SERVER_ADMIN_PORT);
    Configuration.clearValue(Configuration.SERVER_HOST_NAME);
    Configuration.clearValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    Configuration.clearValue(Configuration.SERVER_CLIENT_CONNECTION_TIMEOUT);
    Configuration.clearValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    Configuration.clearValue(Configuration.SERVER_LOGIN_PROXY_CLASSES);
    Configuration.clearValue(Configuration.WEB_SERVER_DOCUMENT_ROOT);
    Configuration.clearValue(Configuration.WEB_SERVER_PORT);
    Configuration.clearValue("java.rmi.server.hostname");
    Configuration.clearValue("java.security.policy");
    Configuration.clearValue("javax.net.ssl.trustStore");
    Configuration.clearValue("javax.net.ssl.keyStore");
    Configuration.clearValue("javax.net.ssl.keyStorePassword");
  }
}
