/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.common.user.User;
import is.codion.framework.demos.manual.store.domain.Store;
import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.framework.demos.manual.store.model.CustomerAddressModel;
import is.codion.framework.demos.manual.store.model.CustomerModel;
import is.codion.framework.demos.manual.store.model.StoreApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

// tag::storeAppPanel[]
public class StoreApplicationPanel extends EntityApplicationPanel<StoreApplicationModel> {

  public StoreApplicationPanel(StoreApplicationModel applicationModel) {
    super(applicationModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels() {
    CustomerModel customerModel =
            applicationModel().entityModel(Customer.TYPE);
    CustomerAddressModel customerAddressModel =
            customerModel.detailModel(CustomerAddress.TYPE);

    EntityPanel customerPanel = new EntityPanel(customerModel,
            new CustomerEditPanel(customerModel.editModel()),
            new CustomerTablePanel(customerModel.tableModel()));
    EntityPanel customerAddressPanel = new EntityPanel(customerAddressModel,
            new CustomerAddressEditPanel(customerAddressModel.editModel()));

    customerPanel.addDetailPanel(customerAddressPanel);

    return Collections.singletonList(customerPanel);
  }

  // tag::createSupportEntityPanelBuilders[]
  @Override
  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders() {
    EntityPanel.Builder addressPanelBuilder =
            EntityPanel.builder(Address.TYPE)
                    .editPanel(AddressEditPanel.class);

    return Collections.singletonList(addressPanelBuilder);
  }
  // end::createSupportEntityPanelBuilders[]

  public static void main(String[] args) {
    Locale.setDefault(new Locale("en", "EN"));
    EntityPanel.Config.TOOLBAR_CONTROLS.set(true);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
            .set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
    EntityApplicationPanel.builder(StoreApplicationModel.class, StoreApplicationPanel.class)
            .applicationName("Store")
            .domainType(Store.DOMAIN)
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
}
// end::storeAppPanel[]