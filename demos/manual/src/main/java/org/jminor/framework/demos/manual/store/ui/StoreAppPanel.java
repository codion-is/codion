/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.store.ui;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.manual.store.domain.Store;
import org.jminor.framework.demos.manual.store.model.AddressModel;
import org.jminor.framework.demos.manual.store.model.CustomerModel;
import org.jminor.framework.demos.manual.store.model.StoreAppModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanel;

import java.util.Collections;
import java.util.List;

public class StoreAppPanel extends EntityApplicationPanel<StoreAppModel> {

  @Override
  protected List<EntityPanel> initializeEntityPanels(final StoreAppModel applicationModel) {
    CustomerModel customerModel =
            (CustomerModel) applicationModel.getEntityModel(Store.T_CUSTOMER);
    EntityPanel customerPanel = new EntityPanel(customerModel,
            new CustomerEditPanel(customerModel.getEditModel()),
            new CustomerTablePanel(customerModel.getTableModel()));

    AddressModel addressModel =
            (AddressModel) customerModel.getDetailModel(Store.T_ADDRESS);
    EntityPanel addressPanel = new EntityPanel(addressModel);

    customerPanel.addDetailPanel(addressPanel);

    return Collections.singletonList(customerPanel);
  }

  @Override
  protected StoreAppModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
    return new StoreAppModel(connectionProvider);
  }
}
