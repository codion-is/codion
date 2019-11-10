/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.model.table.SortingDirective;
import org.jminor.framework.domain.property.Property;
import org.jminor.swing.common.ui.TemporalInputPanel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityLookupField;
import org.jminor.swing.framework.ui.EntityPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class InvoiceEditPanel extends EntityEditPanel {

  private EntityPanel invoiceLinePanel;

  public InvoiceEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  public void setInvoiceLinePanel(final EntityPanel invoiceLinePanel) {
    this.invoiceLinePanel = invoiceLinePanel;
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(INVOICE_CUSTOMER_FK);
    final EntityLookupField customerField = createForeignKeyLookupField(INVOICE_CUSTOMER_FK);
    configureCustomerLookup(customerField);
    customerField.setColumns(16);
    final TemporalInputPanel datePanel = createDateInputPanel(INVOICE_INVOICEDATE);
    datePanel.getInputField().setColumns(16);
    final JTextField addressField = createTextField(INVOICE_BILLINGADDRESS);
    addressField.setColumns(16);
    final JTextField cityField = createTextField(INVOICE_BILLINGCITY);
    cityField.setColumns(16);
    final JTextField stateField = createTextField(INVOICE_BILLINGSTATE);
    stateField.setColumns(16);
    final JTextField countryField = createTextField(INVOICE_BILLINGCOUNTRY);
    countryField.setColumns(16);
    final JTextField postalcodeField = createTextField(INVOICE_BILLINGPOSTALCODE);
    postalcodeField.setColumns(16);
    final JTextField totalField = createTextField(INVOICE_TOTAL_SUB);
    totalField.setColumns(16);

    final JPanel centerPanel = new JPanel(new GridLayout(4, 2, 5, 5));
    centerPanel.add(createPropertyPanel(INVOICE_CUSTOMER_FK));
    centerPanel.add(createPropertyPanel(INVOICE_INVOICEDATE));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGADDRESS));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGCITY));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGSTATE));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGCOUNTRY));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGPOSTALCODE));
    centerPanel.add(createPropertyPanel(INVOICE_TOTAL_SUB));

    final JPanel centerBasePanel = new JPanel(new BorderLayout(5, 5));
    centerBasePanel.add(centerPanel, BorderLayout.CENTER);

    invoiceLinePanel.setBorder(BorderFactory.createTitledBorder("Invoice lines"));

    setLayout(new BorderLayout(5, 5));
    add(centerBasePanel, BorderLayout.CENTER);
    add(invoiceLinePanel, BorderLayout.EAST);
  }

  private void configureCustomerLookup(final EntityLookupField customerField) {
    final EntityLookupField.TableSelectionProvider customerSelectionProvider = new EntityLookupField.TableSelectionProvider(customerField.getModel());
    final SwingEntityTableModel tableModel = customerSelectionProvider.getEntityTablePanel().getEntityTableModel();
    tableModel.setColumns(CUSTOMER_LASTNAME, CUSTOMER_FIRSTNAME);
    tableModel.getSortModel().setSortingDirective((Property) tableModel.getColumnModel().getColumn(0).getIdentifier(),
              SortingDirective.ASCENDING, false);
    customerField.setSelectionProvider(customerSelectionProvider);
  }
}