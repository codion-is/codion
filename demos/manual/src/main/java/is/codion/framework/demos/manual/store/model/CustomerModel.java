/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::customerModel[]
public class CustomerModel extends SwingEntityModel {

  public CustomerModel(EntityConnectionProvider connectionProvider) {
    super(new CustomerTableModel(connectionProvider));
    bindEvents();
  }

  // tag::bindEvents[]
  private void bindEvents() {
    tableModel().refresher().observer().addDataListener(refreshing -> {
      if (refreshing) {
        System.out.println("Refresh is about to start");
      }
      else {
        System.out.println("Refresh is about to end");
      }
    });

    editModel().addValueListener(Customer.FIRST_NAME, value ->
            System.out.println("First name changed to " + value));
  }
  // end::bindEvents[]
}
// end::customerModel[]