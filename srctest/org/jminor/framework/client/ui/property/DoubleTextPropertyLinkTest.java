package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;

import static org.junit.Assert.*;
import org.junit.Test;

public class DoubleTextPropertyLinkTest {

  private EntityEditModel model;

  public DoubleTextPropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.dbProvider).getEditModel();
  }

  @Test
  public void test() throws Exception {
    final DoubleField txt = new DoubleField();
    txt.setDecimalSymbol(DoubleField.POINT);
    new DoubleTextPropertyLink(txt, model, EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_COMMISSION),
            true, LinkType.READ_WRITE);
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