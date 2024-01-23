/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import java.util.Collection;
import java.util.function.Supplier;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

public final class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Customer.FIRSTNAME);

    createTextField(Customer.FIRSTNAME)
            .columns(6);
    createTextField(Customer.LASTNAME)
            .columns(6);
    createTextField(Customer.EMAIL)
            .columns(12);
    createTextField(Customer.COMPANY)
            .columns(12);
    createTextField(Customer.ADDRESS)
            .columns(12);
    createTextField(Customer.CITY)
            .columns(8);
    createTextField(Customer.POSTALCODE)
            .columns(4);
    createTextField(Customer.STATE)
            .columns(4)
            .upperCase(true)
            .selector(Dialogs.singleSelector(new StateValueSupplier()));
    createTextField(Customer.COUNTRY)
            .columns(8);
    createTextField(Customer.PHONE)
            .columns(12);
    createTextField(Customer.FAX)
            .columns(12);
    createForeignKeyComboBox(Customer.SUPPORTREP_FK)
            .preferredWidth(120);

    JPanel firstLastNamePanel = gridLayoutPanel(1, 2)
            .add(createInputPanel(Customer.FIRSTNAME))
            .add(createInputPanel(Customer.LASTNAME))
            .build();

    JPanel cityPostalCodePanel = flexibleGridLayoutPanel(1, 2)
            .add(createInputPanel(Customer.CITY))
            .add(createInputPanel(Customer.POSTALCODE))
            .build();

    JPanel stateCountryPanel = flexibleGridLayoutPanel(1, 2)
            .add(createInputPanel(Customer.STATE))
            .add(createInputPanel(Customer.COUNTRY))
            .build();

    setLayout(flexibleGridLayout(4, 3));

    add(firstLastNamePanel);
    addInputPanel(Customer.EMAIL);
    addInputPanel(Customer.COMPANY);
    addInputPanel(Customer.ADDRESS);
    add(cityPostalCodePanel);
    add(stateCountryPanel);
    addInputPanel(Customer.PHONE);
    addInputPanel(Customer.FAX);
    addInputPanel(Customer.SUPPORTREP_FK);
  }

  private class StateValueSupplier implements Supplier<Collection<String>> {

    @Override
    public Collection<String> get() {
      try {
        return editModel().connectionProvider().connection().select(Customer.STATE);
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}