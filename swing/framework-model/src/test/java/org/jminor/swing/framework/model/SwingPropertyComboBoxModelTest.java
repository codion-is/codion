/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.testing.TestDomain;
import org.jminor.swing.common.model.combobox.SwingFilteredComboBoxModel;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class SwingPropertyComboBoxModelTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()), Databases.getInstance());

  private final SwingFilteredComboBoxModel comboBoxModel;
  private final Event refreshEvent = Events.event();

  public SwingPropertyComboBoxModelTest() {
    final Property.ColumnProperty property = ENTITIES.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    comboBoxModel = new SwingPropertyComboBoxModel(TestDomain.T_DEPARTMENT,
            CONNECTION_PROVIDER, property, null);
    refreshEvent.addListener(comboBoxModel::refresh);
  }

  @Test
  public void test() {
    assertTrue(comboBoxModel.getSize() == 0);
    refreshEvent.fire();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
