/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.model.DefaultConditionModelFactory;
import is.codion.framework.model.DefaultFilterModelFactory;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.framework.model.test.TestDomain.Master;
import is.codion.swing.common.model.component.table.FilteredTableColumn;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityTableModelTest extends AbstractEntityTableModelTest<SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityTableModel createTestTableModel() {
    SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, connectionProvider()) {
      @Override
      protected Collection<Entity> refreshItems() {
        return testEntities;
      }
    };

    return tableModel;
  }

  @Override
  protected SwingEntityTableModel createMasterTableModel() {
    return new SwingEntityTableModel(Master.TYPE, connectionProvider());
  }

  @Override
  protected SwingEntityTableModel createDetailTableModel() {
    return new SwingEntityTableModel(createDetailEditModel());
  }

  @Override
  protected SwingEntityTableModel createDepartmentTableModel() {
    SwingEntityTableModel deptModel = new SwingEntityTableModel(Department.TYPE, testModel.connectionProvider());
    deptModel.sortModel().setSortOrder(Department.NAME, SortOrder.ASCENDING);

    return deptModel;
  }

  @Override
  protected SwingEntityTableModel createEmployeeTableModel() {
    return new SwingEntityTableModel(Employee.TYPE, testModel.connectionProvider());
  }

  @Override
  protected SwingEntityEditModel createDepartmentEditModel() {
    return new SwingEntityEditModel(Master.TYPE, connectionProvider());
  }

  @Override
  protected SwingEntityEditModel createDetailEditModel() {
    return new SwingEntityEditModel(Detail.TYPE, connectionProvider());
  }

  @Test
  void refreshOnForeignKeyConditionValuesSet() throws DatabaseException {
    SwingEntityTableModel employeeTableModel = createEmployeeTableModel();
    assertEquals(0, employeeTableModel.getRowCount());
    Entity accounting = connectionProvider().connection().selectSingle(Department.ID, 10);
    employeeTableModel.setForeignKeyConditionValues(Employee.DEPARTMENT_FK, singletonList(accounting));
    employeeTableModel.refresh();
    assertEquals(7, employeeTableModel.getRowCount());
  }

  @Test
  void nonMatchingConditionModelEntityType() {
    EntityTableConditionModel conditionModel = entityTableConditionModel(Department.TYPE, connectionProvider(),
            new DefaultFilterModelFactory(), new DefaultConditionModelFactory(connectionProvider()));
    assertThrows(IllegalArgumentException.class, () ->
            new SwingEntityTableModel(new SwingEntityEditModel(Employee.TYPE, connectionProvider()), conditionModel));
  }

  @Test
  void nullConditionModel() {
    assertThrows(NullPointerException.class, () -> new SwingEntityTableModel(Employee.TYPE, null));
  }

  @Test
  void testFiltering() {
    testModel.refresh();
    ColumnConditionModel<Attribute<String>, String> filterModel =
            testModel.tableConditionModel().filterModel(Detail.STRING);
    filterModel.setEqualValue("a");
    testModel.filterItems();
    assertEquals(4, testModel.filteredItems().size());
  }

  @Test
  void getColumnIndex() {
    assertEquals(0, testModel.columnModel().getColumnIndex(Detail.INT));
    assertEquals(1, testModel.columnModel().getColumnIndex(Detail.DOUBLE));
    assertEquals(2, testModel.columnModel().getColumnIndex(Detail.STRING));
    assertEquals(3, testModel.columnModel().getColumnIndex(Detail.DATE));
    assertEquals(4, testModel.columnModel().getColumnIndex(Detail.TIMESTAMP));
    assertEquals(5, testModel.columnModel().getColumnIndex(Detail.BOOLEAN));
    assertEquals(6, testModel.columnModel().getColumnIndex(Detail.BOOLEAN_NULLABLE));
    assertEquals(7, testModel.columnModel().getColumnIndex(Detail.MASTER_FK));
    assertEquals(8, testModel.columnModel().getColumnIndex(Detail.MASTER_NAME));
    assertEquals(9, testModel.columnModel().getColumnIndex(Detail.MASTER_CODE));
  }

  @Test
  void getValueAt() {
    testModel.refresh();
    assertEquals(1, testModel.getValueAt(0, 0));
    assertEquals(2, testModel.getValueAt(1, 0));
    assertEquals(3, testModel.getValueAt(2, 0));
    assertEquals(4, testModel.getValueAt(3, 0));
    assertEquals(5, testModel.getValueAt(4, 0));
    assertEquals("a", testModel.getValueAt(0, 2));
    assertEquals("b", testModel.getValueAt(1, 2));
    assertEquals("c", testModel.getValueAt(2, 2));
    assertEquals("d", testModel.getValueAt(3, 2));
    assertEquals("e", testModel.getValueAt(4, 2));
  }

  @Test
  void isEditable() {
    testModel.setEditable(true);
    assertTrue(testModel.isCellEditable(0, 0));
    assertFalse(testModel.isCellEditable(0, testModel.columnModel().getColumnIndex(Detail.INT_DERIVED)));
    testModel.setEditable(false);
  }

  @Test
  void setValueAt() {
    SwingEntityTableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();
    assertThrows(IllegalStateException.class, () -> tableModel.setValueAt("newname", 0, 1));
    tableModel.setEditable(true);
    tableModel.setValueAt("newname", 0, 1);
    Entity entity = tableModel.itemAt(0);
    assertEquals("newname", entity.get(Employee.NAME));
    assertThrows(RuntimeException.class, () -> tableModel.setValueAt("newname", 0, 0));
  }

  @Test
  void columnModel() {
    FilteredTableColumn<Attribute<?>> column = testModel.columnModel().tableColumn(Detail.STRING);
    assertEquals(Detail.STRING, column.getIdentifier());
  }

  @Test
  void getColumnClass() {
    assertEquals(Integer.class, testModel.getColumnClass(0));
    assertEquals(Double.class, testModel.getColumnClass(1));
    assertEquals(String.class, testModel.getColumnClass(2));
    assertEquals(LocalDate.class, testModel.getColumnClass(3));
    assertEquals(LocalDateTime.class, testModel.getColumnClass(4));
    assertEquals(Boolean.class, testModel.getColumnClass(5));
    assertEquals(Boolean.class, testModel.getColumnClass(6));
    assertEquals(Entity.class, testModel.getColumnClass(7));
  }

  @Test
  void backgroundColor() {
    SwingEntityTableModel employeeTableModel = createEmployeeTableModel();
    ColumnConditionModel<Attribute<String>, String> nameConditionModel =
            employeeTableModel.tableConditionModel().conditionModel(Employee.NAME);
    nameConditionModel.setEqualValue("BLAKE");
    employeeTableModel.refresh();
    assertEquals(Color.GREEN, employeeTableModel.backgroundColor(0, Employee.JOB));
  }

  @Test
  void indexOf() {
    SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, testModel.connectionProvider());
    tableModel.refresh();
    tableModel.sortModel().setSortOrder(Employee.NAME, SortOrder.ASCENDING);
    assertEquals(SortOrder.ASCENDING, tableModel.sortModel().sortingState(Employee.NAME).sortOrder());

    Key pk1 = connectionProvider().entities().primaryKey(Employee.TYPE, 10);//ADAMS
    assertEquals(0, tableModel.indexOf(pk1));

    Key pk2 = connectionProvider().entities().primaryKey(Employee.TYPE, -66);
    assertEquals(-1, tableModel.indexOf(pk2));
  }

  @Test
  void preferences() throws Exception {
    testModel.clearPreferences();

    SwingEntityTableModel tableModel = createTestTableModel();
    assertTrue(tableModel.columnModel().isColumnVisible(Detail.STRING));

    tableModel.columnModel().setColumnVisible(Detail.STRING, false);
    tableModel.columnModel().moveColumn(1, 0);//double to 0, int to 1
    TableColumn column = tableModel.columnModel().getColumn(3);
    column.setWidth(150);//timestamp
    column = tableModel.columnModel().getColumn(5);
    column.setWidth(170);//entity_ref

    tableModel.savePreferences();

    SwingEntityTableModel model = createTestTableModel();
    assertFalse(model.columnModel().isColumnVisible(Detail.STRING));
    assertEquals(0, model.columnModel().getColumnIndex(Detail.DOUBLE));
    assertEquals(1, model.columnModel().getColumnIndex(Detail.INT));
    column = model.columnModel().getColumn(3);
    assertEquals(150, column.getPreferredWidth());
    column = model.columnModel().getColumn(5);
    assertEquals(170, column.getPreferredWidth());

    model.clearPreferences();
    UserPreferences.flushUserPreferences();
  }

  @Test
  void orderQueryBySortOrder() {
    SwingEntityTableModel tableModel = createEmployeeTableModel();
    OrderBy orderBy = tableModel.orderBy();
    //default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(Employee.DEPARTMENT, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(Employee.NAME, orderBy.orderByAttributes().get(1).attribute());

    tableModel.sortModel().setSortOrder(Employee.NAME, SortOrder.ASCENDING);
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

    tableModel.sortModel().setSortOrder(Employee.HIREDATE, SortOrder.DESCENDING);
    tableModel.sortModel().addSortOrder(Employee.NAME, SortOrder.ASCENDING);

    orderBy = tableModel.orderBy();
    assertEquals(2, orderBy.orderByAttributes().size());
    assertFalse(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(Employee.HIREDATE, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(Employee.NAME, orderBy.orderByAttributes().get(1).attribute());

    tableModel.sortModel().clear();
    orderBy = tableModel.orderBy();
    //back to default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(Employee.DEPARTMENT, orderBy.orderByAttributes().get(0).attribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(Employee.NAME, orderBy.orderByAttributes().get(1).attribute());
  }
}
