/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityModelProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityModelProvider;
import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entities;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityPanelProviderTest {

  @Test
  public void testDetailPanelProvider() {
    Chinook.init();
    final EntityModelProvider customerModelProvider = new DefaultEntityModelProvider(Chinook.T_CUSTOMER);
    final EntityModelProvider invoiceModelProvider = new DefaultEntityModelProvider(Chinook.T_INVOICE);
    final EntityModelProvider invoiceLineModelProvider = new DefaultEntityModelProvider(Chinook.T_INVOICELINE);

    customerModelProvider.addDetailModelProvider(invoiceModelProvider);
    invoiceModelProvider.addDetailModelProvider(invoiceLineModelProvider);

    final EntityModel customerModel = customerModelProvider.createModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER, false);

    final String customerCaption = "A customer caption";
    final EntityPanelProvider customerPanelProvider = new EntityPanelProvider(Chinook.T_CUSTOMER, customerCaption);
    final EntityPanelProvider invoicePanelProvider = new EntityPanelProvider(Chinook.T_INVOICE);
    final EntityPanelProvider invoiceLinePanelProvider = new EntityPanelProvider(Chinook.T_INVOICELINE);

    customerPanelProvider.addDetailPanelProvider(invoicePanelProvider);
    invoicePanelProvider.addDetailPanelProvider(invoiceLinePanelProvider);

    final EntityPanel customerPanel = customerPanelProvider.createPanel(customerModel);
    assertEquals(customerCaption, customerPanel.getCaption());
    assertTrue(customerPanel.containsDetailPanel(Chinook.T_INVOICE));
    final EntityPanel invoicePanel = customerPanel.getDetailPanel(Chinook.T_INVOICE);
    assertEquals(Entities.getCaption(Chinook.T_INVOICE), invoicePanel.getCaption());
    assertTrue(invoicePanel.containsDetailPanel(Chinook.T_INVOICELINE));
    final EntityPanel invoiceLinePanel = invoicePanel.getDetailPanel(Chinook.T_INVOICELINE);

    assertEquals(customerModel, customerPanel.getModel());
    assertEquals(customerModel.getDetailModel(Chinook.T_INVOICE), invoicePanel.getModel());
    assertEquals(customerModel.getDetailModel(Chinook.T_INVOICE).getDetailModel(Chinook.T_INVOICELINE), invoiceLinePanel.getModel());
  }
}
