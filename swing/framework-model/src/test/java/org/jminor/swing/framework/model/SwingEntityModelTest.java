/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.exception.ValidationException;
import org.jminor.framework.model.AbstractEntityModelTest;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.TestDomain;
import org.jminor.swing.common.ui.value.SelectedValues;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityModelTest
        extends AbstractEntityModelTest<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityModel createDepartmentModel() {
    final SwingEntityModel departmentModel = new SwingEntityModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    final SwingEntityModel employeeModel = new SwingEntityModel(TestDomain.T_EMP, departmentModel.getConnectionProvider());
    employeeModel.getEditModel().refreshComboBoxModels();
    departmentModel.addDetailModel(employeeModel);
    departmentModel.setDetailModelForeignKey(employeeModel, TestDomain.EMP_DEPARTMENT_FK);
    departmentModel.addLinkedDetailModel(employeeModel);
    employeeModel.getTableModel().getQueryConditionRequiredState().set(false);

    return departmentModel;
  }

  @Override
  protected SwingEntityModel createDepartmentModelWithoutDetailModel() {
    return new SwingEntityModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
  }

  @Override
  protected SwingEntityModel createEmployeeModel() {
    return new SwingEntityModel(TestDomain.T_EMP, getConnectionProvider());
  }

  @Override
  protected SwingEntityEditModel createDepartmentEditModel() {
    return new SwingEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
  }

  @Override
  protected SwingEntityTableModel createEmployeeTableModel() {
    return new SwingEntityTableModel(TestDomain.T_EMP, getConnectionProvider());
  }

  @Override
  protected SwingEntityTableModel createDepartmentTableModel() {
    return new SwingEntityTableModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
  }

  @Test
  public void isModified() {
    //here we're basically testing for the entity in the edit model being modified after
    //being set when selected in the table model, this usually happens when combo box models
    //are being filtered on property value change, see EmployeeEditModel.bindEvents()
    final SwingEntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    final SwingEntityEditModel employeeEditModel = employeeModel.getEditModel();
    final SwingEntityTableModel employeeTableModel = employeeModel.getTableModel();
    employeeEditModel.<Entity>value(TestDomain.EMP_MGR_FK).link(
            SelectedValues.selectedValue(new JComboBox<>(employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK))));
    employeeTableModel.refresh();
    for (final Entity employee : employeeTableModel.getItems()) {
      employeeTableModel.getSelectionModel().setSelectedItem(employee);
      assertFalse(employeeEditModel.isModified());
    }
  }

  @Test
  public void testDetailModels() throws DatabaseException, ValidationException {
    assertTrue(departmentModel.containsDetailModel(TestDomain.T_EMP));
    assertFalse(departmentModel.containsDetailModel("undefined"));
    assertFalse(departmentModel.containsDetailModel(EmpModel.class));
    final SwingEntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    assertNotNull(employeeModel);
    assertTrue(departmentModel.getLinkedDetailModels().contains(employeeModel));
    departmentModel.refresh();
    final SwingEntityEditModel employeeEditModel = employeeModel.getEditModel();
    final EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.getForeignKeyComboBoxModel(
            getConnectionProvider().getDomain().getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK));
    departmentsComboBoxModel.refresh();
    final Entity.Key primaryKey = getConnectionProvider().getDomain().key(TestDomain.T_DEPARTMENT, 40);//operations, no employees
    final List<Entity.Key> keys = new ArrayList<>();
    keys.add(primaryKey);
    departmentModel.getTableModel().setSelectedByKey(keys);
    final Entity operations = departmentModel.getTableModel().getSelectionModel().getSelectedItem();
    try {
      departmentModel.getConnectionProvider().getConnection().beginTransaction();
      departmentModel.getEditModel().delete();
      assertFalse(departmentsComboBoxModel.contains(operations, true));
      departmentModel.getEditModel().put(TestDomain.DEPARTMENT_ID, 99);
      departmentModel.getEditModel().put(TestDomain.DEPARTMENT_NAME, "nameit");
      final Entity inserted = departmentModel.getEditModel().insert();
      assertTrue(departmentsComboBoxModel.contains(inserted, true));
      departmentModel.getTableModel().getSelectionModel().setSelectedItem(inserted);
      departmentModel.getEditModel().put(TestDomain.DEPARTMENT_NAME, "nameitagain");
      departmentModel.getEditModel().update();
      assertEquals("nameitagain", departmentsComboBoxModel.getEntity(inserted.getKey()).get(TestDomain.DEPARTMENT_NAME));

      primaryKey.put(TestDomain.DEPARTMENT_ID, 20);//research
      departmentModel.getTableModel().setSelectedByKey(keys);
      departmentModel.getEditModel().put(TestDomain.DEPARTMENT_NAME, "NewName");
      departmentModel.getEditModel().update();

      for (final Entity employee : employeeModel.getTableModel().getItems()) {
        final Entity dept = employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
        assertEquals("NewName", dept.get(TestDomain.DEPARTMENT_NAME));
      }
    }
    finally {
      departmentModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void getDetailModelNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> departmentModel.getDetailModel(EmpModel.class));
  }

  @Test
  public void test() throws Exception {
    super.test();
    try {
      departmentModel.getConnectionProvider().getConnection().beginTransaction();
      departmentModel.refresh();
      final Entity department = departmentModel.getConnectionProvider().getConnection().selectSingle(
              TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "OPERATIONS");
      departmentModel.getTableModel().getSelectionModel().setSelectedItem(department);
      final SwingEntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
      final EntityComboBoxModel deptComboBoxModel = employeeModel.getEditModel()
              .getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
      deptComboBoxModel.refresh();
      deptComboBoxModel.setSelectedItem(department);
      departmentModel.getTableModel().deleteSelected();
      assertEquals(3, employeeModel.getEditModel().getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).getSize());
      assertNotNull(employeeModel.getEditModel().getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).getSelectedValue());
    }
    finally {
      departmentModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void constructor() {
    final SwingEntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, getConnectionProvider());

    new SwingEntityModel(editModel);
    new SwingEntityModel(tableModel);

    tableModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    assertNotEquals(editModel, new SwingEntityModel(tableModel).getEditModel());

    tableModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    tableModel.setEditModel(editModel);
    assertEquals(editModel, new SwingEntityModel(tableModel).getEditModel());
  }

  @Test
  public void constructorNullEntityId() {
    assertThrows(NullPointerException.class, () -> new SwingEntityModel(null, getConnectionProvider()));
  }

  @Test
  public void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new SwingEntityModel(TestDomain.T_EMP, null));
  }

  public static class EmpModel extends SwingEntityModel {
    public EmpModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_EMP, connectionProvider);
    }
  }
}
