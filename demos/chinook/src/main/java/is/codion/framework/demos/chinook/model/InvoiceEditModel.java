/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ValueChange;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.time.LocalDateTime;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.framework.demos.chinook.domain.Chinook.Invoice;

public final class InvoiceEditModel extends SwingEntityEditModel {

  public InvoiceEditModel(final EntityConnectionProvider connectionProvider) {
    super(Invoice.TYPE, connectionProvider);
    setPersistValue(Invoice.CUSTOMER_FK, false);
    bindEvents();
  }

  @Override
  public <T> T getDefaultValue(final Attribute<T> attribute) {
    if (attribute.equals(Invoice.INVOICEDATE)) {
      return (T) LocalDateTime.now();
    }

    return super.getDefaultValue(attribute);
  }

  private void bindEvents() {
    addValueEditListener(Invoice.CUSTOMER_FK, this::setAddress);
  }

  private void setAddress(final ValueChange valueChange) {
    final Entity customer = (Entity) valueChange.getValue();
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
