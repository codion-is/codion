/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityModelProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityPanelProviderTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void testDetailPanelProvider() {
    final SwingEntityModelProvider customerModelProvider = new SwingEntityModelProvider(TestDomain.T_DEPARTMENT);
    final SwingEntityModelProvider invoiceModelProvider = new SwingEntityModelProvider(TestDomain.T_EMP);

    customerModelProvider.addDetailModelProvider(invoiceModelProvider);

    final SwingEntityModel customerModel = customerModelProvider.createModel(CONNECTION_PROVIDER);

    final String customerCaption = "A department caption";
    final EntityPanelProvider customerPanelProvider = new EntityPanelProvider(TestDomain.T_DEPARTMENT, customerCaption);
    final EntityPanelProvider invoicePanelProvider = new EntityPanelProvider(TestDomain.T_EMP, "empCaption");

    customerPanelProvider.addDetailPanelProvider(invoicePanelProvider);

    final EntityPanel customerPanel = customerPanelProvider.createPanel(customerModel);
    assertEquals(customerCaption, customerPanel.getCaption());
    assertTrue(customerPanel.containsDetailPanel(TestDomain.T_EMP));
    final EntityPanel invoicePanel = customerPanel.getDetailPanel(TestDomain.T_EMP);
    assertEquals("empCaption", invoicePanel.getCaption());

    assertEquals(customerModel, customerPanel.getModel());
    assertEquals(customerModel.getDetailModel(TestDomain.T_EMP), invoicePanel.getModel());
  }
}
