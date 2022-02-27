/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityPanelBuilderTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  void setPanelClass() {
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(TestDomain.T_DEPARTMENT)
            .editPanelClass(EntityEditPanel.class).panelClass(EntityPanel.class));
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(TestDomain.T_DEPARTMENT)
            .tablePanelClass(EntityTablePanel.class).panelClass(EntityPanel.class));

    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(TestDomain.T_DEPARTMENT)
            .panelClass(EntityPanel.class).editPanelClass(EntityEditPanel.class));
    assertThrows(IllegalStateException.class, () -> EntityPanel.builder(TestDomain.T_DEPARTMENT)
            .panelClass(EntityPanel.class).tablePanelClass(EntityTablePanel.class));
  }

  @Test
  void testDetailPanelBuilder() {
    SwingEntityModel.Builder customerModelBuilder = SwingEntityModel.builder(TestDomain.T_DEPARTMENT);
    SwingEntityModel.Builder invoiceModelBuilder = SwingEntityModel.builder(TestDomain.T_EMP);

    customerModelBuilder.detailModelBuilder(invoiceModelBuilder);

    SwingEntityModel customerModel = customerModelBuilder.buildModel(CONNECTION_PROVIDER);

    final String customerCaption = "A department caption";
    EntityPanel.Builder customerPanelBuilder = EntityPanel.builder(TestDomain.T_DEPARTMENT)
            .caption(customerCaption);
    EntityPanel.Builder invoicePanelBuilder = EntityPanel.builder(TestDomain.T_EMP)
            .caption("empCaption");

    customerPanelBuilder.detailPanelBuilder(invoicePanelBuilder);

    EntityPanel customerPanel = customerPanelBuilder.buildPanel(customerModel);
    assertEquals(customerCaption, customerPanel.getCaption());
    assertTrue(customerPanel.containsDetailPanel(TestDomain.T_EMP));
    EntityPanel invoicePanel = customerPanel.getDetailPanel(TestDomain.T_EMP);
    assertEquals("empCaption", invoicePanel.getCaption());
    assertEquals(1, customerPanel.getDetailPanels().size());

    assertEquals(customerModel, customerPanel.getModel());
    assertEquals(customerModel.getDetailModel(TestDomain.T_EMP), invoicePanel.getModel());
  }
}
