/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;

public class CustomerEditPanel extends EntityEditPanel {

  public static final int TEXT_FIELD_COLUMNS = 12;

  public CustomerEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Customer.FIRSTNAME);

    textField(Customer.FIRSTNAME)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.LASTNAME)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.COMPANY)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.ADDRESS)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.CITY)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.STATE)
            .columns(TEXT_FIELD_COLUMNS)
            .upperCase()
            .build();
    textField(Customer.COUNTRY)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.POSTALCODE)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.PHONE)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.FAX)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    textField(Customer.EMAIL)
            .columns(TEXT_FIELD_COLUMNS)
            .build();
    foreignKeyComboBox(Customer.SUPPORTREP_FK)
            .preferredHeight(getPreferredTextFieldHeight())
            .build();

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
}