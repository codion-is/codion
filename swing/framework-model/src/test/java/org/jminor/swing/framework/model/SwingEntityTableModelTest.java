/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.PreferencesUtil;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.model.AbstractEntityTableModelTest;
import org.jminor.framework.model.DefaultEntityTableConditionModel;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;
import org.jminor.framework.model.DefaultPropertyFilterModelProvider;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.TestDomain;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityTableModelTest extends AbstractEntityTableModelTest<SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityTableModel createTestTableModel() {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_DETAIL, getConnectionProvider()) {
      @Override
      protected List<Entity> performQuery() {
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
    deptModel.setSortingDirective(TestDomain.DEPARTMENT_NAME, SortingDirective.ASCENDING, false);

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
  public void refreshOnForeignKeyConditionValuesSet() throws DatabaseException {
    final SwingEntityTableModel employeeTableModel = createEmployeeTableModel();
    assertEquals(0, employeeTableModel.getRowCount());
    final Entity accounting = getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_ID, 10);
    final ForeignKeyProperty deptFkProperty = getConnectionProvider().getDomain().getDefinition(TestDomain.T_EMP)
            .getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK);
    employeeTableModel.setForeignKeyConditionValues(deptFkProperty,
            singletonList(accounting));
    assertEquals(7, employeeTableModel.getRowCount());
    employeeTableModel.clear();
    employeeTableModel.setRefreshOnForeignKeyConditionValuesSet(false);
    final Entity sales = getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_ID, 30);
    employeeTableModel.setForeignKeyConditionValues(deptFkProperty, Collections.singleton(sales));
    assertEquals(0, employeeTableModel.getRowCount());
    employeeTableModel.refresh();
    assertEquals(4, employeeTableModel.getRowCount());
  }

  @Test
  public void nonMatchingConditionModelEntityId() {
    final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_DEPARTMENT, getConnectionProvider(),
            new DefaultPropertyFilterModelProvider(), new DefaultPropertyConditionModelProvider());
    assertThrows(IllegalArgumentException.class, () -> new SwingEntityTableModel(TestDomain.T_EMP, getConnectionProvider(),
            new SwingEntityTableModel.DefaultEntityTableSortModel(getConnectionProvider().getDomain(), TestDomain.T_EMP), conditionModel));
  }

  @Test
  public void nullConditionModel() {
    assertThrows(NullPointerException.class, () -> new SwingEntityTableModel(TestDomain.T_EMP, null));
  }

  @Test
  public void testFiltering() {
    testModel.refresh();
    final ColumnConditionModel<Property> filterModel = testModel.getConditionModel().getPropertyFilterModel(TestDomain.DETAIL_STRING);
    filterModel.setLikeValue("a");
    testModel.filterContents();
    assertEquals(4, testModel.getFilteredItems().size());
  }

  @Test
  public void getPropertyColumnIndex() {
    assertEquals(0, testModel.getPropertyColumnIndex(TestDomain.DETAIL_INT));
    assertEquals(1, testModel.getPropertyColumnIndex(TestDomain.DETAIL_DOUBLE));
    assertEquals(2, testModel.getPropertyColumnIndex(TestDomain.DETAIL_STRING));
    assertEquals(3, testModel.getPropertyColumnIndex(TestDomain.DETAIL_DATE));
    assertEquals(4, testModel.getPropertyColumnIndex(TestDomain.DETAIL_TIMESTAMP));
    assertEquals(5, testModel.getPropertyColumnIndex(TestDomain.DETAIL_BOOLEAN));
    assertEquals(6, testModel.getPropertyColumnIndex(TestDomain.DETAIL_BOOLEAN_NULLABLE));
    assertEquals(7, testModel.getPropertyColumnIndex(TestDomain.DETAIL_MASTER_FK));
    assertEquals(8, testModel.getPropertyColumnIndex(TestDomain.DETAIL_MASTER_NAME));
    assertEquals(9, testModel.getPropertyColumnIndex(TestDomain.DETAIL_MASTER_CODE));
  }

  @Test
  public void getValueAt() {
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
  public void isEditable() {
    testModel.setEditable(true);
    assertTrue(testModel.isCellEditable(0, 0));
    assertFalse(testModel.isCellEditable(0, testModel.getPropertyColumnIndex(TestDomain.DETAIL_INT_DERIVED)));
    testModel.setEditable(false);
  }

  @Test
  public void setValueAt() {
    final SwingEntityTableModel tableModel = createEmployeeTableModel();
    tableModel.refresh();
    assertThrows(IllegalStateException.class, () -> tableModel.setValueAt("newname", 0, 1));
    tableModel.setEditable(true);
    tableModel.setValueAt("newname", 0, 1);
    final Entity entity = tableModel.getItemAt(0);
    assertEquals("newname", entity.getString(TestDomain.EMP_NAME));
    assertThrows(RuntimeException.class, () -> tableModel.setValueAt("newname", 0, 0));
  }

  @Test
  public void testSortComparator() {
    final Property masterFKProperty = getConnectionProvider().getDomain().getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_MASTER_FK);
    final Comparator comparator = ((SwingEntityTableModel.DefaultEntityTableSortModel) testModel.getSortModel()).initializeColumnComparator(masterFKProperty);
    //make sure we get the comparator from the entity referenced by the foreign key
    assertEquals(comparator, getConnectionProvider().getDomain().getDefinition(TestDomain.T_MASTER).getComparator());
  }

  @Test
  public void columnModel() {
    final Property property = getConnectionProvider().getDomain().getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_STRING);
    final TableColumn column = testModel.getColumnModel().getTableColumn(property);
    assertEquals(property, column.getIdentifier());
  }

  @Test
  public void getColumnClass() {
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
  public void indexOf() {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.refresh();
    tableModel.getSortModel().setSortingDirective(getConnectionProvider().getDomain().getDefinition(TestDomain.T_EMP).getProperty(TestDomain.EMP_NAME),
            SortingDirective.ASCENDING, false);
    assertEquals(SortingDirective.ASCENDING, tableModel.getSortModel()
            .getSortingState(getConnectionProvider().getDomain().getDefinition(TestDomain.T_EMP).getProperty(TestDomain.EMP_NAME)).getDirective());

    final Entity.Key pk1 = getConnectionProvider().getDomain().key(TestDomain.T_EMP, 10);//ADAMS
    assertEquals(0, tableModel.indexOf(pk1));

    final Entity.Key pk2 = getConnectionProvider().getDomain().key(TestDomain.T_EMP, -66);
    assertEquals(-1, tableModel.indexOf(pk2));
  }

  @Test
  public void preferences() throws Exception {
    testModel.clearPreferences();

    final SwingEntityTableModel tableModel = createTestTableModel();
    assertTrue(tableModel.getColumnModel().isColumnVisible(getConnectionProvider().getDomain().getDefinition(TestDomain.T_DETAIL).getColumnProperty(TestDomain.DETAIL_STRING)));

    tableModel.getColumnModel().hideColumn(getConnectionProvider().getDomain().getDefinition(TestDomain.T_DETAIL).getColumnProperty(TestDomain.DETAIL_STRING));
    tableModel.getColumnModel().moveColumn(1, 0);//double to 0, int to 1
    TableColumn column = tableModel.getColumnModel().getColumn(3);
    column.setWidth(150);//timestamp
    column = tableModel.getColumnModel().getColumn(5);
    column.setWidth(170);//entity_ref

    tableModel.savePreferences();

    final SwingEntityTableModel model = createTestTableModel();
    assertFalse(model.getColumnModel().isColumnVisible(getConnectionProvider().getDomain().getDefinition(TestDomain.T_DETAIL).getColumnProperty(TestDomain.DETAIL_STRING)));
    assertEquals(0, model.getPropertyColumnIndex(TestDomain.DETAIL_DOUBLE));
    assertEquals(1, model.getPropertyColumnIndex(TestDomain.DETAIL_INT));
    column = model.getColumnModel().getColumn(3);
    assertEquals(150, column.getPreferredWidth());
    column = model.getColumnModel().getColumn(5);
    assertEquals(170, column.getPreferredWidth());

    model.clearPreferences();
    PreferencesUtil.flushUserPreferences();
  }
}
