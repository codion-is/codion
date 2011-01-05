/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntValueLinkTest {

  private final ValueChangeMapEditModel<String, Object> model;

  public IntValueLinkTest() {
    model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.DB_PROVIDER);
  }

  @Test
  public void test() throws Exception {
    final IntField txt = new IntField();
    new IntValueLink<String>(txt, model, EmpDept.EMPLOYEE_ID, true, LinkType.READ_WRITE);
    assertNull("Initial Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setInt(42);
    assertEquals("Integer value should be 42", 42, model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setText("");
    assertNull("Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    model.setValue(EmpDept.EMPLOYEE_ID, 33);
    assertEquals("Text field should contain value", "33", txt.getText());
  }
}