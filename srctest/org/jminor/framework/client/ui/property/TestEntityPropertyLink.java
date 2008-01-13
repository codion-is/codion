/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.Constants;
import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.dbprovider.LocalEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.Entity;

import junit.framework.TestCase;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.util.Date;

/**
 * User: Bj�rn Darri
 * Date: 13.1.2008
 * Time: 13:23:40
 */
public class TestEntityPropertyLink extends TestCase {

  private EntityModel model;

  public TestEntityPropertyLink() throws UserException {
    super("TestEntityPropertyLink");
    new EmpDept();
    model = new EmployeeModel(new LocalEntityDbProvider(new User("scott", "tiger")));
  }

  @SuppressWarnings({"UnnecessaryBoxing"})
  public void testIntegerPropertyLink() {
    final IntField txt = new IntField();
    new IntTextPropertyLink(model, Entity.repository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_ID),
            txt, "", true, LinkType.READ_WRITE, null);
    assertNull("Initial Integer value should be null", model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setInteger(42);
    assertEquals("Integer value should be 42.2", new Integer(42), model.getValue(EmpDept.EMPLOYEE_ID));
    txt.setText("");
    assertEquals("Integer value should be null", Constants.INTEGER_NULL_VALUE, model.getValue(EmpDept.EMPLOYEE_ID));
  }

  @SuppressWarnings({"UnnecessaryBoxing"})
  public void testDoublePropertyLink() {
    final DoubleField txt = new DoubleField();
    new DoubleTextPropertyLink(model, Entity.repository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_COMMISSION),
            txt, "", true, LinkType.READ_WRITE, null);
    assertNull("Initial Double value should be null", model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    txt.setDouble(42.2);
    assertEquals("Double value should be 42.2", new Double(42.2), model.getValue(EmpDept.EMPLOYEE_COMMISSION));
    txt.setText("");
    assertEquals("Double value should be null", new Double(Constants.DOUBLE_NULL_VALUE), model.getValue(EmpDept.EMPLOYEE_COMMISSION));
  }

  public void testStringPropertyLink() {
    final JTextField txt = new JTextField();
    new TextPropertyLink(model, Entity.repository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME),
            txt, "", true, LinkType.READ_WRITE, null);
    assertNull("Initial String value should be null", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("darri");
    assertEquals("String value should be 'darri", "darri", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("");
    assertEquals("String value should be empty", "", model.getValue(EmpDept.EMPLOYEE_NAME));
  }

  public void testDatePropertyLink() {
    final ShortDashDateFormat format = new ShortDashDateFormat();
    final JFormattedTextField txtDate = UiUtil.createFormattedField(format.getDateMask());
    new DateTextPropertyLink(model, Entity.repository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_HIREDATE),
            txtDate, "", LinkType.READ_WRITE, null, format, format.getDateMask());
    assertNull("Initial Date value should be null", model.getValue(EmpDept.EMPLOYEE_HIREDATE));
    final Date now = new Date();
    model.uiSetValue(EmpDept.EMPLOYEE_HIREDATE, now);//hmm, why didn't txtDate.setText(format.format(now)) work?
    assertEquals("Date value should be now", now, model.getValue(EmpDept.EMPLOYEE_HIREDATE));
    txtDate.setText("");
    assertNull("Date value should be null", model.getValue(EmpDept.EMPLOYEE_HIREDATE));
  }
}
