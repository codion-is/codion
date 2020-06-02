/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
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
    setInitialFocusAttribute(CUSTOMER_FIRSTNAME);

    createTextField(CUSTOMER_FIRSTNAME).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_LASTNAME).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_COMPANY).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_ADDRESS).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_CITY).setColumns(TEXT_FIELD_COLUMNS);
    TextFields.upperCase(createTextField(CUSTOMER_STATE)).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_COUNTRY).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_POSTALCODE).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_PHONE).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_FAX).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(CUSTOMER_EMAIL).setColumns(TEXT_FIELD_COLUMNS);
    setPreferredHeight(createForeignKeyComboBox(CUSTOMER_SUPPORTREP_FK), getPreferredTextFieldHeight());

    setLayout(flexibleGridLayout(4, 3));
    addPropertyPanel(CUSTOMER_FIRSTNAME);
    addPropertyPanel(CUSTOMER_LASTNAME);
    addPropertyPanel(CUSTOMER_COMPANY);
    addPropertyPanel(CUSTOMER_ADDRESS);
    addPropertyPanel(CUSTOMER_CITY);
    addPropertyPanel(CUSTOMER_STATE);
    addPropertyPanel(CUSTOMER_COUNTRY);
    addPropertyPanel(CUSTOMER_POSTALCODE);
    addPropertyPanel(CUSTOMER_PHONE);
    addPropertyPanel(CUSTOMER_FAX);
    addPropertyPanel(CUSTOMER_EMAIL);
    addPropertyPanel(CUSTOMER_SUPPORTREP_FK);
  }
}