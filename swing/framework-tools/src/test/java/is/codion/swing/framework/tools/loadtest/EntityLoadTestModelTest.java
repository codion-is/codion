/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.loadtest;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityLoadTestModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final String CONNECTION_TYPE_BEFORE_TEST = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();

  private static final User ADMIN_USER = User.parse("scott:tiger");
  private static Server<?, EntityServerAdmin> server;
  private static EntityServerAdmin admin;

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    EntityServerConfiguration configuration = configure();
    EntityServer.startServer(configuration);
    server = (Server<?, EntityServerAdmin>) LocateRegistry.getRegistry(Clients.SERVER_HOST_NAME.get(),
            configuration.registryPort()).lookup(configuration.serverName());
    admin = server.serverAdmin(ADMIN_USER);
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
  }

  @AfterAll
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(CONNECTION_TYPE_BEFORE_TEST);
  }

  private static final class TestLoadTestModel extends EntityLoadTestModel<SwingEntityApplicationModel> {

    public TestLoadTestModel() {
      super(UNIT_TEST_USER, asList(new AbstractEntityUsageScenario<SwingEntityApplicationModel>("1") {
        @Override
        protected void perform(SwingEntityApplicationModel application) throws Exception {}
      }, new AbstractEntityUsageScenario<SwingEntityApplicationModel>("2") {
        @Override
        protected void perform(SwingEntityApplicationModel application) throws Exception {}
      }));
    }

    @Override
    protected SwingEntityApplicationModel initializeApplication() {
      return new SwingEntityApplicationModel(EntityConnectionProvider.builder()
              .domainClassName(TestDomain.class.getName())
              .clientTypeId(EntityLoadTestModelTest.class.getSimpleName())
              .user(getUser())
              .build());
    }
  }

  @Test
  void setLoginDelayFactorNegative() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel().loginDelayFactorValue().set(-1));
  }

  @Test
  void setUpdateIntervalNegative() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel().setUpdateInterval(-1));
  }

  @Test
  void testLoadTesting() throws Exception {
    TestLoadTestModel loadTest = new TestLoadTestModel();

    loadTest.collectChartDataState().set(true);
    loadTest.setUpdateInterval(350);
    loadTest.loginDelayFactorValue().set(1);

    assertEquals(350, loadTest.getUpdateInterval());
    assertEquals(1, loadTest.loginDelayFactorValue().get());

    loadTest.setWeight("1", 1);
    loadTest.setWeight("2", 0);

    loadTest.minimumThinkTimeValue().set(50);
    loadTest.maximumThinkTimeValue().set(100);

    loadTest.applicationBatchSizeValue().set(2);
    assertEquals(2, loadTest.applicationBatchSizeValue().get());

    loadTest.addApplicationBatch();

    Thread.sleep(1500);

    assertEquals(2, loadTest.applicationCount(),
            "Two clients expected, if this fails try increasing the Thread.sleep() value above");
    assertTrue(loadTest.usageScenario("1").totalRunCount() > 0);
    assertTrue(loadTest.usageScenario("1").successfulRunCount() > 0);
    assertEquals(0, loadTest.usageScenario("1").unsuccessfulRunCount());
    assertEquals(0, loadTest.usageScenario("2").totalRunCount());

    loadTest.pausedState().set(true);

    loadTest.resetChartData();

    loadTest.applicationBatchSizeValue().set(1);
    loadTest.removeApplicationBatch();
    assertEquals(1, loadTest.applicationCount());
    loadTest.shutdown();

    Thread.sleep(500);

    assertEquals(0, loadTest.applicationCount());
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOST_NAME.set("localhost");
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .adminUser(User.parse("scott:tiger"))
            .connectionPoolUsers(Collections.singletonList(UNIT_TEST_USER))
            .clientTypeIdleConnectionTimeouts(Collections.singletonMap("ClientTypeID", 10000))
            .domainModelClassNames(Collections.singletonList("is.codion.swing.framework.tools.loadtest.TestDomain"))
            .database(DatabaseFactory.getDatabase())
            .sslEnabled(false)
            .build();
  }
}
