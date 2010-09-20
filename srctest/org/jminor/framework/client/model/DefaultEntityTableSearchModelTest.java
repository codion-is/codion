package org.jminor.framework.client.model;

import org.jminor.common.model.Conjunction;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.*;
import org.junit.Test;

public class DefaultEntityTableSearchModelTest {

  @Test
  public void test() {
    EmpDept.init();
    final EntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.DB_PROVIDER);
    final EntityTableSearchModel model = tableModel.getSearchModel();
    assertEquals(EmpDept.T_EMPLOYEE, model.getEntityID());
    assertNotNull(model.getSearchableProperties());
    assertEquals(false, model.isSimpleSearch());
    model.setSearchConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, model.getSearchConjunction());
    assertEquals(9, model.getPropertyFilterModels().size());
    assertEquals(8, model.getPropertySearchModels().size());

    model.refresh();
    assertTrue(((ForeignKeySearchModel) model.getPropertySearchModel(EmpDept.EMPLOYEE_DEPARTMENT_FK)).getEntityComboBoxModel().getSize() > 1);
    model.clear();
    assertTrue(((ForeignKeySearchModel) model.getPropertySearchModel(EmpDept.EMPLOYEE_DEPARTMENT_FK)).getEntityComboBoxModel().getSize() == 0);

    assertFalse(model.isFilterEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertFalse(model.isSearchEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK));
  }
}
