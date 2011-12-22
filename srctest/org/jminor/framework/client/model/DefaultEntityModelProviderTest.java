/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.chinook.domain.Chinook;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DefaultEntityModelProviderTest {

  public DefaultEntityModelProviderTest() {
    Chinook.init();
  }

  @Test
  public void testDetailModelProvider() {
    final EntityModelProvider customerModelProvider = new DefaultEntityModelProvider(Chinook.T_CUSTOMER);
    final EntityModelProvider invoiceModelProvider = new DefaultEntityModelProvider(Chinook.T_INVOICE);
    final EntityModelProvider invoiceLineModelProvider = new DefaultEntityModelProvider(Chinook.T_INVOICELINE);

    customerModelProvider.addDetailModelProvider(invoiceModelProvider);
    invoiceModelProvider.addDetailModelProvider(invoiceLineModelProvider);

    final EntityModel customerModel = customerModelProvider.createModel(EntityConnectionImplTest.CONNECTION_PROVIDER, false);
    assertTrue(customerModel.containsDetailModel(Chinook.T_INVOICE));
    final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
    assertTrue(invoiceModel.containsDetailModel(Chinook.T_INVOICELINE));
  }
}
