/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.test;

import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.Scenario;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.model.StoreApplicationModel;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityPerformer;

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

  private static class StoreScenarioPerformer extends
          AbstractEntityPerformer<StoreApplicationModel> {

    @Override
    public void perform(StoreApplicationModel application) {
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
                    .scenarios(singletonList(Scenario.builder(new StoreScenarioPerformer()).build()))
                    .titleFactory(model -> "Store LoadTest - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
                    .build();
    new LoadTestPanel<>(LoadTestModel.loadTestModel(loadTest)).run();
  }
}
// end::storeLoadTest[]