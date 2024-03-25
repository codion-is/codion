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
 * Copyright (c) 2011 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityPanelBuilderTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void setPanelClass() {
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(Department.TYPE)
            .editPanel(EntityEditPanel.class).panel(EntityPanel.class));
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(Department.TYPE)
            .tablePanel(EntityTablePanel.class).panel(EntityPanel.class));

    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(Department.TYPE)
            .panel(EntityPanel.class).editPanel(EntityEditPanel.class));
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(Department.TYPE)
            .panel(EntityPanel.class).tablePanel(EntityTablePanel.class));
  }

  @Test
  void testDetailPanelBuilder() {
    SwingEntityModel.Builder departmentModelBuilder = SwingEntityModel.builder(Department.TYPE);
    SwingEntityModel.Builder employeeModelBuilder = SwingEntityModel.builder(Employee.TYPE);

    departmentModelBuilder.detailModel(employeeModelBuilder);

    SwingEntityModel departmentModel = departmentModelBuilder.build(CONNECTION_PROVIDER);

    String departmentCaption = "A department caption";
    EntityPanel.Builder departmentPanelBuilder = EntityPanel.builder(Department.TYPE)
            .caption(departmentCaption);
    EntityPanel.Builder employeePanelBuilder = EntityPanel.builder(Employee.TYPE)
            .caption("empCaption");

    departmentPanelBuilder.detailPanel(employeePanelBuilder);

    EntityPanel departmentPanel = departmentPanelBuilder.build(departmentModel);
    assertEquals(departmentCaption, departmentPanel.caption().get());
    assertThrows(IllegalArgumentException.class, () -> departmentPanel.detailPanel(Department.TYPE));
    EntityPanel employeePanel = departmentPanel.detailPanel(Employee.TYPE);
    assertEquals("empCaption", employeePanel.caption().get());
    assertEquals(1, departmentPanel.detailPanels().size());

    assertEquals(departmentModel, departmentPanel.model());
    assertEquals(departmentModel.detailModel(Employee.TYPE), employeePanel.model());
  }
}
