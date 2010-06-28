/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.ui.control.LinkType;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.swing.JTextField;

public class TextValueLinkTest {

  private ValueChangeMapEditModel<String, Object> model;

  public TextValueLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.DB_PROVIDER).getEditModel();
  }

  @Test
  public void test() throws Exception {
    final JTextField txt = new JTextField();
    new TextValueLink<String>(txt, model, EmpDept.EMPLOYEE_NAME, true, LinkType.READ_WRITE);
    assertTrue("String value should be empty", model.isValueNull(EmpDept.EMPLOYEE_NAME));
    assertNull("Initial String value should be null", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("darri");
    assertFalse("String value should not be empty", model.isValueNull(EmpDept.EMPLOYEE_NAME));
    assertEquals("String value should be 'darri", "darri", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("");
    assertTrue("String value should be null", model.isValueNull(EmpDept.EMPLOYEE_NAME));
    model.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
    assertEquals("Text field should contain value", "Björn", txt.getText());
  }
}
