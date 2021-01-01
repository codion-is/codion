/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.common.db.reports.Report;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.framework.demos.manual.store.model.CustomerAddressModel;
import is.codion.framework.demos.manual.store.model.CustomerModel;
import is.codion.framework.demos.manual.store.model.StoreAppModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

// tag::storeAppPanel[]
public class StoreAppPanel extends EntityApplicationPanel<StoreAppModel> {

  @Override
  protected List<EntityPanel> initializeEntityPanels(final StoreAppModel applicationModel) {
    CustomerModel customerModel =
            (CustomerModel) applicationModel.getEntityModel(Customer.TYPE);
    //populate model with rows from database
    customerModel.refresh();

    EntityPanel customerPanel = new EntityPanel(customerModel,
            new CustomerEditPanel(customerModel.getEditModel()),
            new CustomerTablePanel(customerModel.getTableModel()));

    CustomerAddressModel customerAddressModel =
            (CustomerAddressModel) customerModel.getDetailModel(CustomerAddress.TYPE);
    EntityPanel customerAddressPanel = new EntityPanel(customerAddressModel,
            new CustomerAddressEditPanel(customerAddressModel.getEditModel()));

    customerPanel.addDetailPanel(customerAddressPanel);

    return Collections.singletonList(customerPanel);
  }

  @Override
  protected void setupEntityPanelBuilders() {
    addSupportPanelBuilder(new EntityPanelBuilder(Address.TYPE)
            .editPanelClass(AddressEditPanel.class));
  }

  @Override
  protected StoreAppModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
    return new StoreAppModel(connectionProvider);
  }

  public static void main(String[] args) {
    Locale.setDefault(new Locale("en", "EN"));
    EntityEditModel.POST_EDIT_EVENTS.set(true);
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityPanel.COMPACT_ENTITY_PANEL_LAYOUT.set(true);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(ColumnConditionModel.AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.manual.store.domain.Store");
    Report.REPORT_PATH.set("http://test.io");
    new StoreAppPanel().startApplication("Store", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.6), Users.parseUser("scott:tiger"));
  }
}
// end::storeAppPanel[]