/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.test;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.framework.demos.manual.store.model.StoreAppModel;
import org.jminor.framework.model.EntityModel;
import org.jminor.swing.framework.tools.EntityLoadTestModel;

import java.util.Collections;
import java.util.UUID;

public class StoreLoadTest extends EntityLoadTestModel<StoreAppModel> {

  public StoreLoadTest(final User user) {
    super(user, Collections.singletonList(new Scenario()));
  }

  @Override
  protected StoreAppModel initializeApplication() {
    final EntityConnectionProvider connectionProvider =
            new RemoteEntityConnectionProvider()
                    .setClientId(UUID.randomUUID())
                    .setUser(getUser())
                    .setDomainClassName(Store.class.getName());

    return new StoreAppModel(connectionProvider);
  }

  private static class Scenario extends
          EntityLoadTestModel.AbstractEntityUsageScenario<StoreAppModel> {

    @Override
    protected void performScenario(final StoreAppModel application)
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
