/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.test;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.model.StoreAppModel;
import is.codion.framework.model.EntityModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.UUID;

import static java.util.Collections.singletonList;

// tag::storeLoadTest[]
public class StoreLoadTest extends EntityLoadTestModel<StoreAppModel> {

  public StoreLoadTest(User user) {
    super(user, singletonList(new UsageScenario()));
  }

  @Override
  protected StoreAppModel initializeApplication() {
    EntityConnectionProvider connectionProvider =
            new RemoteEntityConnectionProvider()
                    .setClientId(UUID.randomUUID())
                    .setUser(getUser())
                    .setDomainClassName(Store.class.getName());

    return new StoreAppModel(connectionProvider);
  }

  private static class UsageScenario extends
          EntityLoadTestModel.AbstractEntityUsageScenario<StoreAppModel> {

    @Override
    protected void perform(StoreAppModel application)
            throws ScenarioException {
      try {
        EntityModel customerModel = application.getEntityModel(Store.T_CUSTOMER);
        customerModel.refresh();
        selectRandomRow(customerModel.getTableModel());
      }
      catch (Exception e) {
        throw new ScenarioException(e);
      }
    }
  }
}
// end::storeLoadTest[]