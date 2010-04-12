/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * User: Bjorn Darri
 * Date: 11.10.2009
 * Time: 21:44:41
 */
public class PropertyComboBoxModelTest {

  private final PropertyComboBoxModel comboBoxModel;
  private final Event refreshEvent = new Event();

  public PropertyComboBoxModelTest() {
    new EmpDept();
    comboBoxModel = new PropertyComboBoxModel(EmpDept.T_DEPARTMENT,
            EntityRepository.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME),
            EntityDbConnectionTest.DB_PROVIDER, null, refreshEvent);
  }

  @Test
  public void test() {
    assertTrue(comboBoxModel.getSize() == 0);
    refreshEvent.fire();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
