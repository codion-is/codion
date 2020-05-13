/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.test;

import dev.codion.common.user.User;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import dev.codion.framework.demos.manual.store.domain.Store;
import dev.codion.framework.demos.manual.store.model.StoreAppModel;
import dev.codion.framework.model.EntityModel;
import dev.codion.swing.common.tools.loadtest.ScenarioException;
import dev.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.Collections;
import java.util.UUID;

// tag::storeLoadTest[]
public class StoreLoadTest extends EntityLoadTestModel<StoreAppModel> {

  public StoreLoadTest(User user) {
    super(user, Collections.singletonList(new Scenario()));
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

  private static class Scenario extends
          EntityLoadTestModel.AbstractEntityUsageScenario<StoreAppModel> {

    @Override
    protected void performScenario(StoreAppModel application)
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