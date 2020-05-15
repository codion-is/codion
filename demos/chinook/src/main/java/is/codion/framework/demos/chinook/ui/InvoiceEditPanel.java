/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityLookupField;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.common.model.table.SortingDirective.ASCENDING;
import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.selectAllOnFocusGained;

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
    final TemporalInputPanel datePanel = createTemporalInputPanel(INVOICE_INVOICEDATE);
    datePanel.getInputField().setColumns(12);
    final JTextField addressField = createTextField(INVOICE_BILLINGADDRESS);
    selectAllOnFocusGained(addressField);
    addressField.setColumns(16);
    final JTextField cityField = createTextField(INVOICE_BILLINGCITY);
    selectAllOnFocusGained(cityField);
    cityField.setColumns(16);
    final JTextField stateField = createTextField(INVOICE_BILLINGSTATE);
    selectAllOnFocusGained(stateField);
    stateField.setColumns(16);
    final JTextField countryField = createTextField(INVOICE_BILLINGCOUNTRY);
    selectAllOnFocusGained(countryField);
    countryField.setColumns(16);
    final JTextField postalcodeField = createTextField(INVOICE_BILLINGPOSTALCODE);
    selectAllOnFocusGained(postalcodeField);
    postalcodeField.setColumns(16);

    final JPanel centerPanel = new JPanel(gridLayout(4, 2));
    centerPanel.add(createPropertyPanel(INVOICE_CUSTOMER_FK));
    centerPanel.add(createPropertyPanel(INVOICE_INVOICEDATE));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGADDRESS));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGCITY));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGSTATE));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGCOUNTRY));
    centerPanel.add(createPropertyPanel(INVOICE_BILLINGPOSTALCODE));

    final JPanel centerBasePanel = new JPanel(borderLayout());
    centerBasePanel.add(centerPanel, BorderLayout.CENTER);

    invoiceLinePanel.setBorder(BorderFactory.createTitledBorder("Invoice lines"));

    setLayout(borderLayout());
    add(centerBasePanel, BorderLayout.CENTER);
    add(invoiceLinePanel, BorderLayout.EAST);
  }

  private void configureCustomerLookup(final EntityLookupField customerField) {
    final EntityLookupField.TableSelectionProvider customerSelectionProvider =
            new EntityLookupField.TableSelectionProvider(customerField.getModel());
    final SwingEntityTableModel tableModel = customerSelectionProvider.getTable().getModel();
    tableModel.setColumns(CUSTOMER_LASTNAME, CUSTOMER_FIRSTNAME, CUSTOMER_EMAIL);
    tableModel.setSortingDirective(CUSTOMER_LASTNAME, ASCENDING);
    customerSelectionProvider.setPreferredSize(new Dimension(500, 300));
    customerField.setSelectionProvider(customerSelectionProvider);
  }
}