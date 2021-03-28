/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
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
import java.time.LocalDate;

import static is.codion.common.model.table.SortingDirective.ASCENDING;
import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.framework.demos.chinook.domain.Chinook.Invoice;
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
    setInitialFocusAttribute(Invoice.CUSTOMER_FK);
    final EntityLookupField customerField = createForeignKeyLookupField(Invoice.CUSTOMER_FK);
    configureCustomerLookup(customerField);
    customerField.setColumns(16);
    final TemporalInputPanel<LocalDate> datePanel = createTemporalInputPanel(Invoice.INVOICEDATE);
    datePanel.getInputField().setColumns(12);
    final JTextField addressField = createTextField(Invoice.BILLINGADDRESS);
    selectAllOnFocusGained(addressField);
    addressField.setColumns(16);
    final JTextField cityField = createTextField(Invoice.BILLINGCITY);
    selectAllOnFocusGained(cityField);
    cityField.setColumns(16);
    final JTextField stateField = createTextField(Invoice.BILLINGSTATE);
    selectAllOnFocusGained(stateField);
    stateField.setColumns(16);
    final JTextField countryField = createTextField(Invoice.BILLINGCOUNTRY);
    selectAllOnFocusGained(countryField);
    countryField.setColumns(16);
    final JTextField postalcodeField = createTextField(Invoice.BILLINGPOSTALCODE);
    selectAllOnFocusGained(postalcodeField);
    postalcodeField.setColumns(16);

    final JPanel centerPanel = new JPanel(gridLayout(4, 2));
    centerPanel.add(createInputPanel(Invoice.CUSTOMER_FK));
    centerPanel.add(createInputPanel(Invoice.INVOICEDATE));
    centerPanel.add(createInputPanel(Invoice.BILLINGADDRESS));
    centerPanel.add(createInputPanel(Invoice.BILLINGCITY));
    centerPanel.add(createInputPanel(Invoice.BILLINGSTATE));
    centerPanel.add(createInputPanel(Invoice.BILLINGCOUNTRY));
    centerPanel.add(createInputPanel(Invoice.BILLINGPOSTALCODE));

    final JPanel centerBasePanel = new JPanel(borderLayout());
    centerBasePanel.add(centerPanel, BorderLayout.CENTER);

    invoiceLinePanel.setBorder(BorderFactory.createTitledBorder(getEditModel().getEntities().getDefinition(InvoiceLine.TYPE).getCaption()));

    setLayout(borderLayout());
    add(centerBasePanel, BorderLayout.CENTER);
    add(invoiceLinePanel, BorderLayout.EAST);
  }

  private static void configureCustomerLookup(final EntityLookupField customerField) {
    final EntityLookupField.TableSelectionProvider customerSelectionProvider =
            new EntityLookupField.TableSelectionProvider(customerField.getModel());
    final SwingEntityTableModel tableModel = customerSelectionProvider.getTable().getModel();
    tableModel.getColumnModel().setColumns(Customer.LASTNAME, Customer.FIRSTNAME, Customer.EMAIL);
    tableModel.getSortModel().setSortingDirective(Customer.LASTNAME, ASCENDING);
    customerSelectionProvider.setPreferredSize(new Dimension(500, 300));
    customerField.setSelectionProvider(customerSelectionProvider);
  }
}