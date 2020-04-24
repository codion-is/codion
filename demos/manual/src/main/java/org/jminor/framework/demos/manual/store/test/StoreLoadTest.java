/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.test;

import org.jminor.common.user.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.rmi.RemoteEntityConnectionProvider;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.framework.demos.manual.store.model.StoreAppModel;
import org.jminor.framework.model.EntityModel;
import org.jminor.swing.framework.tools.loadtest.EntityLoadTestModel;

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
        EntityModel customerModel =
                application.getEntityModel(Store.T_CUSTOMER);
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