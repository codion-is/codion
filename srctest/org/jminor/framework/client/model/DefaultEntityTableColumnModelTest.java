package org.jminor.framework.client.model;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * User: Björn Darri
 * Date: 28.7.2010
 * Time: 18:58:24
 */
public final class DefaultEntityTableColumnModelTest {

  @Test
  public void test() {
    EmpDept.init();
    final DefaultEntityTableColumnModel model = new DefaultEntityTableColumnModel(EmpDept.T_DEPARTMENT);
    assertEquals(EmpDept.T_DEPARTMENT, model.getEntityID());
    assertEquals(Entities.getVisibleProperties(EmpDept.T_DEPARTMENT), model.getColumnProperties());
  }
}
