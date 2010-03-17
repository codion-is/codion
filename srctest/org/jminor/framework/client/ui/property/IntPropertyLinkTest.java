/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class IntPropertyLinkTest {

  private EntityEditModel model;

  public IntPropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.dbProvider).getEditModel();
  }

  @Test
  public void test() throws Exception {
    final IntField txt = new IntField();
    new IntPropertyLink(txt, model, EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_ID),
            true, LinkType.READ_WRITE);
    assertNull("Initial Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setInt(42);
    assertEquals("Integer value should be 42", 42, model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setText("");
    assertNull("Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    model.setValue(EmpDept.EMPLOYEE_ID, 33);
    assertEquals("Text field should contain value", "33", txt.getText());
  }
}