/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.Scenario;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.framework.demos.chinook.testing.scenarios.InsertDeleteAlbum;
import is.codion.framework.demos.chinook.testing.scenarios.InsertDeleteInvoice;
import is.codion.framework.demos.chinook.testing.scenarios.LogoutLogin;
import is.codion.framework.demos.chinook.testing.scenarios.RaisePrices;
import is.codion.framework.demos.chinook.testing.scenarios.RandomPlaylist;
import is.codion.framework.demos.chinook.testing.scenarios.UpdateTotals;
import is.codion.framework.demos.chinook.testing.scenarios.ViewAlbum;
import is.codion.framework.demos.chinook.testing.scenarios.ViewCustomerReport;
import is.codion.framework.demos.chinook.testing.scenarios.ViewGenre;
import is.codion.framework.demos.chinook.testing.scenarios.ViewInvoice;
import is.codion.framework.demos.chinook.ui.ChinookAppPanel;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;

import java.util.Collection;
import java.util.function.Function;

import static is.codion.common.model.loadtest.LoadTest.Scenario.scenario;
import static is.codion.swing.common.model.tools.loadtest.LoadTestModel.loadTestModel;
import static java.util.Arrays.asList;

public final class ChinookLoadTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Collection<Scenario<EntityConnectionProvider>> SCENARIOS = asList(
          scenario(new ViewGenre(), 10),
          scenario(new ViewCustomerReport(), 2),
          scenario(new ViewInvoice(), 10),
          scenario(new ViewAlbum(), 10),
          scenario(new UpdateTotals(), 1),
          scenario(new InsertDeleteAlbum(), 3),
          scenario(new LogoutLogin(), 1),
          scenario(new RaisePrices(), 1),
          scenario(new RandomPlaylist(), 1),
          scenario(new InsertDeleteInvoice(), 3));

  private static final class ConnectionProviderFactory implements Function<User, EntityConnectionProvider> {

    @Override
    public EntityConnectionProvider apply(User user) {
      EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
              .domainType(Chinook.DOMAIN)
              .clientTypeId(ChinookAppPanel.class.getName())
              .clientVersion(ChinookAppModel.VERSION)
              .user(user)
              .build();
      connectionProvider.connection();

      return connectionProvider;
    }
  }

  public static void main(String[] args) {
    LoadTest<EntityConnectionProvider> loadTest =
            LoadTest.builder(new ConnectionProviderFactory(), EntityConnectionProvider::close)
                    .scenarios(SCENARIOS)
                    .user(UNIT_TEST_USER)
                    .build();
    new LoadTestPanel<>(loadTestModel(loadTest)).run();
  }
}
