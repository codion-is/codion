/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.value.AbstractValue;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityModelTest
        extends AbstractEntityModelTest<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityModel createDepartmentModel() {
    SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider());
    SwingEntityModel employeeModel = new SwingEntityModel(Employee.TYPE, departmentModel.connectionProvider());
    employeeModel.editModel().refreshComboBoxModels();
    departmentModel.addDetailModel(employeeModel, Employee.DEPARTMENT_FK).setActive(true);
    employeeModel.tableModel().queryConditionRequiredState().set(false);

    return departmentModel;
  }

  @Override
  protected SwingEntityModel createDepartmentModelWithoutDetailModel() {
    return new SwingEntityModel(Department.TYPE, connectionProvider());
  }

  @Override
  protected SwingEntityModel createEmployeeModel() {
    return new SwingEntityModel(Employee.TYPE, connectionProvider());
  }

  @Override
  protected SwingEntityEditModel createDepartmentEditModel() {
    return new SwingEntityEditModel(Department.TYPE, connectionProvider());
  }

  @Override
  protected SwingEntityTableModel createEmployeeTableModel() {
    return new SwingEntityTableModel(Employee.TYPE, connectionProvider());
  }

  @Override
  protected SwingEntityTableModel createDepartmentTableModel() {
    return new SwingEntityTableModel(Department.TYPE, connectionProvider());
  }

  @Test
  void isModified() {
    //here we're basically testing for the entity in the edit model being modified after
    //being set when selected in the table model, this usually happens when combo box models
    //are being filtered on property value change, see EmployeeEditModel.bindEvents()
    SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
    SwingEntityEditModel employeeEditModel = employeeModel.editModel();
    SwingEntityTableModel employeeTableModel = employeeModel.tableModel();

    EntityComboBoxModel comboBoxModel = employeeEditModel.foreignKeyComboBoxModel(Employee.MGR_FK);
    new EntityComboBoxModelValue(comboBoxModel).link(employeeEditModel.value(Employee.MGR_FK));
    employeeTableModel.refresh();
    for (Entity employee : employeeTableModel.items()) {
      employeeTableModel.selectionModel().setSelectedItem(employee);
      assertFalse(employeeEditModel.isModified());
    }
  }

  @Test
  public void testDetailModels() throws DatabaseException, ValidationException {
    assertTrue(departmentModel.containsDetailModel(Employee.TYPE));
    assertFalse(departmentModel.containsDetailModel(Department.TYPE));
    assertFalse(departmentModel.containsDetailModel(EmpModel.class));
    SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
    assertNotNull(employeeModel);
    assertTrue(departmentModel.activeDetailModels().contains(employeeModel));
    departmentModel.tableModel().refresh();
    SwingEntityEditModel employeeEditModel = employeeModel.editModel();
    EntityComboBoxModel departmentsComboBoxModel = employeeEditModel.foreignKeyComboBoxModel(Employee.DEPARTMENT_FK);
    departmentsComboBoxModel.refresh();
    Key primaryKey = connectionProvider().entities().primaryKey(Department.TYPE, 40);//operations, no employees
    departmentModel.tableModel().selectByKey(Collections.singletonList(primaryKey));
    Entity operations = departmentModel.tableModel().selectionModel().getSelectedItem();
    EntityConnection connection = departmentModel.connectionProvider().connection();
    connection.beginTransaction();
    try {
      departmentModel.editModel().delete();
      assertFalse(departmentsComboBoxModel.containsItem(operations));
      departmentModel.editModel().put(Department.ID, 99);
      departmentModel.editModel().put(Department.NAME, "nameit");
      Entity inserted = departmentModel.editModel().insert();
      assertTrue(departmentsComboBoxModel.containsItem(inserted));
      departmentModel.tableModel().selectionModel().setSelectedItem(inserted);
      departmentModel.editModel().put(Department.NAME, "nameitagain");
      departmentModel.editModel().update();
      assertEquals("nameitagain", departmentsComboBoxModel.entity(inserted.primaryKey()).orElse(null).get(Department.NAME));

      departmentModel.tableModel().selectByKey(Collections.singletonList(primaryKey.copyBuilder().with(Department.ID, 20).build()));
      departmentModel.editModel().put(Department.NAME, "NewName");
      departmentModel.editModel().update();

      for (Entity employee : employeeModel.tableModel().items()) {
        Entity dept = employee.referencedEntity(Employee.DEPARTMENT_FK);
        assertEquals("NewName", dept.get(Department.NAME));
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
              connection.selectSingle(Department.NAME, "OPERATIONS");
      departmentModel.tableModel().selectionModel().setSelectedItem(department);
      SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
      EntityComboBoxModel deptComboBoxModel = employeeModel.editModel()
              .foreignKeyComboBoxModel(Employee.DEPARTMENT_FK);
      deptComboBoxModel.refresh();
      deptComboBoxModel.setSelectedItem(department);
      departmentModel.tableModel().deleteSelected();
      assertEquals(3, employeeModel.editModel().foreignKeyComboBoxModel(Employee.DEPARTMENT_FK).getSize());
      assertNotNull(employeeModel.editModel().foreignKeyComboBoxModel(Employee.DEPARTMENT_FK).selectedValue());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void constructor() {
    SwingEntityEditModel editModel = new SwingEntityEditModel(Department.TYPE, connectionProvider());
    SwingEntityTableModel tableModel = new SwingEntityTableModel(Department.TYPE, connectionProvider());

    new SwingEntityModel(editModel);
    new SwingEntityModel(tableModel);

    tableModel = new SwingEntityTableModel(Department.TYPE, connectionProvider());
    assertNotEquals(editModel, new SwingEntityModel(tableModel).editModel());

    tableModel = new SwingEntityTableModel(editModel);
    assertEquals(editModel, new SwingEntityModel(tableModel).editModel());
  }

  @Test
  void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new SwingEntityModel(null, connectionProvider()));
  }

  @Test
  void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new SwingEntityModel(Employee.TYPE, null));
  }

  public static class EmpModel extends SwingEntityModel {
    public EmpModel(EntityConnectionProvider connectionProvider) {
      super(Employee.TYPE, connectionProvider);
    }
  }

  private static final class EntityComboBoxModelValue extends AbstractValue<Entity> {

    private final EntityComboBoxModel comboBoxModel;

    public EntityComboBoxModelValue(EntityComboBoxModel comboBoxModel) {
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
