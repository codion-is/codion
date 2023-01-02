/*
 * Copyright (c) 2011 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void setPanelClass() {
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(Department.TYPE)
            .editPanelClass(EntityEditPanel.class).panelClass(EntityPanel.class));
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(Department.TYPE)
            .tablePanelClass(EntityTablePanel.class).panelClass(EntityPanel.class));

    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(Department.TYPE)
            .panelClass(EntityPanel.class).editPanelClass(EntityEditPanel.class));
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(Department.TYPE)
            .panelClass(EntityPanel.class).tablePanelClass(EntityTablePanel.class));
  }

  @Test
  void testDetailPanelBuilder() {
    SwingEntityModel.Builder customerModelBuilder = SwingEntityModel.builder(Department.TYPE);
    SwingEntityModel.Builder invoiceModelBuilder = SwingEntityModel.builder(Employee.TYPE);

    customerModelBuilder.detailModelBuilder(invoiceModelBuilder);

    SwingEntityModel customerModel = customerModelBuilder.buildModel(CONNECTION_PROVIDER);

    final String customerCaption = "A department caption";
    EntityPanel.Builder customerPanelBuilder = EntityPanel.builder(Department.TYPE)
            .caption(customerCaption);
    EntityPanel.Builder invoicePanelBuilder = EntityPanel.builder(Employee.TYPE)
            .caption("empCaption");

    customerPanelBuilder.detailPanelBuilder(invoicePanelBuilder);

    EntityPanel customerPanel = customerPanelBuilder.buildPanel(customerModel);
    assertEquals(customerCaption, customerPanel.getCaption());
    assertTrue(customerPanel.containsDetailPanel(Employee.TYPE));
    EntityPanel invoicePanel = customerPanel.detailPanel(Employee.TYPE);
    assertEquals("empCaption", invoicePanel.getCaption());
    assertEquals(1, customerPanel.detailPanels().size());

    assertEquals(customerModel, customerPanel.model());
    assertEquals(customerModel.detailModel(Employee.TYPE), invoicePanel.model());
  }
}
