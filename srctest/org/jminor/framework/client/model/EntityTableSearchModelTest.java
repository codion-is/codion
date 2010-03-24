package org.jminor.framework.client.model;

import org.jminor.common.db.CriteriaSet;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class EntityTableSearchModelTest {

  @Test
  public void test() {
    new EmpDept();
    final EntityTableModel tableModel = new EntityTableModel(EmpDept.T_DEPARTMENT, EntityDbConnectionTest.dbProvider);
    final EntityTableSearchModel model = tableModel.getSearchModel();
    assertEquals(EmpDept.T_DEPARTMENT, model.getEntityID());
    assertNotNull(model.getTableColumnModel());
    assertEquals(false, model.isSimpleSearch());
    model.setSearchConjunction(CriteriaSet.Conjunction.OR);
    assertEquals(CriteriaSet.Conjunction.OR, model.getSearchConjunction());
  }
}
