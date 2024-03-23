/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2024, Björn Darri Sigurðsson.
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
  void selectableComponents() {
    SwingEntityEditModel editModel = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
    EntityEditComponentPanel componentPanel = new EntityEditComponentPanel(editModel);
    assertThrows(NullPointerException.class, () -> componentPanel.selectableComponents().add(null));
    assertThrows(IllegalArgumentException.class, () -> componentPanel.selectableComponents().add(Department.NAME));
  }
}
