/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.store.minimal.ui;

import is.codion.common.db.database.Database;
import is.codion.common.user.Users;
import is.codion.dbms.h2database.H2DatabaseProvider;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.manual.store.minimal.domain.Store;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;
import is.codion.swing.framework.ui.EntityPanel;

import static is.codion.framework.demos.manual.store.minimal.domain.Store.*;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS;

public class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(CUSTOMER_FIRST_NAME);
    createTextField(CUSTOMER_FIRST_NAME).setColumns(12);
    createTextField(CUSTOMER_LAST_NAME).setColumns(12);
    createTextField(CUSTOMER_EMAIL).setColumns(12);
    createCheckBox(CUSTOMER_IS_ACTIVE, null, IncludeCaption.NO);
    setLayout(gridLayout(2, 2));
    addPropertyPanel(CUSTOMER_FIRST_NAME);
    addPropertyPanel(CUSTOMER_LAST_NAME);
    addPropertyPanel(CUSTOMER_EMAIL);
    addPropertyPanel(CUSTOMER_IS_ACTIVE);
  }

  public static void main(String[] args) {
    Database database = new H2DatabaseProvider()
            .createDatabase("jdbc:h2:mem:h2db",
                    "src/main/sql/create_schema_minimal.sql");

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(database)
                    .setDomainClassName(Store.class.getName())
                    .setUser(Users.parseUser("scott:tiger"));

    SwingEntityModel customerModel =
            new SwingEntityModel(T_CUSTOMER, connectionProvider);
    EntityPanel customerPanel =
            new EntityPanel(customerModel,
                    new CustomerEditPanel(customerModel.getEditModel()));

    customerPanel.getTablePanel().getTable().setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
    customerPanel.getTablePanel().setConditionPanelVisible(true);

    customerModel.refresh();
    customerPanel.initializePanel();

    Dialogs.displayInDialog(null, customerPanel, "Customers");

    connectionProvider.disconnect();
  }
}
