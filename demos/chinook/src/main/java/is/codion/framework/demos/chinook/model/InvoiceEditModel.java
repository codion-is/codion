/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ValueChange;
import is.codion.framework.domain.property.Property;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.time.LocalDateTime;

import static is.codion.framework.demos.chinook.domain.Chinook.*;

public final class InvoiceEditModel extends SwingEntityEditModel {

  public InvoiceEditModel(final EntityConnectionProvider connectionProvider) {
    super(T_INVOICE, connectionProvider);
    setPersistValue(INVOICE_CUSTOMER_FK, false);
    bindEvents();
  }

  @Override
  public Object getDefaultValue(final Property property) {
    if (property.is(INVOICE_INVOICEDATE)) {
      return LocalDateTime.now();
    }

    return super.getDefaultValue(property);
  }

  private void bindEvents() {
    addValueEditListener(INVOICE_CUSTOMER_FK, this::setAddress);
  }

  private void setAddress(final ValueChange valueChange) {
    final Entity customer = (Entity) valueChange.getValue();
    if (customer == null) {
      put(INVOICE_BILLINGADDRESS, null);
      put(INVOICE_BILLINGCITY, null);
      put(INVOICE_BILLINGPOSTALCODE, null);
      put(INVOICE_BILLINGSTATE, null);
      put(INVOICE_BILLINGCOUNTRY, null);
    }
    else {
      put(INVOICE_BILLINGADDRESS, customer.get(CUSTOMER_ADDRESS));
      put(INVOICE_BILLINGCITY, customer.get(CUSTOMER_CITY));
      put(INVOICE_BILLINGPOSTALCODE, customer.get(CUSTOMER_POSTALCODE));
      put(INVOICE_BILLINGSTATE, customer.get(CUSTOMER_STATE));
      put(INVOICE_BILLINGCOUNTRY, customer.get(CUSTOMER_COUNTRY));
    }
  }
}
