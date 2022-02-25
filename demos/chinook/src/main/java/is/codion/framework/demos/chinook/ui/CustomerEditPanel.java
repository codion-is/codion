/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.util.Collection;
import java.util.function.Supplier;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

public final class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(12);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Customer.FIRSTNAME);

    createTextField(Customer.FIRSTNAME);
    createTextField(Customer.LASTNAME);
    createTextField(Customer.COMPANY);
    createTextField(Customer.ADDRESS);
    createTextField(Customer.CITY);
    createTextField(Customer.STATE)
            .upperCase(true)
            .lookupDialog(new StateValueSupplier());
    createTextField(Customer.COUNTRY);
    createTextField(Customer.POSTALCODE);
    createTextField(Customer.PHONE);
    createTextField(Customer.FAX);
    createTextField(Customer.EMAIL);
    createForeignKeyComboBox(Customer.SUPPORTREP_FK);

    setLayout(flexibleGridLayout(4, 3));
    addInputPanel(Customer.FIRSTNAME);
    addInputPanel(Customer.LASTNAME);
    addInputPanel(Customer.COMPANY);
    addInputPanel(Customer.ADDRESS);
    addInputPanel(Customer.CITY);
    addInputPanel(Customer.STATE);
    addInputPanel(Customer.COUNTRY);
    addInputPanel(Customer.POSTALCODE);
    addInputPanel(Customer.PHONE);
    addInputPanel(Customer.FAX);
    addInputPanel(Customer.EMAIL);
    addInputPanel(Customer.SUPPORTREP_FK);
  }

  private class StateValueSupplier implements Supplier<Collection<String>> {

    @Override
    public Collection<String> get() {
      try {
        return getEditModel().getConnectionProvider().getConnection().select(Customer.STATE);
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}