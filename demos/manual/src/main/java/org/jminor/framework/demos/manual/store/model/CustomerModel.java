/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.swing.framework.model.SwingEntityModel;

public class CustomerModel extends SwingEntityModel {

  public CustomerModel(EntityConnectionProvider connectionProvider) {
    super(new CustomerEditModel(connectionProvider),
            new CustomerTableModel(connectionProvider));
    bindEvents();
  }

  protected void bindEvents() {
    getTableModel().addRefreshStartedListener(() ->
            System.out.println("Refresh is about to start"));

    getEditModel().addValueListener(Store.CUSTOMER_ADDRESS_FK, valueChange -> {
      System.out.println("Property " + valueChange.getKey() +
              " changed from " + valueChange.getPreviousValue() +
              " to " + valueChange.getCurrentValue());
    });
  }
}
