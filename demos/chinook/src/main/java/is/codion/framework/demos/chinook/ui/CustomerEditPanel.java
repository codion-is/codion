/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.swing.common.ui.Components.setPreferredHeight;
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

    createTextField(Customer.FIRSTNAME).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.LASTNAME).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.COMPANY).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.ADDRESS).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.CITY).setColumns(TEXT_FIELD_COLUMNS);
    TextFields.upperCase(createTextField(Customer.STATE)).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.COUNTRY).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.POSTALCODE).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.PHONE).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.FAX).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(Customer.EMAIL).setColumns(TEXT_FIELD_COLUMNS);
    setPreferredHeight(createForeignKeyComboBox(Customer.SUPPORTREP_FK), getPreferredTextFieldHeight());

    setLayout(flexibleGridLayout(4, 3));
    addPropertyPanel(Customer.FIRSTNAME);
    addPropertyPanel(Customer.LASTNAME);
    addPropertyPanel(Customer.COMPANY);
    addPropertyPanel(Customer.ADDRESS);
    addPropertyPanel(Customer.CITY);
    addPropertyPanel(Customer.STATE);
    addPropertyPanel(Customer.COUNTRY);
    addPropertyPanel(Customer.POSTALCODE);
    addPropertyPanel(Customer.PHONE);
    addPropertyPanel(Customer.FAX);
    addPropertyPanel(Customer.EMAIL);
    addPropertyPanel(Customer.SUPPORTREP_FK);
  }
}