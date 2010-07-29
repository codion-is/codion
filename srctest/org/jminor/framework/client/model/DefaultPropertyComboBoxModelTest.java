/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * User: Bjorn Darri
 * Date: 11.10.2009
 * Time: 21:44:41
 */
public final class DefaultPropertyComboBoxModelTest {

  private final PropertyComboBoxModel comboBoxModel;
  private final Property.ColumnProperty property;
  private final Event refreshEvent = Events.event();

  public DefaultPropertyComboBoxModelTest() {
    new EmpDept();
    property = EntityRepository.getColumnProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME);
    comboBoxModel = new DefaultPropertyComboBoxModel(EmpDept.T_DEPARTMENT,
            EntityDbConnectionTest.DB_PROVIDER, property,
            null, refreshEvent);
  }

  @Test
  public void test() {
    assertNotNull(comboBoxModel.getDbProvider());
    assertEquals(property, comboBoxModel.getProperty());
    assertTrue(comboBoxModel.getSize() == 0);
    refreshEvent.fire();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
