/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.value.AbstractValue;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityModelTest
        extends AbstractEntityModelTest<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityModel createDepartmentModel() {
    SwingEntityModel departmentModel = new SwingEntityModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    SwingEntityModel employeeModel = new SwingEntityModel(TestDomain.T_EMP, departmentModel.connectionProvider());
    employeeModel.editModel().refreshComboBoxModels();
    departmentModel.addDetailModel(employeeModel);
    departmentModel.setDetailModelForeignKey(employeeModel, TestDomain.EMP_DEPARTMENT_FK);
    departmentModel.addLinkedDetailModel(employeeModel);
    employeeModel.tableModel().queryConditionRequiredState().set(false);

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
  void isModified() {
    //here we're basically testing for the entity in the edit model being modified after
    //being set when selected in the table model, this usually happens when combo box models
    //are being filtered on property value change, see EmployeeEditModel.bindEvents()
    SwingEntityModel employeeModel = departmentModel.detailModel(TestDomain.T_EMP);
    SwingEntityEditModel employeeEditModel = employeeModel.editModel();
    SwingEntityTableModel employeeTableModel = employeeModel.tableModel();

    SwingEntityComboBoxModel comboBoxModel = employeeEditModel.foreignKeyComboBoxModel(TestDomain.EMP_MGR_FK);
    new EntityComboBoxModelValue(comboBoxModel).link(employeeEditModel.value(TestDomain.EMP_MGR_FK));
    employeeTableModel.refresh();
    for (Entity employee : employeeTableModel.items()) {
      employeeTableModel.selectionModel().setSelectedItem(employee);
      assertFalse(employeeEditModel.isModified());
    }
  }

  @Test
  public void testDetailModels() throws DatabaseException, ValidationException {
    assertTrue(departmentModel.containsDetailModel(TestDomain.T_EMP));
    assertFalse(departmentModel.containsDetailModel(TestDomain.T_DEPARTMENT));
    assertFalse(departmentModel.containsDetailModel(EmpModel.class));
    SwingEntityModel employeeModel = departmentModel.detailModel(TestDomain.T_EMP);
    assertNotNull(employeeModel);
    assertTrue(departmentModel.linkedDetailModels().contains(employeeModel));
    departmentModel.tableModel().refresh();
    SwingEntityEditModel employeeEditModel = employeeModel.editModel();
    EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.foreignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
    departmentsComboBoxModel.refresh();
    Key primaryKey = getConnectionProvider().entities().primaryKey(TestDomain.T_DEPARTMENT, 40);//operations, no employees
    departmentModel.tableModel().setSelectedByKey(Collections.singletonList(primaryKey));
    Entity operations = departmentModel.tableModel().selectionModel().getSelectedItem();
    EntityConnection connection = departmentModel.connectionProvider().connection();
    connection.beginTransaction();
    try {
      departmentModel.editModel().delete();
      assertFalse(departmentsComboBoxModel.containsItem(operations));
      departmentModel.editModel().put(TestDomain.DEPARTMENT_ID, 99);
      departmentModel.editModel().put(TestDomain.DEPARTMENT_NAME, "nameit");
      Entity inserted = departmentModel.editModel().insert();
      assertTrue(departmentsComboBoxModel.containsItem(inserted));
      departmentModel.tableModel().selectionModel().setSelectedItem(inserted);
      departmentModel.editModel().put(TestDomain.DEPARTMENT_NAME, "nameitagain");
      departmentModel.editModel().update();
      assertEquals("nameitagain", departmentsComboBoxModel.entity(inserted.primaryKey()).orElse(null).get(TestDomain.DEPARTMENT_NAME));

      departmentModel.tableModel().setSelectedByKey(Collections.singletonList(primaryKey.copyBuilder().with(TestDomain.DEPARTMENT_ID, 20).build()));
      departmentModel.editModel().put(TestDomain.DEPARTMENT_NAME, "NewName");
      departmentModel.editModel().update();

      for (Entity employee : employeeModel.tableModel().items()) {
        Entity dept = employee.referencedEntity(TestDomain.EMP_DEPARTMENT_FK);
        assertEquals("NewName", dept.get(TestDomain.DEPARTMENT_NAME));
      }
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void getDetailModelNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> departmentModel.detailModel(EmpModel.class));
  }

  @Test
  public void test() throws Exception {
    super.test();
    EntityConnection connection = departmentModel.connectionProvider().connection();
    connection.beginTransaction();
    try {
      departmentModel.tableModel().refresh();
      Entity department =
              connection.selectSingle(TestDomain.DEPARTMENT_NAME, "OPERATIONS");
      departmentModel.tableModel().selectionModel().setSelectedItem(department);
      SwingEntityModel employeeModel = departmentModel.detailModel(TestDomain.T_EMP);
      EntityComboBoxModel deptComboBoxModel = employeeModel.editModel()
              .foreignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK);
      deptComboBoxModel.refresh();
      deptComboBoxModel.setSelectedItem(department);
      departmentModel.tableModel().deleteSelected();
      assertEquals(3, employeeModel.editModel().foreignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).getSize());
      assertNotNull(employeeModel.editModel().foreignKeyComboBoxModel(TestDomain.EMP_DEPARTMENT_FK).selectedValue());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void constructor() {
    SwingEntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    SwingEntityTableModel tableModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, getConnectionProvider());

    new SwingEntityModel(editModel);
    new SwingEntityModel(tableModel);

    tableModel = new SwingEntityTableModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
    assertNotEquals(editModel, new SwingEntityModel(tableModel).editModel());

    tableModel = new SwingEntityTableModel(editModel);
    assertEquals(editModel, new SwingEntityModel(tableModel).editModel());
  }

  @Test
  void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new SwingEntityModel(null, getConnectionProvider()));
  }

  @Test
  void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new SwingEntityModel(TestDomain.T_EMP, null));
  }

  public static class EmpModel extends SwingEntityModel {
    public EmpModel(EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_EMP, connectionProvider);
    }
  }

  private static final class EntityComboBoxModelValue extends AbstractValue<Entity> {

    private final SwingEntityComboBoxModel comboBoxModel;

    public EntityComboBoxModelValue(SwingEntityComboBoxModel comboBoxModel) {
      this.comboBoxModel = comboBoxModel;
      comboBoxModel.addSelectionListener(selected -> notifyValueChange());
    }

    @Override
    protected void setValue(Entity value) {
      comboBoxModel.setSelectedItem(value);
    }

    @Override
    public Entity get() {
      return comboBoxModel.selectedValue();
    }
  }
}
