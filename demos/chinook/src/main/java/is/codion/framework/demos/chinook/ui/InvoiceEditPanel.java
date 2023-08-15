/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntitySearchField;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static javax.swing.SortOrder.ASCENDING;

public final class InvoiceEditPanel extends EntityEditPanel {

  private final EntityPanel invoiceLinePanel;

  public InvoiceEditPanel(SwingEntityEditModel editModel, EntityPanel invoiceLinePanel) {
    super(editModel);
    this.invoiceLinePanel = invoiceLinePanel;
    setClearAfterInsert(false);
    setDefaultTextFieldColumns(12);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Invoice.CUSTOMER_FK);

    createForeignKeySearchField(Invoice.CUSTOMER_FK)
            .columns(14)
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

    JPanel customerDatePanel = flexibleGridLayoutPanel(1, 2)
            .add(createInputPanel(Invoice.CUSTOMER_FK))
            .add(createInputPanel(Invoice.DATE))
            .build();

    JPanel cityPostalCodePanel = flexibleGridLayoutPanel(1, 2)
            .add(createInputPanel(Invoice.BILLINGCITY))
            .add(createInputPanel(Invoice.BILLINGPOSTALCODE))
            .build();

    JPanel stateCountryPanel = flexibleGridLayoutPanel(1, 2)
            .add(createInputPanel(Invoice.BILLINGSTATE))
            .add(createInputPanel(Invoice.BILLINGCOUNTRY))
            .build();

    JPanel cityPostalCodeStateCountryPanel = gridLayoutPanel(1, 2)
            .add(cityPostalCodePanel)
            .add(stateCountryPanel)
            .build();

    JPanel centerPanel = gridLayoutPanel(4, 1)
            .add(customerDatePanel)
            .add(createInputPanel(Invoice.BILLINGADDRESS))
            .add(cityPostalCodeStateCountryPanel)
            .build();

    invoiceLinePanel.setBorder(BorderFactory.createTitledBorder(editModel().entities().definition(InvoiceLine.TYPE).caption()));
    invoiceLinePanel.initialize();

    setLayout(borderLayout());

    add(centerPanel, BorderLayout.CENTER);
    add(invoiceLinePanel, BorderLayout.EAST);
  }

  private static final class CustomerSelectionProvider extends EntitySearchField.TableSelectionProvider {

    private CustomerSelectionProvider(EntitySearchModel searchModel) {
      super(searchModel);
      FilteredTableModel<Entity, Attribute<?>> tableModel = table().getModel();
      tableModel.columnModel().setVisibleColumns(Customer.LASTNAME, Customer.FIRSTNAME, Customer.EMAIL);
      tableModel.sortModel().setSortOrder(Customer.LASTNAME, ASCENDING);
      setPreferredSize(new Dimension(500, 300));
    }
  }
}