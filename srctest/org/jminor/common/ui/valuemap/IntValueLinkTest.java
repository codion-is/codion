/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.EditModelValue;
import org.jminor.common.ui.LinkType;
import org.jminor.common.ui.ValueLinks;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import static org.junit.Assert.*;

public class IntValueLinkTest {

  @Test
  public void nullInitialValue() throws Exception {
    final EntityEditModel model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final IntField txt = new IntField();
    ValueLinks.intValueLink(txt, new EditModelValue<String, Integer>(model, EmpDept.EMPLOYEE_ID), LinkType.READ_WRITE, false, null);
    assertNull("Initial Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setInt(42);
    assertEquals("Integer value should be 42", 42, model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setText("");
    assertNull("Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    model.setValue(EmpDept.EMPLOYEE_ID, 33);
    assertEquals("Text field should contain value", "33", txt.getText());
  }

  @Test
  public void nonNullInitialValue() {
    final EntityEditModel model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
    model.setValue(EmpDept.EMPLOYEE_ID, 32);
    final IntField txt = new IntField();
    ValueLinks.intValueLink(txt, new EditModelValue<String, Integer>(model, EmpDept.EMPLOYEE_ID), LinkType.READ_WRITE, false, null);
    assertNotNull("Initial Integer value should not be null", model.getValue(EmpDept.EMPLOYEE_ID));
    assertEquals("32", txt.getText());
    assertEquals(Integer.valueOf(32), txt.getInt());
    txt.setInt(42);
    assertEquals("Integer value should be 42", 42, model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setText("");
    assertNull("Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    model.setValue(EmpDept.EMPLOYEE_ID, 33);
    assertEquals("Text field should contain value", "33", txt.getText());
  }
}