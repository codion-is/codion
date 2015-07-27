/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultEntityModelProviderTest {

  public DefaultEntityModelProviderTest() {
    TestDomain.init();
  }

  @Test
  public void testDetailModelProvider() {
    final EntityModelProvider customerModelProvider = new DefaultEntityModelProvider(TestDomain.T_CUSTOMER)
            .setEditModelClass(CustomerEditModel.class)
            .setTableModelClass(CustomerTableModel.class);
    final EntityModelProvider invoiceModelProvider = new DefaultEntityModelProvider(TestDomain.T_INVOICE);
    final EntityModelProvider invoiceLineModelProvider = new DefaultEntityModelProvider(TestDomain.T_INVOICELINE);

    customerModelProvider.addDetailModelProvider(invoiceModelProvider);
    invoiceModelProvider.addDetailModelProvider(invoiceLineModelProvider);

    assertEquals(CustomerEditModel.class, customerModelProvider.getEditModelClass());
    assertEquals(CustomerTableModel.class, customerModelProvider.getTableModelClass());

    final EntityModel customerModel = customerModelProvider.createModel(LocalEntityConnectionTest.CONNECTION_PROVIDER, false);
    assertTrue(customerModel.getEditModel() instanceof CustomerEditModel);
    assertTrue(customerModel.getTableModel() instanceof CustomerTableModel);
    assertTrue(customerModel.containsDetailModel(TestDomain.T_INVOICE));
    final EntityModel invoiceModel = customerModel.getDetailModel(TestDomain.T_INVOICE);
    assertTrue(invoiceModel.containsDetailModel(TestDomain.T_INVOICELINE));
  }

  static final class CustomerEditModel extends DefaultEntityEditModel {

    public CustomerEditModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_CUSTOMER, connectionProvider);
    }
  }

  static final class CustomerTableModel extends DefaultEntityTableModel {

    public CustomerTableModel(final EntityConnectionProvider connectionProvider) {
      super(TestDomain.T_CUSTOMER, connectionProvider);
    }
  }
}
