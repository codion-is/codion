/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.Databases;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityModelBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EntityPanelBuilderTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void setPanelClass() {
    assertThrows(IllegalStateException.class, () -> new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .setEditPanelClass(EntityEditPanel.class).setPanelClass(EntityPanel.class));
    assertThrows(IllegalStateException.class, () -> new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .setTablePanelClass(EntityTablePanel.class).setPanelClass(EntityPanel.class));

    assertThrows(IllegalStateException.class, () -> new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .setPanelClass(EntityPanel.class).setEditPanelClass(EntityEditPanel.class));
    assertThrows(IllegalStateException.class, () -> new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .setPanelClass(EntityPanel.class).setTablePanelClass(EntityTablePanel.class));
  }

  @Test
  public void testDetailPanelBuilder() {
    final SwingEntityModelBuilder customerModelBuilder = new SwingEntityModelBuilder(TestDomain.T_DEPARTMENT);
    final SwingEntityModelBuilder invoiceModelBuilder = new SwingEntityModelBuilder(TestDomain.T_EMP);

    customerModelBuilder.addDetailModelBuilder(invoiceModelBuilder);

    final SwingEntityModel customerModel = customerModelBuilder.createModel(CONNECTION_PROVIDER);

    final String customerCaption = "A department caption";
    final EntityPanelBuilder customerPanelBuilder = new EntityPanelBuilder(TestDomain.T_DEPARTMENT)
            .setCaption(customerCaption);
    final EntityPanelBuilder invoicePanelBuilder = new EntityPanelBuilder(TestDomain.T_EMP)
            .setCaption("empCaption");

    customerPanelBuilder.addDetailPanelBuilder(invoicePanelBuilder);

    final EntityPanel customerPanel = customerPanelBuilder.createPanel(customerModel);
    assertEquals(customerCaption, customerPanel.getCaption());
    assertTrue(customerPanel.containsDetailPanel(TestDomain.T_EMP));
    final EntityPanel invoicePanel = customerPanel.getDetailPanel(TestDomain.T_EMP);
    assertEquals("empCaption", invoicePanel.getCaption());

    assertEquals(customerModel, customerPanel.getModel());
    assertEquals(customerModel.getDetailModel(TestDomain.T_EMP), invoicePanel.getModel());
  }
}
