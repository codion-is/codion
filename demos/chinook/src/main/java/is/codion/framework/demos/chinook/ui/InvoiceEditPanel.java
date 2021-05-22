/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntitySearchField;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.common.model.table.TableSortModel.SortingDirective.ASCENDING;
import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public class InvoiceEditPanel extends EntityEditPanel {

  private EntityPanel invoiceLinePanel;

  public InvoiceEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(16);
  }

  public void setInvoiceLinePanel(final EntityPanel invoiceLinePanel) {
    this.invoiceLinePanel = invoiceLinePanel;
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Invoice.CUSTOMER_FK);
    createForeignKeySearchField(Invoice.CUSTOMER_FK)
            .addBuildListener(InvoiceEditPanel::configureCustomerSearch);
    createTemporalInputPanel(Invoice.DATE);
    createTextField(Invoice.BILLINGADDRESS)
            .selectAllOnFocusGained();
    createTextField(Invoice.BILLINGCITY)
            .selectAllOnFocusGained();
    createTextField(Invoice.BILLINGSTATE)
            .selectAllOnFocusGained();
    createTextField(Invoice.BILLINGCOUNTRY)
            .selectAllOnFocusGained();
    createTextField(Invoice.BILLINGPOSTALCODE)
            .selectAllOnFocusGained();

    final JPanel centerPanel = new JPanel(gridLayout(4, 2));
    centerPanel.add(createInputPanel(Invoice.CUSTOMER_FK));
    centerPanel.add(createInputPanel(Invoice.DATE));
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

  private static void configureCustomerSearch(final EntitySearchField customerField) {
    final EntitySearchField.TableSelectionProvider customerSelectionProvider =
            new EntitySearchField.TableSelectionProvider(customerField.getModel());
    final SwingEntityTableModel tableModel = customerSelectionProvider.getTable().getModel();
    tableModel.getColumnModel().setColumns(Customer.LASTNAME, Customer.FIRSTNAME, Customer.EMAIL);
    tableModel.getSortModel().setSortingDirective(Customer.LASTNAME, ASCENDING);
    customerSelectionProvider.setPreferredSize(new Dimension(500, 300));
    customerField.setSelectionProvider(customerSelectionProvider);
  }
}