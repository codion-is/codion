package org.jminor.framework.client.ui.property;

import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.beans.EmployeeModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import junit.framework.TestCase;

public class ComboBoxPropertyLinkTest extends TestCase {

  private EntityEditModel model;

  public ComboBoxPropertyLinkTest() {
    model = new EmployeeModel(EntityDbConnectionTest.dbProvider).getEditModel();
  }

  public void test() throws Exception {
    final Property.ForeignKeyProperty fkProperty = EntityRepository.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK);
    final EntityComboBoxModel comboBoxModel = model.createEntityComboBoxModel(fkProperty);
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    comboBoxModel.refresh();
    new ComboBoxPropertyLink(comboBox, model, fkProperty);
    assertTrue(comboBox.getSelectedItem() == null);
    Entity department = model.getDbProvider().getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    model.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    assertEquals(comboBox.getSelectedItem(), department);
    department = model.getDbProvider().getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "OPERATIONS");
    comboBox.setSelectedItem(department);
    assertEquals(model.getValue(fkProperty), department);
  }
}
