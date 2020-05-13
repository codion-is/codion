/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.swing.framework.model.SwingEntityModel;

// tag::customerModel[]
public class CustomerModel extends SwingEntityModel {

  public CustomerModel(EntityConnectionProvider connectionProvider) {
    super(new CustomerEditModel(connectionProvider),
            new CustomerTableModel(connectionProvider));
    bindEvents();
  }

  // tag::bindEvents[]
  private void bindEvents() {
    getTableModel().addRefreshStartedListener(() ->
            System.out.println("Refresh is about to start"));

    getEditModel().addValueListener(Store.CUSTOMER_FIRST_NAME, valueChange ->
            System.out.println("Property " + valueChange.getProperty() +
                    " changed from " + valueChange.getPreviousValue() +
                    " to " + valueChange.getValue()));
  }
  // end::bindEvents[]
}
// end::customerModel[]