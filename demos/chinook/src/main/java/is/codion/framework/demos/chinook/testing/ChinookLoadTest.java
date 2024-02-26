/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.UsageScenario;
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
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;

import java.util.Collection;
import java.util.function.Function;

import static java.util.Arrays.asList;

public final class ChinookLoadTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Collection<UsageScenario<EntityConnectionProvider>> SCENARIOS =
          asList(new ViewGenre(), new ViewCustomerReport(), new ViewInvoice(), new ViewAlbum(), new UpdateTotals(),
                  new InsertDeleteAlbum(), new LogoutLogin(), new RaisePrices(),new RandomPlaylist(), new InsertDeleteInvoice());

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
    new LoadTestPanel<>(LoadTestModel.loadTestModel(loadTest)).run();
  }
}
