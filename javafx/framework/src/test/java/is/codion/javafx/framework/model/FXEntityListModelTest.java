/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.framework.model.test.TestDomain.Master;
import is.codion.javafx.framework.ui.EntityTableColumn;
import is.codion.javafx.framework.ui.EntityTableView;

import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class FXEntityListModelTest extends AbstractEntityTableModelTest<FXEntityEditModel, FXEntityListModel> {

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Override
  protected FXEntityListModel createTestTableModel() {
    FXEntityListModel listModel = new FXEntityListModel(Detail.TYPE, connectionProvider()) {
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
    return new FXEntityListModel(Master.TYPE, connectionProvider());
  }

  @Override
  protected FXEntityListModel createDetailTableModel() {
    return new FXEntityListModel(createDetailEditModel());
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    FXEntityListModel deptModel = new FXEntityListModel(Department.TYPE, testModel.connectionProvider());
    EntityTableView tableView = new EntityTableView(deptModel);
    tableView.getSortOrder().add(deptModel.tableColumn(Department.NAME));

    return deptModel;
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    FXEntityListModel empModel = new FXEntityListModel(Employee.TYPE, testModel.connectionProvider());
    new EntityTableView(empModel);

    return empModel;
  }

  @Override
  protected FXEntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(Master.TYPE, connectionProvider());
  }

  @Override
  protected FXEntityEditModel createDetailEditModel() {
    return new FXEntityEditModel(Detail.TYPE, connectionProvider());
  }

  @Test
  void orderQueryBySortOrder() {
    FXEntityListModel tableModel = createEmployeeTableModel();
    OrderBy orderBy = tableModel.orderBy();
    //default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(Employee.DEPARTMENT, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(Employee.NAME, orderBy.orderByAttributes().get(1).attribute());

    ObservableList<TableColumn<Entity, ?>> sortOrder = tableModel.getColumnSortOrder();
    sortOrder.clear();
    EntityTableColumn<?> column = tableModel.tableColumn(Employee.NAME);
    column.setSortType(TableColumn.SortType.ASCENDING);
    sortOrder.add(column);
    orderBy = tableModel.orderBy();
    //still default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(Employee.DEPARTMENT, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(Employee.NAME, orderBy.orderByAttributes().get(1).attribute());

    tableModel.setOrderQueryBySortOrder(true);
    orderBy = tableModel.orderBy();
    assertEquals(1, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(Employee.NAME, orderBy.orderByAttributes().get(0).attribute());

    sortOrder.clear();
    column = tableModel.tableColumn(Employee.HIREDATE);
    column.setSortType(TableColumn.SortType.DESCENDING);
    sortOrder.add(column);
    column = tableModel.tableColumn(Employee.NAME);
    column.setSortType(TableColumn.SortType.ASCENDING);
    sortOrder.add(column);

    orderBy = tableModel.orderBy();
    assertEquals(2, orderBy.orderByAttributes().size());
    assertFalse(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(Employee.HIREDATE, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(Employee.NAME, orderBy.orderByAttributes().get(1).attribute());

    sortOrder.clear();
    orderBy = tableModel.orderBy();
    //back to default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(Employee.DEPARTMENT, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(Employee.NAME, orderBy.orderByAttributes().get(1).attribute());
  }

  @Test
  void editEvents() throws DatabaseException, ValidationException {
    EntityConnectionProvider connectionProvider = connectionProvider();
    Entity researchDept = connectionProvider.connection().select(connectionProvider.entities().primaryKey(Department.TYPE, 20));

    FXEntityListModel tableModel = createEmployeeTableModel();
    assertTrue(tableModel.isListenToEditEvents());
    tableModel.conditionModel().attributeModel(Employee.DEPARTMENT_FK).setEqualValue(researchDept);
    tableModel.refresh();

    tableModel.items().forEach(emp ->
            assertEquals("RESEARCH", emp.get(Employee.DEPARTMENT_FK).get(Department.NAME)));

    FXEntityEditModel editModel = new FXEntityEditModel(Department.TYPE, connectionProvider);
    editModel.setEntity(researchDept);
    editModel.put(Department.NAME, "R&D");
    editModel.update();

    assertTrue(tableModel.getRowCount() > 0);

    tableModel.items().forEach(emp ->
            assertEquals("R&D", emp.get(Employee.DEPARTMENT_FK).get(Department.NAME)));

    tableModel.setListenToEditEvents(false);

    editModel.put(Department.NAME, "RESEARCH");
    editModel.update();

    tableModel.items().forEach(emp ->
            assertEquals("R&D", emp.get(Employee.DEPARTMENT_FK).get(Department.NAME)));
  }

  @Test
  void validItems() {
    FXEntityListModel tableModel = createEmployeeTableModel();
    Entity dept = tableModel.entities().builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "dept")
            .build();
    assertThrows(IllegalArgumentException.class, () -> tableModel.add(dept));
    assertThrows(IllegalArgumentException.class, () -> tableModel.addAll(0, singletonList(dept)));

    assertThrows(NullPointerException.class, () -> tableModel.add(null));
    assertThrows(NullPointerException.class, () -> tableModel.addAll(0, singletonList(null)));
  }
}
