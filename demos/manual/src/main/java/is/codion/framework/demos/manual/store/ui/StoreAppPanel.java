/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.common.db.report.Report;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store.Address;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.demos.manual.store.domain.Store.CustomerAddress;
import is.codion.framework.demos.manual.store.model.CustomerAddressModel;
import is.codion.framework.demos.manual.store.model.CustomerModel;
import is.codion.framework.demos.manual.store.model.StoreAppModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import javax.swing.SwingUtilities;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// tag::storeAppPanel[]
public class StoreAppPanel extends EntityApplicationPanel<StoreAppModel> {

  public StoreAppPanel() {
    super("Store");
  }

  @Override
  protected List<EntityPanel> createEntityPanels(StoreAppModel applicationModel) {
    CustomerModel customerModel =
            (CustomerModel) applicationModel.entityModel(Customer.TYPE);
    //populate model with rows from database
    customerModel.tableModel().refresh();

    EntityPanel customerPanel = new EntityPanel(customerModel,
            new CustomerEditPanel(customerModel.editModel()),
            new CustomerTablePanel(customerModel.tableModel()));

    CustomerAddressModel customerAddressModel =
            (CustomerAddressModel) customerModel.detailModel(CustomerAddress.TYPE);
    EntityPanel customerAddressPanel = new EntityPanel(customerAddressModel,
            new CustomerAddressEditPanel(customerAddressModel.editModel()));

    customerPanel.addDetailPanel(customerAddressPanel);

    return Collections.singletonList(customerPanel);
  }

  // tag::createSupportEntityPanelBuilders[]
  @Override
  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders(StoreAppModel applicationModel) {
    EntityPanel.Builder addressPanelBuilder =
            EntityPanel.builder(Address.TYPE)
                    .editPanelClass(AddressEditPanel.class);

    return Collections.singletonList(addressPanelBuilder);
  }
  // end::createSupportEntityPanelBuilders[]

  @Override
  protected StoreAppModel createApplicationModel(EntityConnectionProvider connectionProvider) {
    return new StoreAppModel(connectionProvider);
  }

  public static void main(String[] args) {
    Locale.setDefault(new Locale("en", "EN"));
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
            .set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.manual.store.domain.Store");
    Report.REPORT_PATH.set("http://test.io");
    SwingUtilities.invokeLater(() -> new StoreAppPanel().starter()
            .frameSize(Windows.screenSizeRatio(0.6))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start());
  }
}
// end::storeAppPanel[]