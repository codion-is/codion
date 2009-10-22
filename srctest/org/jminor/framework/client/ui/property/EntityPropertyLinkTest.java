/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.util.DateUtil;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;

import junit.framework.TestCase;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: Björn Darri
 * Date: 13.1.2008
 * Time: 13:23:40
 */
public class EntityPropertyLinkTest extends TestCase {

  private EntityEditModel model;

  public EntityPropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.dbProvider).getEditModel();
  }

  public void testIntegerPropertyLink() {
    final IntField txt = new IntField();
    new IntTextPropertyLink(txt, model, EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_ID),
            true, LinkType.READ_WRITE);
    assertNull("Initial Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setInt(42);
    assertEquals("Integer value should be 42", 42, model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setText("");
    assertNull("Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
  }

  public void testDoublePropertyLink() {
    final DoubleField txt = new DoubleField();
    new DoubleTextPropertyLink(txt, model, EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_COMMISSION),
            true, LinkType.READ_WRITE);
    assertNull("Initial Double value should be null", model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    txt.setDouble(42.2);
    assertEquals("Double value should be 42.2", 42.2, model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    txt.setText("");
    assertNull("Double value should be null", model.getValue(EmpDept.EMPLOYEE_COMMISSION));
  }

  public void testStringPropertyLink() {
    final JTextField txt = new JTextField();
    new TextPropertyLink(txt, model, EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME),
            true, LinkType.READ_WRITE);
    assertNull("Initial String value should be null", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("darri");
    assertEquals("String value should be 'darri", "darri", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("");
    assertEquals("String value should be empty", "", model.getValue(EmpDept.EMPLOYEE_NAME));
  }

  public void testDatePropertyLink() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat(DateFormats.SHORT_DASH);
    final JFormattedTextField txtDate = UiUtil.createFormattedField(DateUtil.getDateMask(format), true);
    new DateTextPropertyLink(txtDate, model, EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_HIREDATE),
            LinkType.READ_WRITE, format);
    assertNull("Initial Date value should be null", model.getValue(EmpDept.EMPLOYEE_HIREDATE));
    final Date dateValue = format.parse("03-10-1975");
    txtDate.setText(format.format(dateValue));
    assertEquals("Date value should be 'dateValue'", dateValue, model.getValue(EmpDept.EMPLOYEE_HIREDATE));
    txtDate.setText("");
    assertNull("Date value should be null", model.getValue(EmpDept.EMPLOYEE_HIREDATE));
  }
}
