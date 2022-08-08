/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain;
import is.codion.javafx.framework.ui.EntityTableColumn;
import is.codion.javafx.framework.ui.EntityTableView;

import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class FXEntityListModelTest extends AbstractEntityTableModelTest<FXEntityEditModel, FXEntityListModel> {

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Override
  protected FXEntityListModel createTestTableModel() {
    FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_DETAIL, connectionProvider()) {
      @Override
      protected List<Entity> performQuery() {
        return testEntities;
      }
    };
    ListView<Entity> listView = new ListView<>(listModel);
    listModel.setSelectionModel(listView.getSelectionModel());

    return listModel;
  }

  @Override
  protected FXEntityListModel createMasterTableModel() {
    return new FXEntityListModel(TestDomain.T_MASTER, connectionProvider());
  }

  @Override
  protected FXEntityListModel createDetailTableModel() {
    return new FXEntityListModel(createDetailEditModel());
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    FXEntityListModel deptModel = new FXEntityListModel(TestDomain.T_DEPARTMENT, testModel.connectionProvider());
    EntityTableView tableView = new EntityTableView(deptModel);
    tableView.getSortOrder().add(deptModel.tableColumn(TestDomain.DEPARTMENT_NAME));

    return deptModel;
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    FXEntityListModel empModel = new FXEntityListModel(TestDomain.T_EMP, testModel.connectionProvider());
    new EntityTableView(empModel);

    return empModel;
  }

  @Override
  protected FXEntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(TestDomain.T_MASTER, connectionProvider());
  }

  @Override
  protected FXEntityEditModel createDetailEditModel() {
    return new FXEntityEditModel(TestDomain.T_DETAIL, connectionProvider());
  }

  @Test
  void orderQueryBySortOrder() {
    FXEntityListModel tableModel = createEmployeeTableModel();
    OrderBy orderBy = tableModel.orderBy();
    //default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(1).attribute());

    ObservableList<TableColumn<Entity, ?>> sortOrder = tableModel.getColumnSortOrder();
    sortOrder.clear();
    EntityTableColumn<?> column = tableModel.tableColumn(TestDomain.EMP_NAME);
    column.setSortType(TableColumn.SortType.ASCENDING);
    sortOrder.add((TableColumn<Entity, ?>) column);
    orderBy = tableModel.orderBy();
    //still default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(1).attribute());

    tableModel.setOrderQueryBySortOrder(true);
    orderBy = tableModel.orderBy();
    assertEquals(1, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(0).attribute());

    sortOrder.clear();
    column = tableModel.tableColumn(TestDomain.EMP_HIREDATE);
    column.setSortType(TableColumn.SortType.DESCENDING);
    sortOrder.add(column);
    column = tableModel.tableColumn(TestDomain.EMP_NAME);
    column.setSortType(TableColumn.SortType.ASCENDING);
    sortOrder.add(column);

    orderBy = tableModel.orderBy();
    assertEquals(2, orderBy.orderByAttributes().size());
    assertFalse(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_HIREDATE, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(1).attribute());

    sortOrder.clear();
    orderBy = tableModel.orderBy();
    //back to default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(1).attribute());
  }
}
