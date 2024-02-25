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
package is.codion.framework.demos.manual.store.test;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.model.StoreApplicationModel;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import java.util.function.Function;

import static java.util.Collections.singletonList;

// tag::storeLoadTest[]
public class StoreLoadTest {

  private static final class StoreApplicationModelFactory
          implements Function<User, StoreApplicationModel> {

    @Override
    public StoreApplicationModel apply(User user) {
      EntityConnectionProvider connectionProvider =
              RemoteEntityConnectionProvider.builder()
                      .user(user)
                      .domainType(Store.DOMAIN)
                      .build();

      return new StoreApplicationModel(connectionProvider);
    }
  }

  private static class UsageScenario extends
          AbstractEntityUsageScenario<StoreApplicationModel> {

    @Override
    protected void perform(StoreApplicationModel application) {
      SwingEntityModel customerModel = application.entityModel(Customer.TYPE);
      customerModel.tableModel().refresh();
      selectRandomRow(customerModel.tableModel());
    }
  }

  public static void main(String[] args) {
    LoadTest<StoreApplicationModel> loadTest =
            LoadTest.builder(new StoreApplicationModelFactory(),
                            application -> application.connectionProvider().close())
                    .user(User.parse("scott:tiger"))
                    .usageScenarios(singletonList(new UsageScenario()))
                    .titleFactory(model -> "Store LoadTest - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
                    .build();
    new LoadTestPanel<>(LoadTestModel.loadTestModel(loadTest)).run();
  }
}
// end::storeLoadTest[]