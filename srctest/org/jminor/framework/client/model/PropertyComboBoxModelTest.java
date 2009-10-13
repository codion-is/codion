package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;

import junit.framework.TestCase;

/**
 * User: Björn Darri
 * Date: 11.10.2009
 * Time: 21:44:41
 */
public class PropertyComboBoxModelTest extends TestCase {

  private final PropertyComboBoxModel comboBoxModel;
  private final Event refreshEvent = new Event();

  public PropertyComboBoxModelTest() {
    new EmpDept();
    comboBoxModel = new PropertyComboBoxModel(EmpDept.T_DEPARTMENT,
            EntityRepository.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME),
            EntityDbConnectionTest.dbProvider, null, refreshEvent);
  }

  public void test() {
    assertTrue(comboBoxModel.getSize() == 0);
    refreshEvent.fire();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
