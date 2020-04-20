/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.model.tests.AbstractEntityTableModelTest;
import org.jminor.framework.model.tests.TestDomain;
import org.jminor.javafx.framework.ui.EntityTableView;

import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeAll;
import org.testfx.api.FxToolkit;

import java.util.List;

public final class FXEntityListModelTest extends AbstractEntityTableModelTest<FXEntityEditModel, FXEntityListModel> {

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Override
  protected FXEntityListModel createTestTableModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_DETAIL, getConnectionProvider()) {
      @Override
      protected List<Entity> performQuery() {
        return testEntities;
      }
    };
    listModel.setEditModel(new FXEntityEditModel(TestDomain.T_DETAIL, getConnectionProvider()));
    final ListView<Entity> listView = new ListView<>(listModel);
    listModel.setSelectionModel(listView.getSelectionModel());

    return listModel;
  }

  @Override
  protected FXEntityListModel createMasterTableModel() {
    return new FXEntityListModel(TestDomain.T_MASTER, getConnectionProvider());
  }

  @Override
  protected FXEntityListModel createDetailTableModel() {
    return new FXEntityListModel(TestDomain.T_DETAIL, getConnectionProvider());
  }

  @Override
  protected FXEntityListModel createEmployeeTableModelWithoutEditModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, getConnectionProvider());
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    final FXEntityListModel deptModel = new FXEntityListModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider());
    deptModel.setEditModel(new FXEntityEditModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider()));
    final EntityTableView tableView = new EntityTableView(deptModel);
    tableView.getSortOrder().add(deptModel.getTableColumn(TestDomain.DEPARTMENT_NAME));

    return deptModel;
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    final FXEntityListModel empModel = new FXEntityListModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    empModel.setEditModel(new FXEntityEditModel(TestDomain.T_EMP, testModel.getConnectionProvider()));
    new EntityTableView(empModel);

    return empModel;
  }

  @Override
  protected FXEntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(TestDomain.T_MASTER, getConnectionProvider());
  }

  @Override
  protected FXEntityEditModel createDetailEditModel() {
    return new FXEntityEditModel(TestDomain.T_DETAIL, getConnectionProvider());
  }
}
