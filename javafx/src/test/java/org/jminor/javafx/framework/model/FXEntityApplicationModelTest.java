/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.AbstractEntityApplicationModelTest;
import org.jminor.framework.model.TestDomain;
import org.jminor.javafx.framework.ui.EntityTableView;

import org.junit.jupiter.api.BeforeAll;
import org.testfx.api.FxToolkit;

public final class FXEntityApplicationModelTest extends AbstractEntityApplicationModelTest<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Override
  protected FXEntityModel createDepartmentModel() {
    final FXEntityModel model = new DeptModel(CONNECTION_PROVIDER);
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
