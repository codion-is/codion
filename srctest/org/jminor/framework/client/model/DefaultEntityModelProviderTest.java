/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultEntityModelProviderTest {

  public DefaultEntityModelProviderTest() {
    Chinook.init();
  }

  @Test
  public void testDetailModelProvider() {
    final EntityModelProvider customerModelProvider = new DefaultEntityModelProvider(Chinook.T_CUSTOMER)
            .setEditModelClass(CustomerEditModel.class)
            .setTableModelClass(CustomerTableModel.class);
    final EntityModelProvider invoiceModelProvider = new DefaultEntityModelProvider(Chinook.T_INVOICE);
    final EntityModelProvider invoiceLineModelProvider = new DefaultEntityModelProvider(Chinook.T_INVOICELINE);

    customerModelProvider.addDetailModelProvider(invoiceModelProvider);
    invoiceModelProvider.addDetailModelProvider(invoiceLineModelProvider);

    assertEquals(CustomerEditModel.class, customerModelProvider.getEditModelClass());
    assertEquals(CustomerTableModel.class, customerModelProvider.getTableModelClass());

    final EntityModel customerModel = customerModelProvider.createModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER, false);
    assertTrue(customerModel.getEditModel() instanceof CustomerEditModel);
    assertTrue(customerModel.getTableModel() instanceof CustomerTableModel);
    assertTrue(customerModel.containsDetailModel(Chinook.T_INVOICE));
    final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
    assertTrue(invoiceModel.containsDetailModel(Chinook.T_INVOICELINE));
  }

  static final class CustomerEditModel extends DefaultEntityEditModel {

    public CustomerEditModel(final EntityConnectionProvider connectionProvider) {
      super(Chinook.T_CUSTOMER, connectionProvider);
    }
  }

  static final class CustomerTableModel extends DefaultEntityTableModel {

    public CustomerTableModel(final EntityConnectionProvider connectionProvider) {
      super(Chinook.T_CUSTOMER, connectionProvider);
    }
  }
}
