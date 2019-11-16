/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class CustomerEditPanel extends EntityEditPanel {

  public CustomerEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(CUSTOMER_FIRSTNAME);

    createTextField(CUSTOMER_FIRSTNAME).setColumns(16);
    createTextField(CUSTOMER_LASTNAME).setColumns(16);
    createTextField(CUSTOMER_COMPANY).setColumns(16);
    createTextField(CUSTOMER_ADDRESS).setColumns(16);
    createTextField(CUSTOMER_CITY).setColumns(16);
    UiUtil.makeUpperCase(createTextField(CUSTOMER_STATE)).setColumns(16);
    createTextField(CUSTOMER_COUNTRY).setColumns(16);
    createTextField(CUSTOMER_POSTALCODE).setColumns(16);
    createTextField(CUSTOMER_PHONE).setColumns(16);
    createTextField(CUSTOMER_FAX).setColumns(16);
    createTextField(CUSTOMER_EMAIL).setColumns(16);
    createForeignKeyComboBox(CUSTOMER_SUPPORTREP_FK);

    setLayout(new FlexibleGridLayout(4, 3, 5, 5));
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