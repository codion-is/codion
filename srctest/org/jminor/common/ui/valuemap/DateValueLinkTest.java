/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.ValueLinks;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EditModelValue;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateValueLinkTest {

  @Test
  public void test() throws Exception {
    final EntityEditModel model = new DefaultEntityEditModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DASH);
    final JFormattedTextField txtDate = UiUtil.createFormattedField(DateUtil.getDateMask(format));
    ValueLinks.dateValueLink(txtDate, new EditModelValue(model, EmpDept.EMPLOYEE_HIREDATE), LinkType.READ_WRITE, format, false);
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