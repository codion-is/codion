/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.model.TestDomain;
import org.jminor.swing.common.model.combobox.SwingFilteredComboBoxModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SwingPropertyComboBoxModelTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()));

  private final SwingFilteredComboBoxModel comboBoxModel;
  private final Event refreshEvent = Events.event();

  public SwingPropertyComboBoxModelTest() {
    final ColumnProperty property = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT).getColumnProperty(TestDomain.DEPARTMENT_NAME);
    comboBoxModel = new SwingPropertyComboBoxModel(TestDomain.T_DEPARTMENT,
            CONNECTION_PROVIDER, property, null);
    refreshEvent.addListener(comboBoxModel::refresh);
  }

  @Test
  public void test() {
    assertEquals(0, comboBoxModel.getSize());
    refreshEvent.fire();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
