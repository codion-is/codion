/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.model.PreferencesUtil;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityTableConditionModel;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;
import org.jminor.framework.model.DefaultPropertyFilterModelProvider;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.testing.AbstractEntityTableModelTest;
import org.jminor.framework.model.testing.TestDomain;
import org.jminor.swing.common.model.table.SortingDirective;

import org.junit.Test;

import javax.swing.table.TableColumn;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public final class SwingEntityTableModelTest extends AbstractEntityTableModelTest<SwingEntityEditModel, SwingEntityTableModel> {

  private static final Entities ENTITIES = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  @Override
  protected SwingEntityTableModel createTestTableModel() {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER) {
      @Override
      protected List<Entity> performQuery() {
        return testEntities;
      }
    };
    tableModel.setEditModel(new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER));

    return tableModel;
  }

  @Override
  protected SwingEntityTableModel createMasterTableModel() {
    return new SwingEntityTableModel(TestDomain.T_MASTER, CONNECTION_PROVIDER);
  }

  @Override
  protected SwingEntityTableModel createDetailTableModel() {
    return new SwingEntityTableModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);
  }

  @Override
  protected SwingEntityTableModel createEmployeeTableModelWithoutEditModel() {
    return new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  }

  @Override
  protected SwingEntityTableModel createDepartmentTableModel() {
    final SwingEntityTableModel deptModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider());
    deptModel.setEditModel(new SwingEntityEditModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider()));

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
    return new SwingEntityEditModel(TestDomain.T_MASTER, CONNECTION_PROVIDER);
  }

  @Override
  protected SwingEntityEditModel createDetailEditModel() {
    return new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonMatchingConditionModelEntityId() {
    final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER,
            new DefaultPropertyFilterModelProvider(), new DefaultPropertyConditionModelProvider());
    new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER,
            new SwingEntityTableModel.DefaultEntityTableSortModel(ENTITIES, TestDomain.T_EMP), conditionModel);
  }

  @Test(expected = NullPointerException.class)
  public void nullConditionModel() {
    new SwingEntityTableModel(TestDomain.T_EMP, null);
  }

  @Test
  public void testFiltering() {
    testModel.refresh();
    final ColumnConditionModel<Property> filterModel = testModel.getConditionModel().getPropertyFilterModel(TestDomain.DETAIL_STRING);
    filterModel.setLikeValue("a");
    testModel.filterContents();
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

  @Test(expected = UnsupportedOperationException.class)
  public void setValueAt() {
    testModel.setValueAt("hello", 0, 0);
  }

  @Test
  public void testSortComparator() {
    final Property masterFKProperty = ENTITIES.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK);
    final Comparator comparator = ((SwingEntityTableModel.DefaultEntityTableSortModel) testModel.getSortModel()).initializeColumnComparator(masterFKProperty);
    assertEquals(comparator, ENTITIES.getComparator(TestDomain.T_MASTER));
  }

  @Test
  public void getValues() {
    testModel.refresh();
    final Property property = ENTITIES.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING);
    final Collection values = testModel.getValues(property, false);
    assertEquals(5, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("b"));
    assertTrue(values.contains("c"));
    assertTrue(values.contains("d"));
    assertTrue(values.contains("e"));
    assertFalse(values.contains("zz"));
  }

  @Test
  public void columnModel() {
    final Property property = ENTITIES.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING);
    final TableColumn column = testModel.getColumnModel().getTableColumn(property);
    assertEquals(property, column.getIdentifier());
  }

  @Test
  public void getColumnClass() {
    assertEquals(Integer.class, testModel.getColumnClass(0));
    assertEquals(Double.class, testModel.getColumnClass(1));
    assertEquals(String.class, testModel.getColumnClass(2));
    assertEquals(Date.class, testModel.getColumnClass(3));
    assertEquals(Timestamp.class, testModel.getColumnClass(4));
    assertEquals(Boolean.class, testModel.getColumnClass(5));
    assertEquals(Boolean.class, testModel.getColumnClass(6));
    assertEquals(Object.class, testModel.getColumnClass(7));
  }

  @Test
  public void testTheRest() {
    assertFalse(testModel.isCellEditable(0, 0));
  }

  @Test
  public void indexOf() {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    tableModel.refresh();
    tableModel.getSortModel().setSortingDirective(ENTITIES.getProperty(TestDomain.T_EMP, TestDomain.EMP_NAME),
            SortingDirective.ASCENDING, false);
    assertEquals(SortingDirective.ASCENDING, tableModel.getSortModel()
            .getSortingDirective(ENTITIES.getProperty(TestDomain.T_EMP, TestDomain.EMP_NAME)));

    final Entity.Key pk1 = ENTITIES.key(TestDomain.T_EMP);
    pk1.put(TestDomain.EMP_ID, 10);//ADAMS
    assertEquals(0, tableModel.indexOf(pk1));

    final Entity.Key pk2 = ENTITIES.key(TestDomain.T_EMP);
    pk2.put(TestDomain.EMP_ID, -66);
    assertEquals(-1, tableModel.indexOf(pk2));
  }

  @Test
  public void preferences() throws Exception {
    testModel.clearPreferences();

    final SwingEntityTableModel tableModel = createTestTableModel();
    assertTrue(tableModel.getColumnModel().isColumnVisible(ENTITIES.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING)));

    tableModel.getColumnModel().setColumnVisible(ENTITIES.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING), false);
    tableModel.getColumnModel().moveColumn(1, 0);//double to 0, int to 1
    TableColumn column = tableModel.getColumnModel().getColumn(3);
    column.setWidth(150);//timestamp
    column = tableModel.getColumnModel().getColumn(5);
    column.setWidth(170);//entity_ref

    tableModel.savePreferences();

    final SwingEntityTableModel model = createTestTableModel();
    assertFalse(model.getColumnModel().isColumnVisible(ENTITIES.getColumnProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_STRING)));
    assertTrue(model.getPropertyColumnIndex(TestDomain.DETAIL_DOUBLE) == 0);
    assertTrue(model.getPropertyColumnIndex(TestDomain.DETAIL_INT) == 1);
    column = model.getColumnModel().getColumn(3);
    assertEquals(150, column.getPreferredWidth());
    column = model.getColumnModel().getColumn(5);
    assertEquals(170, column.getPreferredWidth());

    model.clearPreferences();
    PreferencesUtil.flushUserPreferences();
  }
}
