/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
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
import is.codion.swing.framework.ui.component.EntitySearchField;
import is.codion.swing.framework.ui.component.EntitySearchField.Selector;
import is.codion.swing.framework.ui.component.EntitySearchField.TableSelector;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Function;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SortOrder.ASCENDING;

public final class InvoiceEditPanel extends EntityEditPanel {

  private final EntityPanel invoiceLinePanel;

  public InvoiceEditPanel(SwingEntityEditModel editModel, EntityPanel invoiceLinePanel) {
    super(editModel);
    this.invoiceLinePanel = invoiceLinePanel;
    clearAfterInsert().set(false);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Invoice.CUSTOMER_FK);

    createForeignKeySearchField(Invoice.CUSTOMER_FK)
            .columns(14)
            .selectorFactory(new CustomerSelectorFactory());
    createTemporalFieldPanel(Invoice.DATE)
            .columns(6);

    createTextField(Invoice.BILLINGADDRESS)
            .columns(12)
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

    invoiceLinePanel.setBorder(createTitledBorder(editModel().entities().definition(InvoiceLine.TYPE).caption()));
    invoiceLinePanel.initialize();

    setLayout(borderLayout());

    add(centerPanel, BorderLayout.CENTER);
    add(invoiceLinePanel, BorderLayout.EAST);
  }

  private static final class CustomerSelectorFactory implements Function<EntitySearchModel, Selector> {

    @Override
    public Selector apply(EntitySearchModel searchModel) {
      TableSelector selector = EntitySearchField.tableSelector(searchModel);
      FilteredTableModel<Entity, Attribute<?>> tableModel = selector.table().getModel();
      tableModel.columnModel().setVisibleColumns(Customer.LASTNAME, Customer.FIRSTNAME, Customer.EMAIL);
      tableModel.sortModel().setSortOrder(Customer.LASTNAME, ASCENDING);
      tableModel.sortModel().addSortOrder(Customer.FIRSTNAME, ASCENDING);
      selector.preferredSize(new Dimension(500, 300));

      return selector;
    }
  }
}