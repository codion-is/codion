package org.jminor.framework.client.model.combobox;

import org.jminor.common.db.User;
import org.jminor.framework.db.provider.EntityDbLocalProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import junit.framework.TestCase;

/**
 * User: Björn Darri
 * Date: 11.10.2009
 * Time: 21:44:41
 */
public class EntityComboBoxModelTest extends TestCase {

  private final EntityComboBoxModel comboBoxModel;

  public EntityComboBoxModelTest() {
    new EmpDept();
    comboBoxModel = new EntityComboBoxModel(EmpDept.T_DEPARTMENT, new EntityDbLocalProvider(new User("scott", "tiger")));
  }

  public void test() {
    assertTrue(comboBoxModel.getSize() == 0);
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}