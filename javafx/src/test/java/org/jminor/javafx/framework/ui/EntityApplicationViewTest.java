/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.javafx.framework.model.FXEntityApplicationModel;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.model.FXEntityListModel;
import org.jminor.javafx.framework.model.FXEntityModel;

import javafx.embed.swing.JFXPanel;
import org.junit.Test;

public final class EntityApplicationViewTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.createInstance());

  static {
    new JFXPanel();
  }

  @Test
  public void constructor() {
    final EntityApplicationView<FXEntityApplicationModel> applicationView = new EntityApplicationView<FXEntityApplicationModel>("EntityApplicationViewTest") {
      @Override
      protected void initializeEntityViews() {
        final FXEntityEditModel editModel = new FXEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
        final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
        final FXEntityModel model = new FXEntityModel(editModel, listModel);

        addEntityView(new EntityView(model, new EntityEditViewTest.EmpEditView(editModel), new EntityTableView(listModel)));
      }

      @Override
      protected FXEntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
        return new FXEntityApplicationModel(CONNECTION_PROVIDER) {
          @Override
          protected void loadDomainModel() throws ClassNotFoundException {
            TestDomain.init();
          }
        };
      }
    };
    applicationView.initializeApplicationModel(CONNECTION_PROVIDER);
    applicationView.initializeEntityViews();
  }
}
