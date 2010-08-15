/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.DateInputPanel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JTextField;
import java.awt.GridLayout;

public class InvoiceEditPanel extends EntityEditPanel {

  public InvoiceEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(INVOICE_CUSTOMERID_FK);
    final JTextField txtCustomer = createEntityLookupField(INVOICE_CUSTOMERID_FK);
    txtCustomer.setColumns(16);
    final DateInputPanel datePanel = createDateInputPanel(INVOICE_INVOICEDATE);
    datePanel.getInputField().setColumns(16);
    final JTextField txtAddress = createTextField(INVOICE_BILLINGADDRESS);
    txtAddress.setColumns(16);
    final JTextField txtCity = createTextField(INVOICE_BILLINGCITY);
    txtCity.setColumns(16);
    final JTextField txtState = createTextField(INVOICE_BILLINGSTATE);
    txtState.setColumns(16);
    final JTextField txtCountry = createTextField(INVOICE_BILLINGCOUNTRY);
    txtCountry.setColumns(16);
    final JTextField txtPostalcode = createTextField(INVOICE_BILLINGPOSTALCODE);
    txtPostalcode.setColumns(16);
    final JTextField txtTotal = createTextField(INVOICE_TOTAL);
    txtTotal.setColumns(16);

    setLayout(new GridLayout(4, 2, 5, 5));
    addPropertyPanel(INVOICE_CUSTOMERID_FK);
    addPropertyPanel(INVOICE_INVOICEDATE);
    addPropertyPanel(INVOICE_BILLINGADDRESS);
    addPropertyPanel(INVOICE_BILLINGCITY);
    addPropertyPanel(INVOICE_BILLINGSTATE);
    addPropertyPanel(INVOICE_BILLINGCOUNTRY);
    addPropertyPanel(INVOICE_BILLINGPOSTALCODE);
    addPropertyPanel(INVOICE_TOTAL);
  }
}