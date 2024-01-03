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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.loadtest;

import is.codion.common.db.database.Database;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
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
    server = (Server<?, EntityServerAdmin>) LocateRegistry.getRegistry(Clients.SERVER_HOSTNAME.get(),
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
        protected void perform(SwingEntityApplicationModel application) {}
      }, new AbstractEntityUsageScenario<SwingEntityApplicationModel>("2") {
        @Override
        protected void perform(SwingEntityApplicationModel application) {}
      }));
    }

    @Override
    protected SwingEntityApplicationModel createApplication(User user) {
      return new SwingEntityApplicationModel(EntityConnectionProvider.builder()
              .domainType(TestDomain.DOMAIN)
              .clientTypeId(EntityLoadTestModelTest.class.getSimpleName())
              .user(user)
              .build());
    }
  }

  @Test
  void setLoginDelayFactorNegative() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel().loadTestModel().loginDelayFactor().set(-1));
  }

  @Test
  void setUpdateIntervalNegative() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel().loadTestModel().setUpdateInterval(-1));
  }

  @Test
  void testLoadTesting() throws Exception {
    LoadTestModel<?> loadTest = new TestLoadTestModel().loadTestModel();

    loadTest.collectChartData().set(true);
    loadTest.setUpdateInterval(350);
    loadTest.loginDelayFactor().set(1);

    assertEquals(350, loadTest.getUpdateInterval());
    assertEquals(1, loadTest.loginDelayFactor().get());

    loadTest.setWeight("1", 1);
    loadTest.setWeight("2", 0);

    loadTest.minimumThinkTime().set(50);
    loadTest.maximumThinkTime().set(100);

    loadTest.applicationBatchSize().set(2);
    assertEquals(2, loadTest.applicationBatchSize().get());

    loadTest.addApplicationBatch();

    Thread.sleep(1500);

    assertEquals(2, loadTest.applicationCount().get(),
            "Two clients expected, if this fails try increasing the Thread.sleep() value above");
    assertTrue(loadTest.usageScenario("1").totalRunCount() > 0);
    assertTrue(loadTest.usageScenario("1").successfulRunCount() > 0);
    assertEquals(0, loadTest.usageScenario("1").unsuccessfulRunCount());
    assertEquals(0, loadTest.usageScenario("2").totalRunCount());

    loadTest.paused().set(true);

    loadTest.clearCharts();

    loadTest.applicationBatchSize().set(1);
    loadTest.removeApplicationBatch();
    assertEquals(1, loadTest.applicationCount().get());
    loadTest.shutdown();

    Thread.sleep(500);

    assertEquals(0, loadTest.applicationCount().get());
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOSTNAME.set("localhost");
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .adminUser(User.parse("scott:tiger"))
            .connectionPoolUsers(Collections.singletonList(UNIT_TEST_USER))
            .clientTypeIdleConnectionTimeouts(Collections.singletonMap("ClientTypeID", 10000))
            .domainClassNames(Collections.singletonList("is.codion.swing.framework.model.tools.loadtest.TestDomain"))
            .database(Database.instance())
            .sslEnabled(false)
            .build();
  }
}
