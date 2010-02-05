package org.jminor.framework.client.ui.property;

import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.util.DateUtil;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;

import junit.framework.TestCase;

import javax.swing.JFormattedTextField;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTextPropertyLinkTest extends TestCase {

  private EntityEditModel model;

  public DateTextPropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.dbProvider).getEditModel();
  }

  public void test() throws Exception {
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
    model.setValue(EmpDept.EMPLOYEE_HIREDATE, dateValue);
    assertEquals("Text field should contain value", "03-10-1975", txtDate.getText());
  }
}