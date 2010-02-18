package org.jminor.framework.client.model;

import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * User: Björn Darri
 * Date: 11.10.2009
 * Time: 21:44:41
 */
public class EntityComboBoxModelTest extends TestCase {

  private final EntityComboBoxModel comboBoxModel;

  public EntityComboBoxModelTest() {
    new EmpDept();
    comboBoxModel = new EntityComboBoxModel(EmpDept.T_EMPLOYEE, EntityDbConnectionTest.dbProvider);
  }

  public void test() throws Exception {
    assertTrue(comboBoxModel.getSize() == 0);
    comboBoxModel.refresh();
    assertTrue(comboBoxModel.getSize() > 0);

    //test foreign key filtering
    final Entity sales = comboBoxModel.getDbProvider().getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setForeignKeyFilterEntities(EmpDept.EMPLOYEE_DEPARTMENT_FK, Arrays.asList(sales));
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      assertEquals(((Entity) comboBoxModel.getElementAt(0)).getEntityValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), sales);
    }
    final Entity research = comboBoxModel.getDbProvider().getEntityDb().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "RESEARCH");
    comboBoxModel.createForeignKeyFilterComboBoxModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).setSelectedItem(research);
    for (int i = 0; i < comboBoxModel.getSize(); i++) {
      assertEquals(((Entity) comboBoxModel.getElementAt(0)).getEntityValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), research);
    }
  }
}