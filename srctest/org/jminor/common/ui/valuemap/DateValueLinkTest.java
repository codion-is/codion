/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateValueLinkTest {

  private ValueChangeMapEditModel<String, Object> model;

  public DateValueLinkTest() {
    model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.DB_PROVIDER);
  }

  @Test
  public void test() throws Exception {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DASH);
    final JFormattedTextField txtDate = UiUtil.createFormattedField(DateUtil.getDateMask(format), true);
    new DateValueLink<String>(txtDate, model, EmpDept.EMPLOYEE_HIREDATE, LinkType.READ_WRITE, format, false);
    assertNull("Initial Date value should be null", model.getValue(EmpDept.EMPLOYEE_HIREDATE));
    final Date dateValue = format.parse("03-10-1975");
    txtDate.setText(format.format(dateValue));
    assertEquals("Date value should be 'dateValue'", dateValue, model.getValue(EmpDept.EMPLOYEE_HIREDATE));
    txtDate.setText("");
    assertNull("Date value should be null", model.getValue(EmpDept.EMPLOYEE_HIREDATE));
    model.setValue(EmpDept.EMPLOYEE_HIREDATE, dateValue);
    assertEquals("Text field should contain value", "03-10-1975", txtDate.getText());
  }
}