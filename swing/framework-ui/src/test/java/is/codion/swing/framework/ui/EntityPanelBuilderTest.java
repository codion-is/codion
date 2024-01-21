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

import static org.junit.jupiter.api.Assertions.*;

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
    SwingEntityModel.Builder customerModelBuilder = SwingEntityModel.builder(Department.TYPE);
    SwingEntityModel.Builder invoiceModelBuilder = SwingEntityModel.builder(Employee.TYPE);

    customerModelBuilder.detailModel(invoiceModelBuilder);

    SwingEntityModel customerModel = customerModelBuilder.build(CONNECTION_PROVIDER);

    final String customerCaption = "A department caption";
    EntityPanel.Builder customerPanelBuilder = EntityPanel.builder(Department.TYPE)
            .caption(customerCaption);
    EntityPanel.Builder invoicePanelBuilder = EntityPanel.builder(Employee.TYPE)
            .caption("empCaption");

    customerPanelBuilder.detailPanel(invoicePanelBuilder);

    EntityPanel customerPanel = customerPanelBuilder.build(customerModel);
    assertEquals(customerCaption, customerPanel.caption().get());
    assertTrue(customerPanel.containsDetailPanel(Employee.TYPE));
    EntityPanel invoicePanel = customerPanel.detailPanel(Employee.TYPE);
    assertEquals("empCaption", invoicePanel.caption().get());
    assertEquals(1, customerPanel.detailPanels().size());

    assertEquals(customerModel, customerPanel.model());
    assertEquals(customerModel.detailModel(Employee.TYPE), invoicePanel.model());
  }
}
