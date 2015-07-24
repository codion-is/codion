/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.User;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.TestDomain;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class EntityApplicationPanelTest {

  @After
  public void tearDown() {
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  @Test
  public void test() throws Exception {
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, "local");
    final EntityApplicationPanel panel = new EntityApplicationPanel() {
      @Override
      protected List<EntityPanel> initializeEntityPanels(final EntityApplicationModel applicationModel) {
        return Collections.singletonList(new EntityPanel(applicationModel.getEntityModel(TestDomain.T_EMPLOYEE)));
      }
      @Override
      protected EntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
        final EntityApplicationModel model = new DefaultEntityApplicationModel(connectionProvider) {
          @Override
          protected void loadDomainModel() {
            TestDomain.init();
          }
        };

        model.addEntityModel(new DefaultEntityModel(TestDomain.T_EMPLOYEE, connectionProvider));

        return model;
      }
    };
    panel.setLoginRequired(false);
    panel.setShowStartupDialog(false);
    panel.startApplication("Test", null, false, null, null, false, User.UNIT_TEST_USER);
    assertNotNull(panel.getEntityPanel(TestDomain.T_EMPLOYEE));

    panel.logout();
  }
}
