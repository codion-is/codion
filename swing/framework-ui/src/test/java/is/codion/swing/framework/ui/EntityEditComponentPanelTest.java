/*
 * Copyright (c) 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;
import is.codion.swing.framework.ui.component.EntityComponents;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public final class EntityEditComponentPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void test() {
    SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
    assertThrows(IllegalArgumentException.class, () -> new EntityEditComponentPanel(editModel, new EntityComponents(editModel.entities().definition(Department.TYPE))));
    EntityEditComponentPanel componentPanel = new EntityEditComponentPanel(editModel);
    componentPanel.createTextField(Employee.NAME);
    assertThrows(IllegalStateException.class, () -> componentPanel.createTextField(Employee.NAME));
    JTextField nameField = (JTextField) componentPanel.component(Employee.NAME).get();
    assertNotNull(nameField);
    assertThrows(IllegalStateException.class, () -> componentPanel.createTextField(Employee.NAME));
    assertTrue(componentPanel.attributes().contains(Employee.NAME));
    assertEquals(componentPanel.attribute(nameField), Employee.NAME);
    assertThrows(IllegalArgumentException.class, () -> componentPanel.attribute(new JTextField()));
    assertTrue(componentPanel.component(Employee.JOB).isNull());
  }

  @Test
  void excludeComponentFromSelection() {
    SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
    EntityEditComponentPanel componentPanel = new EntityEditComponentPanel(editModel);
    assertThrows(NullPointerException.class, () -> componentPanel.excludeComponentFromSelection().add(null));
  }
}
