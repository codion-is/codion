package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.swing.table.DefaultTableColumnModel;

public class EntityTableSearchModelTest {

  @Test
  public void testConstructor() {
    try {
      new EntityTableSearchModel(null, new DefaultTableColumnModel(), EntityDbConnectionTest.DB_PROVIDER, false);
    }
    catch (IllegalArgumentException e) {}
    try {
      new EntityTableSearchModel("entityID", null, EntityDbConnectionTest.DB_PROVIDER, false);
    }
    catch (IllegalArgumentException e) {}
  }

  @Test
  public void test() {
    new EmpDept();
    final EntityTableModel tableModel = new EntityTableModel(EmpDept.T_EMPLOYEE, EntityDbConnectionTest.DB_PROVIDER);
    final EntityTableSearchModel model = tableModel.getSearchModel();
    assertEquals(EmpDept.T_EMPLOYEE, model.getEntityID());
    assertNotNull(model.getColumnModel());
    assertEquals(false, model.isSimpleSearch());
    model.setSearchConjunction(CriteriaSet.Conjunction.OR);
    assertEquals(CriteriaSet.Conjunction.OR, model.getSearchConjunction());
    assertEquals(9, model.getPropertyFilterModels().size());
    assertEquals(8, model.getPropertySearchModels().size());

    model.refreshSearchComboBoxModels();
    assertTrue(model.getPropertySearchModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).getEntityComboBoxModel().getSize() > 1);
    model.clearSearchComboBoxModels();
    assertTrue(model.getPropertySearchModel(EmpDept.EMPLOYEE_DEPARTMENT_FK).getEntityComboBoxModel().getSize() == 1);

    assertFalse(model.isFilterEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertFalse(model.isSearchEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK));
  }
}
