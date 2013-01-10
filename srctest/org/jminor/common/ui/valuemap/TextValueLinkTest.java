/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.EditModelValue;
import org.jminor.common.ui.ValueLinks;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.*;

public class TextValueLinkTest {

  @Test
  public void nullInitialValue() throws Exception {
    final EntityEditModel model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final JTextField txt = new JTextField();
    ValueLinks.textValueLink(txt, new EditModelValue<String, String>(model, EmpDept.EMPLOYEE_NAME), null, true, false);
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

  @Test
  public void noneNullInitialValue() throws Exception {
    final EntityEditModel model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final JTextField txt = new JTextField();
    model.setValue(EmpDept.EMPLOYEE_NAME, "name");
    ValueLinks.textValueLink(txt, new EditModelValue<String, String>(model, EmpDept.EMPLOYEE_NAME), null, true, false);
    assertFalse("String value should not be empty", model.isValueNull(EmpDept.EMPLOYEE_NAME));
    assertEquals("name", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("darri");
    assertFalse("String value should not be empty", model.isValueNull(EmpDept.EMPLOYEE_NAME));
    assertEquals("String value should be 'darri", "darri", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("");
    assertTrue("String value should be null", model.isValueNull(EmpDept.EMPLOYEE_NAME));
    model.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
    assertEquals("Text field should contain value", "Björn", txt.getText());
  }
}
