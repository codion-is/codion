/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class IntPropertyLinkTest {

  private ChangeValueMapEditModel<String, Object> model;

  public IntPropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.DB_PROVIDER).getEditModel();
  }

  @Test
  public void test() throws Exception {
    final IntField txt = new IntField();
    new IntValueLink(txt, model, EmpDept.EMPLOYEE_ID, true, LinkType.READ_WRITE);
    assertNull("Initial Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setInt(42);
    assertEquals("Integer value should be 42", 42, model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setText("");
    assertNull("Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    model.setValue(EmpDept.EMPLOYEE_ID, 33);
    assertEquals("Text field should contain value", "33", txt.getText());
  }
}