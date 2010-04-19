package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EntityListModelTest {

  @Test
  public void test() throws Exception {
    final EntityListModel model = new EntityListModel(EmpDept.T_DEPARTMENT, EntityDbConnectionTest.DB_PROVIDER);
    assertEquals(EmpDept.T_DEPARTMENT, model.getEntityID());
    model.refresh();
    assertEquals(4, model.getSize());
    model.setEntitySelectCriteria(new EntitySelectCriteria(EmpDept.T_DEPARTMENT, new SimpleCriteria("deptno <> 10"), EmpDept.DEPARTMENT_ID));
    model.refresh();
    assertEquals(3, model.getSize());
    assertEquals(3, model.getAllEntities().size());
    assertEquals("RESEARCH", model.getEntityAt(0).getValue(EmpDept.DEPARTMENT_NAME));

    model.getSelectionModel().addSelectionInterval(0, 0);
    assertEquals(1, model.getSelectedEntities().size());
  }
}
