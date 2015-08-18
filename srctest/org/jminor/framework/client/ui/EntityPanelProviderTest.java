/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityModelProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityModelProvider;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityPanelProviderTest {

  @Test
  public void testDetailPanelProvider() {
    TestDomain.init();
    final EntityModelProvider customerModelProvider = new DefaultEntityModelProvider(TestDomain.T_DEPARTMENT);
    final EntityModelProvider invoiceModelProvider = new DefaultEntityModelProvider(TestDomain.T_EMP);

    customerModelProvider.addDetailModelProvider(invoiceModelProvider);

    final EntityModel customerModel = customerModelProvider.createModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER, false);

    final String customerCaption = "A department caption";
    final EntityPanelProvider customerPanelProvider = new EntityPanelProvider(TestDomain.T_DEPARTMENT, customerCaption);
    final EntityPanelProvider invoicePanelProvider = new EntityPanelProvider(TestDomain.T_EMP);

    customerPanelProvider.addDetailPanelProvider(invoicePanelProvider);

    final EntityPanel customerPanel = customerPanelProvider.createPanel(customerModel);
    assertEquals(customerCaption, customerPanel.getCaption());
    assertTrue(customerPanel.containsDetailPanel(TestDomain.T_EMP));
    final EntityPanel invoicePanel = customerPanel.getDetailPanel(TestDomain.T_EMP);
    assertEquals(Entities.getCaption(TestDomain.T_EMP), invoicePanel.getCaption());

    assertEquals(customerModel, customerPanel.getModel());
    assertEquals(customerModel.getDetailModel(TestDomain.T_EMP), invoicePanel.getModel());
  }
}
