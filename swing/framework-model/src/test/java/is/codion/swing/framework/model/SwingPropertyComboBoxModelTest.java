/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.database.Databases;
import is.codion.common.event.Event;
import is.codion.common.event.Events;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.model.tests.TestDomain;
import is.codion.swing.common.model.combobox.SwingFilteredComboBoxModel;

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
  private final Event<?> refreshEvent = Events.event();

  public SwingPropertyComboBoxModelTest() {
    final ColumnProperty property = DOMAIN.getDefinition(TestDomain.T_DEPARTMENT).getColumnProperty(TestDomain.DEPARTMENT_NAME);
    comboBoxModel = new SwingPropertyComboBoxModel(CONNECTION_PROVIDER, TestDomain.DEPARTMENT_NAME, null);
    refreshEvent.addListener(comboBoxModel::refresh);
  }

  @Test
  public void test() {
    assertEquals(0, comboBoxModel.getSize());
    refreshEvent.onEvent();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
