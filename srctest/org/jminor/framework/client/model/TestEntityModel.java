/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.EntityDbLocalProvider;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.model.EmpDept;

import junit.framework.TestCase;

import java.sql.Timestamp;

public class TestEntityModel extends TestCase {

  private static EntityModel testModel;

  static {
    try {
      new EmpDept();
      FrameworkSettings.get().setProperty(FrameworkSettings.USE_QUERY_RANGE, false);
      testModel  = new EmployeeModel(new EntityDbLocalProvider(new User("scott", "tiger")));
    }
    catch (UserException e) {
      e.printStackTrace();
    }
  }

  public TestEntityModel(final String name) {
    super(name);
  }

  public void testSelection() throws Exception {
    testModel.refresh();
    assertTrue(testModel.isActiveEntityNull());
    assertFalse(testModel.getActiveEntityModifiedState().isActive());
    testModel.getTableModel().setSelectedItemIdx(0);
    assertFalse("Active entity is null after an entity is selected", testModel.isActiveEntityNull());
    assertTrue("Active entity is not equal to the selected entity",
            testModel.getActiveEntityCopy().propertyValuesEqual(testModel.getTableModel().getEntityAtViewIndex(0)));
    assertFalse(testModel.getActiveEntityModifiedState().isActive());
    testModel.getTableModel().getSelectionModel().clearSelection();
    assertTrue("Active entity is not null after selection is cleared", testModel.isActiveEntityNull());
    assertFalse(testModel.getActiveEntityModifiedState().isActive());
  }

  public void testEdit() throws Exception {
    //changes to property values in a selected entity should be reverted when it's deselected
    testModel.getTableModel().refresh();
    testModel.getTableModel().setSelectedItemIdx(0);
    assertFalse(testModel.getActiveEntityModifiedState().isActive());
    final Double originalCommission = (Double) testModel.getValue(EmpDept.EMPLOYEE_COMMISSION);
    final double commission = 66.7;
    final Timestamp originalHiredate = (Timestamp) testModel.getValue(EmpDept.EMPLOYEE_HIREDATE);
    final Timestamp hiredate = new Timestamp(System.currentTimeMillis());
    final String originalName = (String) testModel.getValue(EmpDept.EMPLOYEE_NAME);
    final String name = "Mr. Mr";

    testModel.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    assertTrue(testModel.getActiveEntityModifiedState().isActive());
    testModel.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    testModel.setValue(EmpDept.EMPLOYEE_NAME, name);

    assertEquals("Commission does not fit", testModel.getValue(EmpDept.EMPLOYEE_COMMISSION), commission);
    assertEquals("Hiredate does not fit", testModel.getValue(EmpDept.EMPLOYEE_HIREDATE), hiredate);
    assertEquals("Name does not fit", testModel.getValue(EmpDept.EMPLOYEE_NAME), name);

    testModel.setValue(EmpDept.EMPLOYEE_COMMISSION, originalCommission);
    assertTrue(testModel.getActiveEntityModifiedState().isActive());
    testModel.setValue(EmpDept.EMPLOYEE_HIREDATE, originalHiredate);
    assertTrue(testModel.getActiveEntityModifiedState().isActive());
    testModel.setValue(EmpDept.EMPLOYEE_NAME, originalName);
    assertFalse(testModel.getActiveEntityModifiedState().isActive());

    testModel.getTableModel().getSelectionModel().clearSelection();
    assertTrue("Active entity is not null after selection is cleared", testModel.getActiveEntityCopy().isNull());
  }
}