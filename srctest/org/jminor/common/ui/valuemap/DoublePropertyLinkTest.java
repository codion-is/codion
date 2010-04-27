/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.*;
import org.junit.Test;

public class DoublePropertyLinkTest {

  private ValueChangeMapEditModel<String, Object> model;

  public DoublePropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.DB_PROVIDER).getEditModel();
  }

  @Test
  public void test() throws Exception {
    final DoubleField txt = new DoubleField();
    txt.setDecimalSymbol(DoubleField.POINT);
    new DoubleValueLink(txt, model, EmpDept.EMPLOYEE_COMMISSION, true, LinkType.READ_WRITE);
    assertNull("Initial Double value should be null", model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    txt.setDouble(1000.5);
    assertEquals("Double value should be 1000.5", 1000.5, model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    txt.setDouble(50d);//value out of range, invalid
    assertTrue("ToolTip should contain invalid message", txt.getToolTipText().length() > 0);
    txt.setDouble(1500d);//value out of range, invalid
    assertNull("ToolTip should not contain invalid message", txt.getToolTipText());
    txt.setText("");
    assertNull("Double value should be null", model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    model.setValue(EmpDept.EMPLOYEE_COMMISSION, 950d);
    assertEquals("Text field should contain value", "950.0", txt.getText());
  }
}