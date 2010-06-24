/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.DateInputPanel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.chinook.beans.InvoiceLineModel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class InvoicePanel extends EntityPanel {

  public InvoicePanel(final EntityModel model) {
    super(model);
    addDetailPanel(new InvoiceLinePanel(model.getDetailModel(InvoiceLineModel.class)));
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        final JTextField txtCustomer = createEntityLookupField(INVOICE_CUSTOMERID_FK);
        txtCustomer.setColumns(16);
        setInitialFocusComponent(txtCustomer);
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
        add(createPropertyPanel(INVOICE_CUSTOMERID_FK, txtCustomer));
        add(createPropertyPanel(INVOICE_INVOICEDATE, datePanel));
        add(createPropertyPanel(INVOICE_BILLINGADDRESS, txtAddress));
        add(createPropertyPanel(INVOICE_BILLINGCITY, txtCity));
        add(createPropertyPanel(INVOICE_BILLINGSTATE, txtState));
        add(createPropertyPanel(INVOICE_BILLINGCOUNTRY, txtCountry));
        add(createPropertyPanel(INVOICE_BILLINGPOSTALCODE, txtPostalcode));
        add(createPropertyPanel(INVOICE_TOTAL, txtTotal));
      }
    };
  }
}