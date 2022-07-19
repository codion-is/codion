/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.event.Event;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.test.TestDomain;
import is.codion.swing.common.model.component.combobox.SwingFilteredComboBoxModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SwingPropertyComboBoxModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
            .domainClassName(TestDomain.class.getName())
            .user(UNIT_TEST_USER)
            .build();

  private final SwingFilteredComboBoxModel<String> comboBoxModel;
  private final Event<?> refreshEvent = Event.event();

  public SwingPropertyComboBoxModelTest() {
    comboBoxModel = new SwingPropertyComboBoxModel<>(CONNECTION_PROVIDER, TestDomain.DEPARTMENT_NAME);
    refreshEvent.addListener(comboBoxModel::refresh);
  }

  @Test
  void test() {
    assertEquals(0, comboBoxModel.getSize());
    refreshEvent.onEvent();
    assertTrue(comboBoxModel.getSize() > 0);
  }
}
