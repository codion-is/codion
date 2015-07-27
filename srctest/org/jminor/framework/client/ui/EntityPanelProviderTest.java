/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityModelProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityModelProvider;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityPanelProviderTest {

  @Test
  public void testDetailPanelProvider() {
    TestDomain.init();
    final EntityModelProvider customerModelProvider = new DefaultEntityModelProvider(TestDomain.T_CUSTOMER);
    final EntityModelProvider invoiceModelProvider = new DefaultEntityModelProvider(TestDomain.T_INVOICE);
    final EntityModelProvider invoiceLineModelProvider = new DefaultEntityModelProvider(TestDomain.T_INVOICELINE);

    customerModelProvider.addDetailModelProvider(invoiceModelProvider);
    invoiceModelProvider.addDetailModelProvider(invoiceLineModelProvider);

    final EntityModel customerModel = customerModelProvider.createModel(LocalEntityConnectionTest.CONNECTION_PROVIDER, false);

    final String customerCaption = "A customer caption";
    final EntityPanelProvider customerPanelProvider = new EntityPanelProvider(TestDomain.T_CUSTOMER, customerCaption);
    final EntityPanelProvider invoicePanelProvider = new EntityPanelProvider(TestDomain.T_INVOICE);
    final EntityPanelProvider invoiceLinePanelProvider = new EntityPanelProvider(TestDomain.T_INVOICELINE);

    customerPanelProvider.addDetailPanelProvider(invoicePanelProvider);
    invoicePanelProvider.addDetailPanelProvider(invoiceLinePanelProvider);

    final EntityPanel customerPanel = customerPanelProvider.createPanel(customerModel);
    assertEquals(customerCaption, customerPanel.getCaption());
    assertTrue(customerPanel.containsDetailPanel(TestDomain.T_INVOICE));
    final EntityPanel invoicePanel = customerPanel.getDetailPanel(TestDomain.T_INVOICE);
    assertEquals(Entities.getCaption(TestDomain.T_INVOICE), invoicePanel.getCaption());
    assertTrue(invoicePanel.containsDetailPanel(TestDomain.T_INVOICELINE));
    final EntityPanel invoiceLinePanel = invoicePanel.getDetailPanel(TestDomain.T_INVOICELINE);

    assertEquals(customerModel, customerPanel.getModel());
    assertEquals(customerModel.getDetailModel(TestDomain.T_INVOICE), invoicePanel.getModel());
    assertEquals(customerModel.getDetailModel(TestDomain.T_INVOICE).getDetailModel(TestDomain.T_INVOICELINE), invoiceLinePanel.getModel());
  }
}
