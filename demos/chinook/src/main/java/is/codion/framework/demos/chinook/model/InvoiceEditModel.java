/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.framework.demos.chinook.domain.Chinook.Invoice;

public final class InvoiceEditModel extends SwingEntityEditModel {

  public InvoiceEditModel(EntityConnectionProvider connectionProvider) {
    super(Invoice.TYPE, connectionProvider);
    persistValue(Invoice.CUSTOMER_FK).set(false);
    bindEvents();
  }

  private void bindEvents() {
    addEditListener(Invoice.CUSTOMER_FK, this::setAddress);
  }

  private void setAddress(Entity customer) {
    if (customer == null) {
      put(Invoice.BILLINGADDRESS, null);
      put(Invoice.BILLINGCITY, null);
      put(Invoice.BILLINGPOSTALCODE, null);
      put(Invoice.BILLINGSTATE, null);
      put(Invoice.BILLINGCOUNTRY, null);
    }
    else {
      put(Invoice.BILLINGADDRESS, customer.get(Customer.ADDRESS));
      put(Invoice.BILLINGCITY, customer.get(Customer.CITY));
      put(Invoice.BILLINGPOSTALCODE, customer.get(Customer.POSTALCODE));
      put(Invoice.BILLINGSTATE, customer.get(Customer.STATE));
      put(Invoice.BILLINGCOUNTRY, customer.get(Customer.COUNTRY));
    }
  }
}
