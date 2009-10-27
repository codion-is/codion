package org.jminor.framework.client.model;

import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import junit.framework.TestCase;

import java.sql.Timestamp;

public class EntityEditModelTest extends TestCase {

  private EmployeeModel employeeModel;

  public void test() throws Exception {
    final EntityEditModel editModel = employeeModel.getEditModel();

    employeeModel.getTableModel().refresh();
    assertTrue(editModel.isEntityNull());
    assertFalse(editModel.getEntityModifiedState().isActive());

    employeeModel.getTableModel().setSelectedItemIndex(0);

    assertTrue("Active entity is not equal to the selected entity",
            editModel.getEntityCopy().propertyValuesEqual(employeeModel.getTableModel().getEntityAtViewIndex(0)));

    assertFalse("Active entity is null after an entity is selected", editModel.isEntityNull());
    assertFalse(editModel.getEntityModifiedState().isActive());
    employeeModel.getTableModel().getSelectionModel().clearSelection();
    assertTrue("Active entity is not null after selection is cleared", editModel.isEntityNull());
    assertFalse(editModel.getEntityModifiedState().isActive());
    assertTrue("Active entity is not null after selection is cleared", editModel.getEntityCopy().isNull());

    employeeModel.getTableModel().setSelectedItemIndex(0);
    assertTrue("Active entity is null after selection is made", !editModel.getEntityCopy().isNull());

    final Double originalCommission = (Double) editModel.getValue(EmpDept.EMPLOYEE_COMMISSION);
    final double commission = 66.7;
    final Timestamp originalHiredate = (Timestamp) editModel.getValue(EmpDept.EMPLOYEE_HIREDATE);
    final Timestamp hiredate = new Timestamp(System.currentTimeMillis());
    final String originalName = (String) editModel.getValue(EmpDept.EMPLOYEE_NAME);
    final String name = "Mr. Mr";

    editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    assertTrue(editModel.getEntityModifiedState().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    editModel.setValue(EmpDept.EMPLOYEE_NAME, name);

    assertEquals("Commission does not fit", editModel.getValue(EmpDept.EMPLOYEE_COMMISSION), commission);
    assertEquals("Hiredate does not fit", editModel.getValue(EmpDept.EMPLOYEE_HIREDATE), hiredate);
    assertEquals("Name does not fit", editModel.getValue(EmpDept.EMPLOYEE_NAME), name);

    editModel.setValue(EmpDept.EMPLOYEE_COMMISSION, originalCommission);
    assertTrue(editModel.getEntityModifiedState().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_HIREDATE, originalHiredate);
    assertTrue(editModel.getEntityModifiedState().isActive());
    editModel.setValue(EmpDept.EMPLOYEE_NAME, originalName);
    assertFalse(editModel.getEntityModifiedState().isActive());
    editModel.clear();
    assertTrue("Active entity is not null after model is cleared", employeeModel.getEditModel().getEntityCopy().isNull());
  }

  @Override
  protected void setUp() throws Exception {
    employeeModel = new EmployeeModel(EntityDbConnectionTest.dbProvider);
  }
}
