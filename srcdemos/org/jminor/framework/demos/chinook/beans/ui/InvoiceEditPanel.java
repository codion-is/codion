/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

public class InvoiceEditPanel extends EntityEditPanel {

  private final EntityPanel invoiceLinePanel;

  public InvoiceEditPanel(final EntityEditModel editModel, final EntityPanel invoiceLinePanel) {
    super(editModel);
    this.invoiceLinePanel = invoiceLinePanel;
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

    final JPanel propertyBase = new JPanel(new GridLayout(4, 2, 5, 5));
    propertyBase.add(createPropertyPanel(INVOICE_CUSTOMERID_FK));
    propertyBase.add(createPropertyPanel(INVOICE_INVOICEDATE));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGADDRESS));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGCITY));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGSTATE));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGCOUNTRY));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGPOSTALCODE));
    propertyBase.add(createPropertyPanel(INVOICE_TOTAL));

    final JPanel centerBase = new JPanel(new BorderLayout(5, 5));
    centerBase.add(propertyBase, BorderLayout.CENTER);

    final Control showLineEditPanelAction = new Control(null, null, Images.loadImage("Add16.gif")) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        invoiceLinePanel.setEditPanelState(invoiceLinePanel.getEditPanelState() == EntityPanel.HIDDEN ?
                EntityPanel.DIALOG : EntityPanel.HIDDEN);
      }
    };
    final JButton btnToggleEditPanel = ControlProvider.createButton(showLineEditPanelAction);
    btnToggleEditPanel.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);

    final JPanel labelButtonPanel = new JPanel(new BorderLayout());
    labelButtonPanel.add(new JLabel("Invoice lines"), BorderLayout.CENTER);
    labelButtonPanel.add(btnToggleEditPanel, BorderLayout.EAST);

    setLayout(new BorderLayout(5, 5));
    add(centerBase, BorderLayout.CENTER);
    add(createPropertyPanel(labelButtonPanel, invoiceLinePanel, true, 0, 2), BorderLayout.EAST);
  }
}