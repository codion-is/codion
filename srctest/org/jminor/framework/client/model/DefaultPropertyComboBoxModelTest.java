/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class DefaultPropertyComboBoxModelTest {

  private final FilteredComboBoxModel comboBoxModel;
  private final Event refreshEvent = Events.event();

  public DefaultPropertyComboBoxModelTest() {
    TestDomain.init();
    final Property.ColumnProperty property = Entities.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    comboBoxModel = new DefaultPropertyComboBoxModel(TestDomain.T_DEPARTMENT,
            LocalEntityConnectionTest.CONNECTION_PROVIDER, property, null, refreshEvent);
  }

  @Test
  public void test() {
    assertTrue(comboBoxModel.getSize() == 0);
    refreshEvent.fire();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
