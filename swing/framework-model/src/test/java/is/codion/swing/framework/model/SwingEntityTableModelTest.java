/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.model.DefaultEntityTableConditionModel;
import is.codion.framework.model.DefaultFilterModelFactory;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityTableModelTest extends AbstractEntityTableModelTest<SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityTableModel createTestTableModel() {
    SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_DETAIL, getConnectionProvider()) {
      @Override
      protected Collection<Entity> refreshItems() {
        return testEntities;
      }
    };

    return tableModel;
  }

  @Override
  protected SwingEntityTableModel createMasterTableModel() {
    return new SwingEntityTableModel(TestDomain.T_MASTER, getConnectionProvider());
  }

  @Override
  protected SwingEntityTableModel createDetailTableModel() {
    return new SwingEntityTableModel(createDetailEditModel());
  }

  @Override
  protected SwingEntityTableModel createDepartmentTableModel() {
    SwingEntityTableModel deptModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, testModel.connectionProvider());
    deptModel.sortModel().setSortOrder(TestDomain.DEPARTMENT_NAME, SortOrder.ASCENDING);

    return deptModel;
  }

  @Override
  protected SwingEntityTableModel createEmployeeTableModel() {
    return new SwingEntityTableModel(TestDomain.T_EMP, testModel.connectionProvider());
  }

  @Override
  protected SwingEntityEditModel createDepartmentEditModel() {
    return new SwingEntityEditModel(TestDomain.T_MASTER, getConnectionProvider());
  }

  @Override
  protected SwingEntityEditModel createDetailEditModel() {
    return new SwingEntityEditModel(TestDomain.T_DETAIL, getConnectionProvider());
  }

  @Test
  void refreshOnForeignKeyConditionValuesSet() throws DatabaseException {
    SwingEntityTableModel employeeTableModel = createEmployeeTableModel();
    assertEquals(0, employeeTableModel.getRowCount());
    Entity accounting = getConnectionProvider().connection().selectSingle(TestDomain.DEPARTMENT_ID, 10);
    employeeTableModel.setForeignKeyConditionValues(TestDomain.EMP_DEPARTMENT_FK, singletonList(accounting));
    assertEquals(7, employeeTableModel.getRowCount());
    employeeTableModel.clear();
    employeeTableModel.setRefreshOnForeignKeyConditionValuesSet(false);
    Entity sales = getConnectionProvider().connection().selectSingle(TestDomain.DEPARTMENT_ID, 30);
    employeeTableModel.setForeignKeyConditionValues(TestDomain.EMP_DEPARTMENT_FK, Collections.singleton(sales));
    assertEquals(0, employeeTableModel.getRowCount());
    employeeTableModel.refresh();
    assertEquals(4, employeeTableModel.getRowCount());
  }

  @Test
  void nonMatchingConditionModelEntityType() {
    EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_DEPARTMENT, getConnectionProvider(),
            new DefaultFilterModelFactory(), new DefaultConditionModelFactory(getConnectionProvider()));
    assertThrows(IllegalArgumentException.class, () ->
            new SwingEntityTableModel(new SwingEntityEditModel(TestDomain.T_EMP, getConnectionProvider()), conditionModel));
  }

  @Test
  void nullConditionModel() {
    assertThrows(NullPointerException.class, () -> new SwingEntityTableModel(TestDomain.T_EMP, null));
  }

  @Test
  void testFiltering() {
    testModel.refresh();
    ColumnConditionModel<Attribute<String>, String> filterModel =
            testModel.tableConditionModel().filterModel(TestDomain.DETAIL_STRING);
    filterModel.setEqualValue("a");
    testModel.filterContents();
    assertEquals(4, testModel.filteredItems().size());
  }

  @Test
  void getColumnIndex() {
    assertEquals(0, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_INT));
    assertEquals(1, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_DOUBLE));
    assertEquals(2, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_STRING));
    assertEquals(3, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_DATE));
    assertEquals(4, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_TIMESTAMP));
    assertEquals(5, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_BOOLEAN));
    assertEquals(6, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_BOOLEAN_NULLABLE));
    assertEquals(7, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_MASTER_FK));
    assertEquals(8, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_MASTER_NAME));
    assertEquals(9, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_MASTER_CODE));
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
    assertFalse(testModel.isCellEditable(0, testModel.columnModel().getColumnIndex(TestDomain.DETAIL_INT_DERIVED)));
    testModel.setEditable(false);
  }

  @Test
  void setValueAt() {
    SwingEntityTableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();
    assertThrows(IllegalStateException.class, () -> tableModel.setValueAt("newname", 0, 1));
    tableModel.setEditable(true);
    tableModel.setValueAt("newname", 0, 1);
    Entity entity = tableModel.getItemAt(0);
    assertEquals("newname", entity.get(TestDomain.EMP_NAME));
    assertThrows(RuntimeException.class, () -> tableModel.setValueAt("newname", 0, 0));
  }

  @Test
  void columnModel() {
    TableColumn column = testModel.columnModel().getTableColumn(TestDomain.DETAIL_STRING);
    assertEquals(TestDomain.DETAIL_STRING, column.getIdentifier());
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
            employeeTableModel.tableConditionModel().conditionModel(TestDomain.EMP_NAME);
    nameConditionModel.setEqualValue("BLAKE");
    employeeTableModel.refresh();
    assertEquals(Color.GREEN, employeeTableModel.backgroundColor(0, TestDomain.EMP_JOB));
  }

  @Test
  void indexOf() {
    SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_EMP, testModel.connectionProvider());
    tableModel.refresh();
    tableModel.sortModel().setSortOrder(TestDomain.EMP_NAME, SortOrder.ASCENDING);
    assertEquals(SortOrder.ASCENDING, tableModel.sortModel().getSortingState(TestDomain.EMP_NAME).sortOrder());

    Key pk1 = getConnectionProvider().entities().primaryKey(TestDomain.T_EMP, 10);//ADAMS
    assertEquals(0, tableModel.indexOf(pk1));

    Key pk2 = getConnectionProvider().entities().primaryKey(TestDomain.T_EMP, -66);
    assertEquals(-1, tableModel.indexOf(pk2));
  }

  @Test
  void preferences() throws Exception {
    testModel.clearPreferences();

    SwingEntityTableModel tableModel = createTestTableModel();
    assertTrue(tableModel.columnModel().isColumnVisible(TestDomain.DETAIL_STRING));

    tableModel.columnModel().setColumnVisible(TestDomain.DETAIL_STRING, false);
    tableModel.columnModel().moveColumn(1, 0);//double to 0, int to 1
    TableColumn column = tableModel.columnModel().getColumn(3);
    column.setWidth(150);//timestamp
    column = tableModel.columnModel().getColumn(5);
    column.setWidth(170);//entity_ref

    tableModel.savePreferences();

    SwingEntityTableModel model = createTestTableModel();
    assertFalse(model.columnModel().isColumnVisible(TestDomain.DETAIL_STRING));
    assertEquals(0, model.columnModel().getColumnIndex(TestDomain.DETAIL_DOUBLE));
    assertEquals(1, model.columnModel().getColumnIndex(TestDomain.DETAIL_INT));
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
    OrderBy orderBy = tableModel.getOrderBy();
    //default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.orderByAttributes().get(0).getAttribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(1).getAttribute());

    tableModel.sortModel().setSortOrder(TestDomain.EMP_NAME, SortOrder.ASCENDING);
    orderBy = tableModel.getOrderBy();
    //still default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.orderByAttributes().get(0).getAttribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(1).getAttribute());

    tableModel.setOrderQueryBySortOrder(true);
    orderBy = tableModel.getOrderBy();
    assertEquals(1, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(0).getAttribute());

    tableModel.sortModel().setSortOrder(TestDomain.EMP_HIREDATE, SortOrder.DESCENDING);
    tableModel.sortModel().addSortOrder(TestDomain.EMP_NAME, SortOrder.ASCENDING);

    orderBy = tableModel.getOrderBy();
    assertEquals(2, orderBy.orderByAttributes().size());
    assertFalse(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_HIREDATE, orderBy.orderByAttributes().get(0).getAttribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(1).getAttribute());

    tableModel.sortModel().clear();
    orderBy = tableModel.getOrderBy();
    //back to default order by for entity
    assertEquals(2, orderBy.orderByAttributes().size());
    assertTrue(orderBy.orderByAttributes().get(0).isAscending());
    assertEquals(TestDomain.EMP_DEPARTMENT, orderBy.orderByAttributes().get(0).getAttribute());
    assertTrue(orderBy.orderByAttributes().get(1).isAscending());
    assertEquals(TestDomain.EMP_NAME, orderBy.orderByAttributes().get(1).getAttribute());
  }
}
