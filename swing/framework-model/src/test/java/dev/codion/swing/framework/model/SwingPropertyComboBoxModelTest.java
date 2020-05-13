/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.model;

import dev.codion.common.db.database.Databases;
import dev.codion.common.event.Event;
import dev.codion.common.event.Events;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.domain.Domain;
import dev.codion.framework.domain.property.ColumnProperty;
import dev.codion.framework.model.tests.TestDomain;
import dev.codion.swing.common.model.combobox.SwingFilteredComboBoxModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SwingPropertyComboBoxModelTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

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
    refreshEvent.onEvent();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
