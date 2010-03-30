/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DatePropertyLinkTest {

  private EntityEditModel model;

  public DatePropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.dbProvider).getEditModel();
  }

  @Test
  public void test() throws Exception {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DASH);
    final JFormattedTextField txtDate = UiUtil.createFormattedField(DateUtil.getDateMask(format), true);
    new DatePropertyLink(txtDate, model, EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_HIREDATE),
            LinkType.READ_WRITE, format);
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