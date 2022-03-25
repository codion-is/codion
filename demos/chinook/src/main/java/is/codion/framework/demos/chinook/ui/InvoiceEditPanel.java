/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntitySearchField;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import static is.codion.swing.common.ui.component.Components.panel;
import static is.codion.swing.common.ui.layout.Layouts.*;
import static javax.swing.SortOrder.ASCENDING;

public final class InvoiceEditPanel extends EntityEditPanel {

  private EntityPanel invoiceLinePanel;

  public InvoiceEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setClearAfterInsert(false);
    setDefaultTextFieldColumns(12);
  }

  public void setInvoiceLinePanel(EntityPanel invoiceLinePanel) {
    this.invoiceLinePanel = invoiceLinePanel;
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Invoice.CUSTOMER_FK);

    createForeignKeySearchField(Invoice.CUSTOMER_FK)
            .columns(18)
            .selectionProviderFactory(CustomerSelectionProvider::new);
    createTemporalInputPanel(Invoice.DATE)
            .columns(6)
            .buttonFocusable(false);

    createTextField(Invoice.BILLINGADDRESS)
            .selectAllOnFocusGained(true);

    createTextField(Invoice.BILLINGCITY)
            .columns(8)
            .selectAllOnFocusGained(true);
    createTextField(Invoice.BILLINGPOSTALCODE)
            .columns(4)
            .selectAllOnFocusGained(true);

    createTextField(Invoice.BILLINGSTATE)
            .columns(4)
            .selectAllOnFocusGained(true);
    createTextField(Invoice.BILLINGCOUNTRY)
            .columns(8)
            .selectAllOnFocusGained(true);

    JPanel customerDatePanel = panel(flexibleGridLayout(1, 2))
            .add(createInputPanel(Invoice.CUSTOMER_FK))
            .add(createInputPanel(Invoice.DATE))
            .build();

    JPanel cityPostalCodePanel = panel(flexibleGridLayout(1, 2))
            .add(createInputPanel(Invoice.BILLINGCITY))
            .add(createInputPanel(Invoice.BILLINGPOSTALCODE))
            .build();

    JPanel stateCountryPanel = panel(flexibleGridLayout(1, 2))
            .add(createInputPanel(Invoice.BILLINGSTATE))
            .add(createInputPanel(Invoice.BILLINGCOUNTRY))
            .build();

    JPanel cityPostalCodeStateCountryPanel = panel(gridLayout(1, 2))
            .add(cityPostalCodePanel)
            .add(stateCountryPanel)
            .build();

    JPanel centerPanel = panel(gridLayout(4, 1))
            .add(customerDatePanel)
            .add(createInputPanel(Invoice.BILLINGADDRESS))
            .add(cityPostalCodeStateCountryPanel)
            .build();

    invoiceLinePanel.setBorder(BorderFactory.createTitledBorder(getEditModel().getEntities().getDefinition(InvoiceLine.TYPE).getCaption()));

    setLayout(borderLayout());

    add(centerPanel, BorderLayout.CENTER);
    add(invoiceLinePanel, BorderLayout.EAST);
  }

  private static final class CustomerSelectionProvider extends EntitySearchField.TableSelectionProvider {

    private CustomerSelectionProvider(EntitySearchModel searchModel) {
      super(searchModel);
      SwingEntityTableModel tableModel = getTable().getModel();
      tableModel.getColumnModel().setColumns(Customer.LASTNAME, Customer.FIRSTNAME, Customer.EMAIL);
      tableModel.getSortModel().setSortOrder(Customer.LASTNAME, ASCENDING);
      setPreferredSize(new Dimension(500, 300));
    }
  }
}