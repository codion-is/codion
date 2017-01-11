/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.AbstractEntityApplicationModelTest;
import org.jminor.javafx.framework.ui.EntityTableView;

import javafx.embed.swing.JFXPanel;

public final class FXEntityApplicationModelTest extends AbstractEntityApplicationModelTest<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  static {
    new JFXPanel();
  }

  @Override
  protected FXEntityModel createDepartmentModel() {
    final FXEntityModel model = new DeptModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityTableView(model.getTableModel());
    new EntityTableView(model.getDetailModel(TestDomain.T_EMP).getTableModel());

    return model;
  }

  private static class DeptModel extends FXEntityModel {
    private DeptModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_DEPARTMENT, connectionProvider);
      addDetailModel(new FXEntityModel(TestDomain.T_EMP, connectionProvider));
    }
  }
}
