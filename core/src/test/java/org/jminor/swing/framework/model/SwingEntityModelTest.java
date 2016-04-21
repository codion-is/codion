/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.valuemap.EditModelValues;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.AbstractEntityModelTest;
import org.jminor.framework.model.DefaultEntityModel;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.common.ui.ValueLinks;

import org.junit.Test;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public final class SwingEntityModelTest extends AbstractEntityModelTest<SwingEntityModel> {

  @Override
  protected SwingEntityModel createDepartmentModel() {
    final SwingEntityModel departmentModel = new SwingEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final EntityModel employeeModel = new EmpModel(departmentModel.getConnectionProvider());
    departmentModel.addDetailModel(employeeModel);
    departmentModel.setDetailModelForeignKey(employeeModel, TestDomain.EMP_DEPARTMENT_FK);
    departmentModel.addLinkedDetailModel(employeeModel);
    employeeModel.getTableModel().setQueryCriteriaRequired(false);

    return departmentModel;
  }

  @Override
  protected SwingEntityModel createDepartmentModelWithoutDetailModel() {
    return new SwingEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Override
  protected SwingEntityModel createEmployeeModel() {
    return new SwingEntityModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Override
  protected EntityEditModel createDepartmentEditModel() {
    return new SwingEntityEditModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Override
  protected EntityTableModel createEmployeeTableModel() {
    return new DefaultEntityTableModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Override
  protected EntityTableModel createDepartmentTableModel() {
    return new DefaultEntityTableModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Test
  public void isModified() {
    //here we're basically testing for the entity in the edit model being modified after
    //being set when selected in the table model, this usually happens when combo box models
    //are being filtered on property value change, see EmployeeEditModel.bindEvents()
    final SwingEntityModel employeeModel = (SwingEntityModel) departmentModel.getDetailModel(TestDomain.T_EMP);
    final SwingEntityEditModel employeeEditModel = (SwingEntityEditModel) employeeModel.getEditModel();
    final EntityTableModel employeeTableModel = employeeModel.getTableModel();
    ValueLinks.selectedItemValueLink(new JComboBox<>((ComboBoxModel<Entity>)
            employeeEditModel.getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK)),
            EditModelValues.<Entity>value(employeeEditModel, TestDomain.EMP_MGR_FK));
    employeeTableModel.refresh();
    for (final Entity employee : employeeTableModel.getAllItems()) {
      employeeTableModel.getSelectionModel().setSelectedItem(employee);
      assertFalse(employeeEditModel.isModified());
    }
  }

  @Test
  public void testDetailModels() throws CancelException, DatabaseException, ValidationException {
    assertTrue(departmentModel.containsDetailModel(TestDomain.T_EMP));
    assertNull(departmentModel.getDetailModel(DefaultEntityModel.class));
    assertFalse(departmentModel.containsDetailModel("undefined"));
    assertFalse(departmentModel.containsDetailModel(DefaultEntityModel.class));
    final EntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    assertNotNull(employeeModel);
    assertTrue(departmentModel.getLinkedDetailModels().contains(employeeModel));
    departmentModel.refresh();
    final SwingEntityEditModel employeeEditModel = (SwingEntityEditModel) employeeModel.getEditModel();
    final EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.getForeignKeyComboBoxModel(
            Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK));
    departmentsComboBoxModel.refresh();
    final Entity.Key primaryKey = Entities.key(TestDomain.T_DEPARTMENT);
    primaryKey.put(TestDomain.DEPARTMENT_ID, 40);//operations, no employees
    final List<Entity.Key> keys = new ArrayList<>();
    keys.add(primaryKey);
    departmentModel.getTableModel().setSelectedByKey(keys);
    final Entity operations = departmentModel.getTableModel().getSelectionModel().getSelectedItem();
    try {
      departmentModel.getConnectionProvider().getConnection().beginTransaction();
      departmentModel.getEditModel().delete();
      assertFalse(departmentsComboBoxModel.contains(operations, true));
      departmentModel.getEditModel().setValue(TestDomain.DEPARTMENT_ID, 99);
      departmentModel.getEditModel().setValue(TestDomain.DEPARTMENT_NAME, "nameit");
      final Entity inserted = departmentModel.getEditModel().insert().get(0);
      assertTrue(departmentsComboBoxModel.contains(inserted, true));
      departmentModel.getTableModel().getSelectionModel().setSelectedItem(inserted);
      departmentModel.getEditModel().setValue(TestDomain.DEPARTMENT_NAME, "nameitagain");
      departmentModel.getEditModel().update();
      assertEquals("nameitagain", departmentsComboBoxModel.getEntity(inserted.getKey()).get(TestDomain.DEPARTMENT_NAME));

      primaryKey.put(TestDomain.DEPARTMENT_ID, 20);//research
      departmentModel.getTableModel().setSelectedByKey(keys);
      departmentModel.getEditModel().setValue(TestDomain.DEPARTMENT_NAME, "NewName");
      departmentModel.getEditModel().update();

      for (final Entity employee : employeeModel.getTableModel().getAllItems()) {
        final Entity dept = employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
        assertEquals("NewName", dept.get(TestDomain.DEPARTMENT_NAME));
      }
    }
    finally {
      departmentModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void test() throws Exception {
    try {
      departmentModel.getConnectionProvider().getConnection().beginTransaction();
      departmentModel.refresh();
      final Entity department = departmentModel.getConnectionProvider().getConnection().selectSingle(
              TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "OPERATIONS");
      departmentModel.getTableModel().getSelectionModel().setSelectedItem(department);
      final EntityModel employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
      final EntityComboBoxModel deptComboBoxModel = ((SwingEntityEditModel) employeeModel.getEditModel())
              .getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
      deptComboBoxModel.refresh();
      deptComboBoxModel.setSelectedItem(department);
      departmentModel.getTableModel().deleteSelected();
      assertEquals(3, ((SwingEntityEditModel) employeeModel.getEditModel()).getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).getSize());
      assertNotNull(((SwingEntityEditModel) employeeModel.getEditModel()).getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).getSelectedValue());
    }
    finally {
      departmentModel.getConnectionProvider().getConnection().rollbackTransaction();
    }
  }

  @Test
  public void constructor() {
    new SwingEntityModel(new SwingEntityEditModel(TestDomain.T_DEPARTMENT,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER));
    new SwingEntityModel(new DefaultEntityTableModel(TestDomain.T_DEPARTMENT,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER));
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullEntityID() {
    new SwingEntityModel(null, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullConnectionProvider() {
    new SwingEntityModel(TestDomain.T_EMP, null);
  }

  public static class EmpModel extends SwingEntityModel {
    public EmpModel(final EntityConnectionProvider connectionProvider) {
      super(new SwingEntityEditModel(TestDomain.T_EMP, connectionProvider));
      ((SwingEntityEditModel) getEditModel()).getForeignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).refresh();
      ((SwingEntityEditModel) getEditModel()).getForeignKeyComboBoxModel(TestDomain.EMP_MGR_FK).refresh();
    }
  }
}
