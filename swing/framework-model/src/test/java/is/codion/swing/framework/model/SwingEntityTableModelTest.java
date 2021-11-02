/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableSortModel.SortingDirective;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.model.DefaultConditionModelFactory;
import is.codion.framework.model.DefaultEntityTableConditionModel;
import is.codion.framework.model.DefaultFilterModelFactory;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityTableModelTest extends AbstractEntityTableModelTest<SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityTableModel createTestTableModel() {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_DETAIL, getConnectionProvider()) {
      @Override
      protected Collection<Entity> refreshItems() {
        return testEntities;
      }
    };
    tableModel.setEditModel(new SwingEntityEditModel(TestDomain.T_DETAIL, getConnectionProvider()));

    return tableModel;
  }

  @Override
  protected SwingEntityTableModel createMasterTableModel() {
    return new SwingEntityTableModel(TestDomain.T_MASTER, getConnectionProvider());
  }

  @Override
  protected SwingEntityTableModel createDetailTableModel() {
    return new SwingEntityTableModel(TestDomain.T_DETAIL, getConnectionProvider());
  }

  @Override
  protected SwingEntityTableModel createEmployeeTableModelWithoutEditModel() {
    return new SwingEntityTableModel(TestDomain.T_EMP, getConnectionProvider());
  }

  @Override
  protected SwingEntityTableModel createDepartmentTableModel() {
    final SwingEntityTableModel deptModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider());
    deptModel.setEditModel(new SwingEntityEditModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider()));
    deptModel.getSortModel().setSortingDirective(TestDomain.DEPARTMENT_NAME, SortingDirective.ASCENDING);

    return deptModel;
  }

  @Override
  protected SwingEntityTableModel createEmployeeTableModel() {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.setEditModel(new SwingEntityEditModel(TestDomain.T_EMP, testModel.getConnectionProvider()));

    return tableModel;
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
    final SwingEntityTableModel employeeTableModel = createEmployeeTableModel();
    assertEquals(0, employeeTableModel.getRowCount());
    final Entity accounting = getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_ID, 10);
    employeeTableModel.setForeignKeyConditionValues(TestDomain.EMP_DEPARTMENT_FK, singletonList(accounting));
    assertEquals(7, employeeTableModel.getRowCount());
    employeeTableModel.clear();
    employeeTableModel.setRefreshOnForeignKeyConditionValuesSet(false);
    final Entity sales = getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_ID, 30);
    employeeTableModel.setForeignKeyConditionValues(TestDomain.EMP_DEPARTMENT_FK, Collections.singleton(sales));
    assertEquals(0, employeeTableModel.getRowCount());
    employeeTableModel.refresh();
    assertEquals(4, employeeTableModel.getRowCount());
  }

  @Test
  void nonMatchingConditionModelEntityType() {
    final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_DEPARTMENT, getConnectionProvider(),
            new DefaultFilterModelFactory(), new DefaultConditionModelFactory());
    assertThrows(IllegalArgumentException.class, () -> new SwingEntityTableModel(TestDomain.T_EMP, getConnectionProvider(),
            new SwingEntityTableSortModel(getConnectionProvider().getEntities()), conditionModel));
  }

  @Test
  void nullConditionModel() {
    assertThrows(NullPointerException.class, () -> new SwingEntityTableModel(TestDomain.T_EMP, null));
  }

  @Test
  void testFiltering() {
    testModel.refresh();
    final ColumnConditionModel<Attribute<String>, String> filterModel = testModel.getTableConditionModel().getFilterModel(TestDomain.DETAIL_STRING).orElse(null);
    filterModel.setEqualValue("a");
    testModel.filterContents();
    assertEquals(4, testModel.getFilteredItems().size());
  }

  @Test
  void getColumnIndex() {
    assertEquals(0, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_INT));
    assertEquals(1, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_DOUBLE));
    assertEquals(2, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_STRING));
    assertEquals(3, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_DATE));
    assertEquals(4, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_TIMESTAMP));
    assertEquals(5, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_BOOLEAN));
    assertEquals(6, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_BOOLEAN_NULLABLE));
    assertEquals(7, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_MASTER_FK));
    assertEquals(8, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_MASTER_NAME));
    assertEquals(9, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_MASTER_CODE));
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
    assertFalse(testModel.isCellEditable(0, testModel.getColumnModel().getColumnIndex(TestDomain.DETAIL_INT_DERIVED)));
    testModel.setEditable(false);
  }

  @Test
  void setValueAt() {
    final SwingEntityTableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();
    assertThrows(IllegalStateException.class, () -> tableModel.setValueAt("newname", 0, 1));
    tableModel.setEditable(true);
    tableModel.setValueAt("newname", 0, 1);
    final Entity entity = tableModel.getItemAt(0);
    assertEquals("newname", entity.get(TestDomain.EMP_NAME));
    assertThrows(RuntimeException.class, () -> tableModel.setValueAt("newname", 0, 0));
  }

  @Test
  void testSortComparator() {
    final Comparator<?> comparator = ((SwingEntityTableSortModel) testModel.getSortModel()).initializeColumnComparator(TestDomain.DETAIL_MASTER_FK);
    //make sure we get the comparator from the entity referenced by the foreign key
    assertEquals(comparator, getConnectionProvider().getEntities().getDefinition(TestDomain.T_MASTER).getComparator());
  }

  @Test
  void columnModel() {
    final TableColumn column = testModel.getColumnModel().getTableColumn(TestDomain.DETAIL_STRING);
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
  void indexOf() {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.refresh();
    tableModel.getSortModel().setSortingDirective(TestDomain.EMP_NAME, SortingDirective.ASCENDING);
    assertEquals(SortingDirective.ASCENDING, tableModel.getSortModel().getSortingState(TestDomain.EMP_NAME).getDirective());

    final Key pk1 = getConnectionProvider().getEntities().primaryKey(TestDomain.T_EMP, 10);//ADAMS
    assertEquals(0, tableModel.indexOf(pk1));

    final Key pk2 = getConnectionProvider().getEntities().primaryKey(TestDomain.T_EMP, -66);
    assertEquals(-1, tableModel.indexOf(pk2));
  }

  @Test
  void preferences() throws Exception {
    testModel.clearPreferences();

    final SwingEntityTableModel tableModel = createTestTableModel();
    assertTrue(tableModel.getColumnModel().isColumnVisible(TestDomain.DETAIL_STRING));

    tableModel.getColumnModel().hideColumn(TestDomain.DETAIL_STRING);
    tableModel.getColumnModel().moveColumn(1, 0);//double to 0, int to 1
    TableColumn column = tableModel.getColumnModel().getColumn(3);
    column.setWidth(150);//timestamp
    column = tableModel.getColumnModel().getColumn(5);
    column.setWidth(170);//entity_ref

    tableModel.savePreferences();

    final SwingEntityTableModel model = createTestTableModel();
    assertFalse(model.getColumnModel().isColumnVisible(TestDomain.DETAIL_STRING));
    assertEquals(0, model.getColumnModel().getColumnIndex(TestDomain.DETAIL_DOUBLE));
    assertEquals(1, model.getColumnModel().getColumnIndex(TestDomain.DETAIL_INT));
    column = model.getColumnModel().getColumn(3);
    assertEquals(150, column.getPreferredWidth());
    column = model.getColumnModel().getColumn(5);
    assertEquals(170, column.getPreferredWidth());

    model.clearPreferences();
    UserPreferences.flushUserPreferences();
  }
}
