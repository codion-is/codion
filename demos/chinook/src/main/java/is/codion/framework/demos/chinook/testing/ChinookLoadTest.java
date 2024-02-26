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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
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
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;

import java.util.Collection;
import java.util.function.Function;

import static java.util.Arrays.asList;

public final class ChinookLoadTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Collection<Scenario<EntityConnectionProvider>> SCENARIOS =
          asList(Scenario.builder(new ViewGenre())
                          .defaultWeight(10)
                          .build(),
                  Scenario.builder(new ViewCustomerReport())
                          .defaultWeight(2)
                          .build(),
                  Scenario.builder(new ViewInvoice())
                          .defaultWeight(10)
                          .build(),
                  Scenario.builder(new ViewAlbum())
                          .defaultWeight(10)
                          .build(),
                  Scenario.builder(new UpdateTotals())
                          .defaultWeight(1)
                          .build(),
                  Scenario.builder(new InsertDeleteAlbum())
                          .defaultWeight(3)
                          .build(),
                  Scenario.builder(new LogoutLogin())
                          .defaultWeight(1)
                          .build(),
                  Scenario.builder(new RaisePrices())
                          .defaultWeight(1)
                          .build(),
                  Scenario.builder(new RandomPlaylist())
                          .defaultWeight(1)
                          .build(),
                  Scenario.builder(new InsertDeleteInvoice())
                          .defaultWeight(3)
                          .build());

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
