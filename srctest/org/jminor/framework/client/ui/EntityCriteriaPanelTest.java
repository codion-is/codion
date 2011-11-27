package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

public class EntityCriteriaPanelTest {

  @Test
  public void test() {
    EmpDept.init();
    final EntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.CONNECTION_PROVIDER);
    new EntityCriteriaPanel(tableModel);
  }
}
