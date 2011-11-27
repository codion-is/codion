/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import java.text.NumberFormat;

import static org.junit.Assert.*;

public class DoubleValueLinkTest {

  private final ValueChangeMapEditModel<String, Object> model;

  public DoubleValueLinkTest() {
    model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
  }

  @Test
  public void test() throws Exception {
    final DoubleField txt = new DoubleField();
    txt.setDecimalSymbol(DoubleField.POINT);
    final NumberFormat format = NumberFormat.getNumberInstance();
    format.setMaximumFractionDigits(4);
    final TextValueLink<String> valueLink = new DoubleValueLink<String>(txt, model, EmpDept.EMPLOYEE_COMMISSION, true,
            LinkType.READ_WRITE, format);
    ValueLinkValidators.addValidator(valueLink, txt, model);/*Range 100 - 2000*/
    assertNull("Initial Double value should be null", model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    txt.setDouble(1000.5);
    assertEquals("Double value should be 1000.5", 1000.5, model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    assertNull("ToolTip should not contain invalid message", txt.getToolTipText());
    txt.setDouble(50d);//value out of range, invalid
    assertTrue("ToolTip should contain invalid message", txt.getToolTipText().length() > 0);
    txt.setDouble(2050d);//value out of range, invalid
    assertNotNull("ToolTip should contain invalid message", txt.getToolTipText());
    txt.setDouble(1500d);//value within of range, valid
    assertNull("ToolTip should not contain invalid message", txt.getToolTipText());
    txt.setText("");
    assertNull("Double value should be null", model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    model.setValue(EmpDept.EMPLOYEE_COMMISSION, 950.1234);
    assertEquals("Text field should contain value", "950.1234", txt.getText());
    model.setValue(EmpDept.EMPLOYEE_COMMISSION, 950.123456);
    assertEquals("Text field should contain value", "950.1235", txt.getText());
  }
}