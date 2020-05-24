/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.loadtest;

import is.codion.common.db.database.Databases;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.EntityConnectionProviders;
import is.codion.framework.model.DefaultEntityApplicationModel;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.swing.common.tools.loadtest.ScenarioException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityLoadTestModelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final String CONNECTION_TYPE_BEFORE_TEST = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();

  private static final User ADMIN_USER = Users.parseUser("scott:tiger");
  private static Server<?, EntityServerAdmin> server;
  private static EntityServerAdmin admin;

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    final EntityServerConfiguration configuration = configure();
    EntityServer.startServer(configuration);
    server = (Server) LocateRegistry.getRegistry(ServerConfiguration.SERVER_HOST_NAME.get(),
            configuration.getRegistryPort()).lookup(configuration.getServerName());
    admin = server.getServerAdmin(ADMIN_USER);
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
  }

  @AfterAll
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(CONNECTION_TYPE_BEFORE_TEST);
  }

  private static final class TestLoadTestModel extends EntityLoadTestModel<DefaultEntityApplicationModel> {

    public TestLoadTestModel() {
      super(UNIT_TEST_USER, asList(new EntityLoadTestModel.AbstractEntityUsageScenario<DefaultEntityApplicationModel>("1") {
        @Override
        protected void perform(final DefaultEntityApplicationModel application) throws ScenarioException {}
      }, new EntityLoadTestModel.AbstractEntityUsageScenario<DefaultEntityApplicationModel>("2") {
        @Override
        protected void perform(final DefaultEntityApplicationModel application) throws ScenarioException {}
      }));
    }

    @Override
    protected DefaultEntityApplicationModel initializeApplication() {
      return new DefaultEntityApplicationModel(EntityConnectionProviders.connectionProvider().setDomainClassName(TestDomain.class.getName())
              .setClientTypeId(EntityLoadTestModelTest.class.getSimpleName()).setUser(getUser()));
    }
  }

  @Test
  public void setLoginDelayFactorNegative() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel().setLoginDelayFactor(-1));
  }

  @Test
  public void setUpdateIntervalNegative() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel().setUpdateInterval(-1));
  }

  @Test
  public void testLoadTesting() throws Exception {
    final TestLoadTestModel loadTest = new TestLoadTestModel();

    loadTest.setCollectChartData(true);
    loadTest.setUpdateInterval(350);
    loadTest.setLoginDelayFactor(0);

    assertTrue(loadTest.isCollectChartData());
    assertEquals(350, loadTest.getUpdateInterval());
    assertEquals(0, loadTest.getLoginDelayFactor());

    loadTest.setWeight("1", 1);
    loadTest.setWeight("2", 0);

    loadTest.setMaximumThinkTime(100);
    loadTest.setMinimumThinkTime(50);

    loadTest.setApplicationBatchSize(2);
    assertEquals(2, loadTest.getApplicationBatchSize());

    loadTest.addApplicationBatch();

    Thread.sleep(1500);

    assertEquals(2, loadTest.getApplicationCount(),
            "Two clients expected, if this fails try increasing the Thread.sleep() value above");
    assertTrue(loadTest.getUsageScenario("1").getTotalRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("1").getSuccessfulRunCount() > 0);
    assertEquals(0, loadTest.getUsageScenario("1").getUnsuccessfulRunCount());
    assertEquals(0, loadTest.getUsageScenario("2").getTotalRunCount());

    loadTest.setPaused(true);
    assertTrue(loadTest.isPaused());

    loadTest.resetChartData();

    loadTest.setApplicationBatchSize(1);
    loadTest.removeApplicationBatch();
    assertEquals(1, loadTest.getApplicationCount());
    loadTest.shutdown();

    Thread.sleep(500);

    assertFalse(loadTest.isPaused());
    assertEquals(0, loadTest.getApplicationCount());
  }

  private static EntityServerConfiguration configure() {
    ServerConfiguration.SERVER_HOST_NAME.set("localhost");
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    final EntityServerConfiguration configuration = EntityServerConfiguration.configuration(2223, 2221);
    configuration.setAdminPort(2223);
    configuration.setAdminUser(Users.parseUser("scott:tiger"));
    configuration.setStartupPoolUsers(Collections.singletonList(UNIT_TEST_USER));
    configuration.setClientSpecificConnectionTimeouts(Collections.singletonMap("ClientTypeID", 10000));
    configuration.setDomainModelClassNames(Collections.singletonList("is.codion.swing.framework.tools.loadtest.TestDomain"));
    configuration.setDatabase(Databases.getInstance());
    configuration.setSslEnabled(false);

    return configuration;
  }
}
