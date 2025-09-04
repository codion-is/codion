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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.component.EntitySearchField;
import is.codion.swing.framework.ui.component.EntitySearchField.Selector;
import is.codion.swing.framework.ui.component.EntitySearchField.TableSelector;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Function;

import static is.codion.demos.chinook.domain.api.Chinook.Customer;
import static is.codion.demos.chinook.domain.api.Chinook.Invoice;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static javax.swing.BorderFactory.createTitledBorder;

public final class InvoiceEditPanel extends EntityEditPanel {

	private final EntityPanel invoiceLinePanel;

	public InvoiceEditPanel(SwingEntityEditModel editModel, SwingEntityModel invoiceLineModel) {
		super(editModel, config ->
						// We want this edit panel to keep displaying a newly inserted invoice,
						// since we will continue to work with it, by adding invoice lines for example
						config.clearAfterInsert(false));
		this.invoiceLinePanel = createInvoiceLinePanel(invoiceLineModel);
	}

	@Override
	protected void initializeUI() {
		focus().initial().set(Invoice.CUSTOMER_FK);

		createSearchField(Invoice.CUSTOMER_FK)
						.columns(14)
						// We add a custom selector factory, creating a selector which
						// displays a table instead of a list when selecting a customer
						.selector(new CustomerSelector());
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

	EntityPanel invoiceLinePanel() {
		return invoiceLinePanel;
	}

	private static EntityPanel createInvoiceLinePanel(SwingEntityModel invoiceLineModel) {
		// Here we construct the InvoiceLine panel, which will
		// be embedded in this edit panel, see initializeUI().
		InvoiceLineTablePanel invoiceLineTablePanel =
						new InvoiceLineTablePanel(invoiceLineModel.tableModel());
		InvoiceLineEditPanel invoiceLineEditPanel =
						new InvoiceLineEditPanel(invoiceLineModel.editModel(),
										invoiceLineTablePanel.table().searchField());

		return new EntityPanel(invoiceLineModel,
						invoiceLineEditPanel, invoiceLineTablePanel, config ->
						// We don't include controls so that no buttons appear on this panel
						config.includeControls(false));
	}

	private static final class CustomerSelector implements Function<EntitySearchField, Selector> {

		@Override
		public Selector apply(EntitySearchField searchField) {
			// We use the TableSelector, provided by EntitySearchField,
			// configuring the visible table columns, the sorting and size
			TableSelector selector = EntitySearchField.tableSelector(searchField);
			selector.table().columnModel().visible().set(Customer.LASTNAME, Customer.FIRSTNAME, Customer.EMAIL);
			selector.table().model().sort().ascending(Customer.LASTNAME, Customer.FIRSTNAME);
			selector.preferredSize(new Dimension(500, 300));

			return selector;
		}
	}
}