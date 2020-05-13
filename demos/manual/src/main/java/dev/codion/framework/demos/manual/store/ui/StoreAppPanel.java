/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.store.ui;

import dev.codion.common.db.reports.ReportWrapper;
import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.manual.store.domain.Store;
import dev.codion.framework.demos.manual.store.model.CustomerAddressModel;
import dev.codion.framework.demos.manual.store.model.CustomerModel;
import dev.codion.framework.demos.manual.store.model.StoreAppModel;
import dev.codion.framework.model.EntityEditModel;
import dev.codion.swing.common.ui.Windows;
import dev.codion.swing.framework.ui.EntityApplicationPanel;
import dev.codion.swing.framework.ui.EntityPanel;
import dev.codion.swing.framework.ui.EntityPanelBuilder;
import dev.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

// tag::storeAppPanel[]
public class StoreAppPanel extends EntityApplicationPanel<StoreAppModel> {

  @Override
  protected List<EntityPanel> initializeEntityPanels(final StoreAppModel applicationModel) {
    CustomerModel customerModel =
            (CustomerModel) applicationModel.getEntityModel(Store.T_CUSTOMER);
    //populate model with rows from database
    customerModel.refresh();

    EntityPanel customerPanel = new EntityPanel(customerModel,
            new CustomerEditPanel(customerModel.getEditModel()),
            new CustomerTablePanel(customerModel.getTableModel()));

    CustomerAddressModel customerAddressModel =
            (CustomerAddressModel) customerModel.getDetailModel(Store.T_CUSTOMER_ADDRESS);
    EntityPanel customerAddressPanel = new EntityPanel(customerAddressModel,
            new CustomerAddressEditPanel(customerAddressModel.getEditModel()));

    customerPanel.addDetailPanel(customerAddressPanel);

    return Collections.singletonList(customerPanel);
  }

  @Override
  protected void setupEntityPanelBuilders() {
    addSupportPanelBuilder(new EntityPanelBuilder(Store.T_ADDRESS)
            .setEditPanelClass(AddressEditPanel.class));
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
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.manual.store.domain.Store");
    ReportWrapper.REPORT_PATH.set("http://test.io");
    new StoreAppPanel().startApplication("Store", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.6), Users.parseUser("scott:tiger"));
  }
}
// end::storeAppPanel[]