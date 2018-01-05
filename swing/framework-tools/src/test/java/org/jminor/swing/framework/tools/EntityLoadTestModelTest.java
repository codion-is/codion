/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.tools;

import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.server.Server;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.model.DefaultEntityApplicationModel;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.registry.LocateRegistry;
import java.util.Arrays;

import static org.junit.Assert.*;

public class EntityLoadTestModelTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final String CONNECTION_TYPE_BEFORE_TEST = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();

  private static final User ADMIN_USER = new User("scott", "tiger");
  private static Server<?, EntityConnectionServerAdmin> server;
  private static EntityConnectionServerAdmin admin;

  @BeforeClass
  public static synchronized void setUp() throws Exception {
    configure();
    final Database database = Databases.getInstance();
    final String serverName = Server.SERVER_NAME_PREFIX.get() + " " + Version.getVersionString()
            + "@" + (database.getSid() != null ? database.getSid().toUpperCase() : database.getHost().toUpperCase());
    DefaultEntityConnectionServer.startServer();
    server = (Server) LocateRegistry.getRegistry((Server.SERVER_HOST_NAME.get()),
            Server.REGISTRY_PORT.get()).lookup(serverName);
    admin = server.getServerAdmin(ADMIN_USER);
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
  }

  @AfterClass
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(CONNECTION_TYPE_BEFORE_TEST);
  }

  private static final class TestLoadTestModel extends EntityLoadTestModel<DefaultEntityApplicationModel> {

    public TestLoadTestModel() {
      super(UNIT_TEST_USER, Arrays.asList(new EntityLoadTestModel.AbstractEntityUsageScenario<DefaultEntityApplicationModel>("1") {
        @Override
        protected void performScenario(final DefaultEntityApplicationModel application) throws ScenarioException {}
      }, new EntityLoadTestModel.AbstractEntityUsageScenario<DefaultEntityApplicationModel>("2") {
        @Override
        protected void performScenario(final DefaultEntityApplicationModel application) throws ScenarioException {}
      }));
    }

    @Override
    protected DefaultEntityApplicationModel initializeApplication() {
      return new DefaultEntityApplicationModel(EntityConnectionProviders.connectionProvider(TestDomain.class.getName(), getUser(),
              EntityLoadTestModelTest.class.getSimpleName()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void setLoginDelayFactorNegative() {
    new TestLoadTestModel().setLoginDelayFactor(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setUpdateIntervalNegative() {
    new TestLoadTestModel().setUpdateInterval(-1);
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
    loadTest.setWarningTime(10);

    loadTest.setApplicationBatchSize(2);
    assertEquals(2, loadTest.getApplicationBatchSize());

    loadTest.addApplicationBatch();

    Thread.sleep(1500);

    assertEquals("Two clients expected, if this fails try increasing the Thread.sleep() value above",
            2, loadTest.getApplicationCount());
    assertTrue(loadTest.getUsageScenario("1").getTotalRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("1").getSuccessfulRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("1").getUnsuccessfulRunCount() == 0);
    assertTrue(loadTest.getUsageScenario("2").getTotalRunCount() == 0);

    loadTest.setPaused(true);
    assertTrue(loadTest.isPaused());

    loadTest.resetChartData();

    loadTest.setApplicationBatchSize(1);
    loadTest.removeApplicationBatch();
    assertEquals(1, loadTest.getApplicationCount());
    loadTest.exit();

    Thread.sleep(500);

    assertFalse(loadTest.isPaused());
    assertEquals(0, loadTest.getApplicationCount());
  }

  private static void configure() {
    Server.REGISTRY_PORT.set(2221);
    Server.SERVER_PORT.set(2223);
    Server.SERVER_HOST_NAME.set("localhost");
    Server.SERVER_ADMIN_PORT.set(2223);
    Server.SERVER_ADMIN_USER.set("scott:tiger");
    DefaultEntityConnectionServer.SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS.set(UNIT_TEST_USER.getUsername() + ":" + UNIT_TEST_USER.getPassword());
    DefaultEntityConnectionServer.SERVER_CLIENT_CONNECTION_TIMEOUT.set("ClientTypeID:10000");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set("org.jminor.swing.framework.tools.TestDomain");
    Server.SERVER_CONNECTION_SSL_ENABLED.set(false);
    Server.RMI_SERVER_HOSTNAME.set("localhost");
  }
}
