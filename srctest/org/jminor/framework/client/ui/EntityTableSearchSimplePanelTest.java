package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EntityTableSearchSimplePanelTest {

  @Test
  public void test() {
    EmpDept.init();
    final EntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.DB_PROVIDER);
    final EntityTableSearchSimplePanel panel = new EntityTableSearchSimplePanel(tableModel.getSearchModel(), tableModel);
    panel.setSearchTest("OPERATIONS");
    panel.performSearch();
    assertEquals(1, tableModel.getRowCount());
  }
}
