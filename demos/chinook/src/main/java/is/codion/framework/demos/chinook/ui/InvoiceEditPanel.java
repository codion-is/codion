/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.panel.Panels;
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
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static javax.swing.SortOrder.ASCENDING;

public final class InvoiceEditPanel extends EntityEditPanel {

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
            .selectionProviderFactory(CustomerSelectionProvider::new);
    createTemporalInputPanel(Invoice.DATE);
    createTextField(Invoice.BILLINGADDRESS)
            .selectAllOnFocusGained(true);
    createTextField(Invoice.BILLINGCITY)
            .selectAllOnFocusGained(true);
    createTextField(Invoice.BILLINGSTATE)
            .selectAllOnFocusGained(true);
    createTextField(Invoice.BILLINGCOUNTRY)
            .selectAllOnFocusGained(true);
    createTextField(Invoice.BILLINGPOSTALCODE)
            .selectAllOnFocusGained(true);

    final JPanel centerBasePanel = Panels.builder(borderLayout())
            .add(Panels.builder(gridLayout(4, 2))
                    .add(createInputPanel(Invoice.CUSTOMER_FK)).add(createInputPanel(Invoice.DATE))
                    .add(createInputPanel(Invoice.BILLINGADDRESS)).add(createInputPanel(Invoice.BILLINGCITY))
                    .add(createInputPanel(Invoice.BILLINGSTATE)).add(createInputPanel(Invoice.BILLINGCOUNTRY))
                    .add(createInputPanel(Invoice.BILLINGPOSTALCODE))
                    .build(), BorderLayout.CENTER)
            .build();

    invoiceLinePanel.setBorder(BorderFactory.createTitledBorder(getEditModel().getEntities().getDefinition(InvoiceLine.TYPE).getCaption()));

    setLayout(borderLayout());
    add(centerBasePanel, BorderLayout.CENTER);
    add(invoiceLinePanel, BorderLayout.EAST);
  }

  private static final class CustomerSelectionProvider extends EntitySearchField.TableSelectionProvider {

    private CustomerSelectionProvider(final EntitySearchModel searchModel) {
      super(searchModel);
      final SwingEntityTableModel tableModel = getTable().getModel();
      tableModel.getColumnModel().setColumns(Customer.LASTNAME, Customer.FIRSTNAME, Customer.EMAIL);
      tableModel.getSortModel().setSortOrder(Customer.LASTNAME, ASCENDING);
      setPreferredSize(new Dimension(500, 300));
    }
  }
}