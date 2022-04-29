/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_DETAIL, getConnectionProvider()) {
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
    return new FXEntityListModel(TestDomain.T_MASTER, getConnectionProvider());
  }

  @Override
  protected FXEntityListModel createDetailTableModel() {
    return new FXEntityListModel(createDetailEditModel());
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    FXEntityListModel deptModel = new FXEntityListModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider());
    EntityTableView tableView = new EntityTableView(deptModel);
    tableView.getSortOrder().add(deptModel.getTableColumn(TestDomain.DEPARTMENT_NAME));

    return deptModel;
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    FXEntityListModel empModel = new FXEntityListModel(TestDomain.T_EMP, testModel.getConnectionProvider());
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

  @Test
  void orderQueryBySortOrder() {
    FXEntityListModel tableModel = createEmployeeTableModel();
    OrderBy orderBy = tableModel.getOrderBy();
    //default order by for entity
    assertEquals(2, orderBy.getOrderByAttributes().size());
    assertTrue(orderBy.getOrderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.getOrderByAttributes().get(0).getAttribute());
    assertTrue(orderBy.getOrderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.getOrderByAttributes().get(1).getAttribute());

    ObservableList<TableColumn<Entity, ?>> sortOrder = tableModel.getColumnSortOrder();
    sortOrder.clear();
    EntityTableColumn<?> column = tableModel.getTableColumn(TestDomain.EMP_NAME);
    column.setSortType(TableColumn.SortType.ASCENDING);
    sortOrder.add((TableColumn<Entity, ?>) column);
    orderBy = tableModel.getOrderBy();
    //still default order by for entity
    assertEquals(2, orderBy.getOrderByAttributes().size());
    assertTrue(orderBy.getOrderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.getOrderByAttributes().get(0).getAttribute());
    assertTrue(orderBy.getOrderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.getOrderByAttributes().get(1).getAttribute());

    tableModel.setOrderQueryBySortOrder(true);
    orderBy = tableModel.getOrderBy();
    assertEquals(1, orderBy.getOrderByAttributes().size());
    assertTrue(orderBy.getOrderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.getOrderByAttributes().get(0).getAttribute());

    sortOrder.clear();
    column = tableModel.getTableColumn(TestDomain.EMP_HIREDATE);
    column.setSortType(TableColumn.SortType.DESCENDING);
    sortOrder.add(column);
    column = tableModel.getTableColumn(TestDomain.EMP_NAME);
    column.setSortType(TableColumn.SortType.ASCENDING);
    sortOrder.add(column);

    orderBy = tableModel.getOrderBy();
    assertEquals(2, orderBy.getOrderByAttributes().size());
    assertFalse(orderBy.getOrderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_HIREDATE, orderBy.getOrderByAttributes().get(0).getAttribute());
    assertTrue(orderBy.getOrderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.getOrderByAttributes().get(1).getAttribute());

    sortOrder.clear();
    orderBy = tableModel.getOrderBy();
    //back to default order by for entity
    assertEquals(2, orderBy.getOrderByAttributes().size());
    assertTrue(orderBy.getOrderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.getOrderByAttributes().get(0).getAttribute());
    assertTrue(orderBy.getOrderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.getOrderByAttributes().get(1).getAttribute());
  }
}
