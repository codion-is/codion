/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class SwingPropertyComboBoxModelTest {

  private final FilteredComboBoxModel comboBoxModel;
  private final Event refreshEvent = Events.event();

  public SwingPropertyComboBoxModelTest() {
    TestDomain.init();
    final Property.ColumnProperty property = Entities.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    comboBoxModel = new SwingPropertyComboBoxModel(TestDomain.T_DEPARTMENT,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER, property, null);
    refreshEvent.addListener(comboBoxModel::refresh);
  }

  @Test
  public void test() {
    assertTrue(comboBoxModel.getSize() == 0);
    refreshEvent.fire();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
