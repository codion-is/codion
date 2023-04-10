/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.test;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.model.StoreApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.UUID;

import static java.util.Collections.singletonList;

// tag::storeLoadTest[]
public class StoreLoadTest extends EntityLoadTestModel<StoreApplicationModel> {

  public StoreLoadTest(User user) {
    super(user, singletonList(new UsageScenario()));
  }

  @Override
  protected StoreApplicationModel createApplication() {
    EntityConnectionProvider connectionProvider =
            RemoteEntityConnectionProvider.builder()
                    .clientId(UUID.randomUUID())
                    .user(getUser())
                    .domainClassName(Store.class.getName())
                    .build();

    return new StoreApplicationModel(connectionProvider);
  }

  private static class UsageScenario extends
          AbstractEntityUsageScenario<StoreApplicationModel> {

    @Override
    protected void perform(StoreApplicationModel application)
            throws Exception {
      SwingEntityModel customerModel = application.entityModel(Customer.TYPE);
      customerModel.tableModel().refresh();
      selectRandomRow(customerModel.tableModel());
    }
  }
}
// end::storeLoadTest[]