/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.swing.ui.DateInputPanel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class InvoiceEditPanel extends EntityEditPanel {

  private EntityPanel invoiceLinePanel;

  public InvoiceEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  public void setInvoiceLinePanel(final EntityPanel invoiceLinePanel) {
    this.invoiceLinePanel = invoiceLinePanel;
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(INVOICE_CUSTOMERID_FK);
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
    final JTextField txtTotal = createTextField(INVOICE_TOTAL_SUB);
    txtTotal.setColumns(16);

    final JPanel propertyBase = new JPanel(new GridLayout(4, 2, 5, 5));
    propertyBase.add(createPropertyPanel(INVOICE_CUSTOMERID_FK));
    propertyBase.add(createPropertyPanel(INVOICE_INVOICEDATE));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGADDRESS));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGCITY));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGSTATE));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGCOUNTRY));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGPOSTALCODE));
    propertyBase.add(createPropertyPanel(INVOICE_TOTAL_SUB));

    final JPanel centerBase = new JPanel(new BorderLayout(5, 5));
    centerBase.add(propertyBase, BorderLayout.CENTER);

    invoiceLinePanel.setBorder(BorderFactory.createTitledBorder("Invoice lines"));

    setLayout(new BorderLayout(5, 5));
    add(centerBase, BorderLayout.CENTER);
    add(invoiceLinePanel, BorderLayout.EAST);
  }
}