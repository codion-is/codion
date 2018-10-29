/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(CUSTOMER_FIRSTNAME);
    final JTextField txtFirstName = createTextField(CUSTOMER_FIRSTNAME);
    txtFirstName.setColumns(16);
    final JTextField txtLastName = createTextField(CUSTOMER_LASTNAME);
    txtLastName.setColumns(16);
    final JTextField txtCompany = createTextField(CUSTOMER_COMPANY);
    txtCompany.setColumns(16);
    final JTextField txtAddress = createTextField(CUSTOMER_ADDRESS);
    txtAddress.setColumns(16);
    final JTextField txtCity = createTextField(CUSTOMER_CITY);
    txtCity.setColumns(16);
    final JTextField txtState = UiUtil.makeUpperCase(createTextField(CUSTOMER_STATE));
    txtState.setColumns(16);
    final JTextField txtCountry = createTextField(CUSTOMER_COUNTRY);
    txtCountry.setColumns(16);
    final JTextField txtPostalcode = createTextField(CUSTOMER_POSTALCODE);
    txtPostalcode.setColumns(16);
    final JTextField txtPhone = createTextField(CUSTOMER_PHONE);
    txtPhone.setColumns(16);
    final JTextField txtFax = createTextField(CUSTOMER_FAX);
    txtFax.setColumns(16);
    final JTextField txtEmail = createTextField(CUSTOMER_EMAIL);
    txtEmail.setColumns(16);
    createForeignKeyComboBox(CUSTOMER_SUPPORTREPID_FK);

    setLayout(new FlexibleGridLayout(3, 4, 5, 5));
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
    addPropertyPanel(CUSTOMER_SUPPORTREPID_FK);
  }
}