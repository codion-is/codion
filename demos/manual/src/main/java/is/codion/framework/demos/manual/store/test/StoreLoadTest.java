/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.test;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.model.StoreAppModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.UUID;

import static java.util.Collections.singletonList;

// tag::storeLoadTest[]
public class StoreLoadTest extends EntityLoadTestModel<StoreAppModel> {

  public StoreLoadTest(User user) {
    super(user, singletonList(new UsageScenario()));
  }

  @Override
  protected StoreAppModel createApplication() {
    EntityConnectionProvider connectionProvider =
            RemoteEntityConnectionProvider.builder()
                    .clientId(UUID.randomUUID())
                    .user(getUser())
                    .domainClassName(Store.class.getName())
                    .build();

    return new StoreAppModel(connectionProvider);
  }

  private static class UsageScenario extends
          AbstractEntityUsageScenario<StoreAppModel> {

    @Override
    protected void perform(StoreAppModel application)
            throws Exception {
      SwingEntityModel customerModel = application.entityModel(Customer.TYPE);
      customerModel.tableModel().refresh();
      selectRandomRow(customerModel.tableModel());
    }
  }
}
// end::storeLoadTest[]