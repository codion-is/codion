/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityModelBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityPanelBuilderTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void setPanelClass() {
    assertThrows(IllegalStateException.class, () -> new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .editPanelClass(EntityEditPanel.class).panelClass(EntityPanel.class));
    assertThrows(IllegalStateException.class, () -> new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .tablePanelClass(EntityTablePanel.class).panelClass(EntityPanel.class));

    assertThrows(IllegalStateException.class, () -> new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .panelClass(EntityPanel.class).editPanelClass(EntityEditPanel.class));
    assertThrows(IllegalStateException.class, () -> new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .panelClass(EntityPanel.class).tablePanelClass(EntityTablePanel.class));
  }

  @Test
  public void testDetailPanelBuilder() {
    final SwingEntityModelBuilder customerModelBuilder = new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT);
    final SwingEntityModelBuilder invoiceModelBuilder = new SwingEntityModelBuilder(TestDomain.T_EMP);

    customerModelBuilder.detailModelBuilder(invoiceModelBuilder);

    final SwingEntityModel customerModel = customerModelBuilder.buildModel(CONNECTION_PROVIDER);

    final String customerCaption = "A department caption";
    final EntityPanelBuilder customerPanelBuilder = new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .caption(customerCaption);
    final EntityPanelBuilder invoicePanelBuilder = new EntityPanelBuilder(TestDomain.T_EMP)
            .caption("empCaption");

    customerPanelBuilder.detailPanelBuilder(invoicePanelBuilder);

    final EntityPanel customerPanel = customerPanelBuilder.buildPanel(customerModel);
    assertEquals(customerCaption, customerPanel.getCaption());
    assertTrue(customerPanel.containsDetailPanel(TestDomain.T_EMP));
    final EntityPanel invoicePanel = customerPanel.getDetailPanel(TestDomain.T_EMP);
    assertEquals("empCaption", invoicePanel.getCaption());

    assertEquals(customerModel, customerPanel.getModel());
    assertEquals(customerModel.getDetailModel(TestDomain.T_EMP), invoicePanel.getModel());
  }
}
