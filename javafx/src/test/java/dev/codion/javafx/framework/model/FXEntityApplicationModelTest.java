/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.javafx.framework.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.model.tests.AbstractEntityApplicationModelTest;
import dev.codion.framework.model.tests.TestDomain;
import dev.codion.javafx.framework.ui.EntityTableView;

import org.junit.jupiter.api.BeforeAll;
import org.testfx.api.FxToolkit;

public final class FXEntityApplicationModelTest extends AbstractEntityApplicationModelTest<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Override
  protected FXEntityModel createDepartmentModel() {
    final FXEntityModel model = new DeptModel(getConnectionProvider());
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
