/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.ui;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.manual.store.minimal.domain.Store;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;
import is.codion.swing.framework.ui.EntityPanel;

import static is.codion.framework.demos.manual.store.minimal.domain.Store.Address;
import static is.codion.framework.demos.manual.store.minimal.domain.Store.Customer;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS;

public class StoreDemo {

  private static class CustomerEditPanel extends EntityEditPanel {

    private CustomerEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusAttribute(Customer.FIRST_NAME);
      createTextField(Customer.FIRST_NAME).setColumns(12);
      createTextField(Customer.LAST_NAME).setColumns(12);
      createTextField(Customer.EMAIL).setColumns(12);
      createCheckBox(Customer.IS_ACTIVE, IncludeCaption.NO);
      setLayout(gridLayout(2, 2));
      addInputPanel(Customer.FIRST_NAME);
      addInputPanel(Customer.LAST_NAME);
      addInputPanel(Customer.EMAIL);
      addInputPanel(Customer.IS_ACTIVE);
    }
  }

  private static class AddressEditPanel extends EntityEditPanel {

    private AddressEditPanel(SwingEntityEditModel addressEditModel) {
      super(addressEditModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusAttribute(Address.STREET);
      createForeignKeyComboBox(Address.CUSTOMER_FK);
      createTextField(Address.STREET).setColumns(12);
      createTextField(Address.CITY).setColumns(12);
      setLayout(gridLayout(3, 1));
      addInputPanel(Address.CUSTOMER_FK);
      addInputPanel(Address.STREET);
      addInputPanel(Address.CITY);
    }
  }

  public static void main(String[] args) {
    Database database = new H2DatabaseFactory()
            .createDatabase("jdbc:h2:mem:h2db",
                    "src/main/sql/create_schema_minimal.sql");

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(database)
                    .setDomainClassName(Store.class.getName())
                    .setUser(User.parseUser("scott:tiger"));

    SwingEntityModel customerModel =
            new SwingEntityModel(Customer.TYPE, connectionProvider);
    SwingEntityModel addressModel =
            new SwingEntityModel(Address.TYPE, connectionProvider);
    customerModel.addDetailModel(addressModel);

    EntityPanel customerPanel =
            new EntityPanel(customerModel,
                    new CustomerEditPanel(customerModel.getEditModel()));
    EntityPanel addressPanel =
            new EntityPanel(addressModel,
                    new AddressEditPanel(addressModel.getEditModel()));
    customerPanel.addDetailPanel(addressPanel);

    customerPanel.getTablePanel().setConditionPanelVisible(true);
    customerPanel.getTablePanel().getTable().setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
    addressPanel.getTablePanel().getTable().setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);

    customerModel.refresh();
    customerPanel.initializePanel();

    Dialogs.displayInDialog(null, customerPanel, "Customers");

    connectionProvider.close();
  }
}
