/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.model.ChangeValueMapEditModel;
import org.jminor.common.ui.ChangeValueMapEditPanel;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.InvoiceLineModel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class InvoicePanel extends EntityPanel {

  public InvoicePanel(final EntityModel model) {
    super(model, "Invoices");
  }

  @Override
  protected ChangeValueMapEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        final JTextField txtCustomer = createEntityLookupField(INVOICE_CUSTOMERID_FK);
        txtCustomer.setColumns(16);
        setDefaultFocusComponent(txtCustomer);
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

        setLayout(new FlexibleGridLayout(4, 2, 5, 5));
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

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(InvoiceLineModel.class, InvoiceLinePanel.class));
  }
}