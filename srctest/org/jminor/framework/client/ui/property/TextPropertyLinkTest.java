package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;

import junit.framework.TestCase;

import javax.swing.JTextField;

public class TextPropertyLinkTest extends TestCase {

  private EntityEditModel model;

  public TextPropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.dbProvider).getEditModel();
  }

  public void test() throws Exception {
    final JTextField txt = new JTextField();
    new TextPropertyLink(txt, model, EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME),
            true, LinkType.READ_WRITE);
    assertNull("Initial String value should be null", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("darri");
    assertEquals("String value should be 'darri", "darri", model.getValue(EmpDept.EMPLOYEE_NAME));
    txt.setText("");
    assertEquals("String value should be empty", "", model.getValue(EmpDept.EMPLOYEE_NAME));
    model.setValue(EmpDept.EMPLOYEE_NAME, "Björn");
    assertEquals("Text field should contain value", "Björn", txt.getText());
  }
}
