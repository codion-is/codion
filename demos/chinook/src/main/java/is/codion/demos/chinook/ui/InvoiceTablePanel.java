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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.condition.TableConditionModel;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.ConditionPanel;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityDialogs.EditAttributeDialogBuilder;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.SIMPLE;

public final class InvoiceTablePanel extends EntityTablePanel {

	public InvoiceTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						// The TOTAL column is updated automatically when invoice lines are updated,
						// see InvoiceLineEditModel, so we don't want it to be editable via the popup menu.
						.editable(attributes -> attributes.remove(Invoice.TOTAL))
						// The factory providing our custom condition panel.
						.conditionPanelFactory(new InvoiceConditionPanelFactory(tableModel))
						// Start with the SIMPLE condition panel view.
						.conditionView(SIMPLE));
	}

	// Override to configure the edit dialog when one or more entities are edited via the table popup menu.
	// Here we override to update the billing address when the invoice customer is changed.
	@Override
	protected <T> EditAttributeDialogBuilder<T> editDialogBuilder(Attribute<T> attribute) {
		EditAttributeDialogBuilder<T> builder = super.editDialogBuilder(attribute);
		// If the customer is being edited
		if (attribute.equals(Invoice.CUSTOMER_FK)) {
			// Called to apply the new value to the entities
			// being edited, when the editor value is accepted.
			builder.applier(new ApplyCustomer<>());
		}

		return builder;
	}

	private static final class ApplyCustomer<T> implements BiConsumer<Collection<Entity>, T> {

		@Override
		public void accept(Collection<Entity> invoices, T newValue) {
			Entity customer = (Entity) newValue;
			invoices.forEach(invoice -> {
				// Set the attribute being edited
				invoice.put(Invoice.CUSTOMER_FK, customer);
				// and set the billing address
				invoice.put(Invoice.BILLINGADDRESS, customer.get(Customer.ADDRESS));
				invoice.put(Invoice.BILLINGCITY, customer.get(Customer.CITY));
				invoice.put(Invoice.BILLINGPOSTALCODE, customer.get(Customer.POSTALCODE));
				invoice.put(Invoice.BILLINGSTATE, customer.get(Customer.STATE));
				invoice.put(Invoice.BILLINGCOUNTRY, customer.get(Customer.COUNTRY));
			});
		}
	}

	private static final class InvoiceConditionPanelFactory implements TableConditionPanel.Factory<Attribute<?>> {

		private final SwingEntityTableModel tableModel;

		private InvoiceConditionPanelFactory(SwingEntityTableModel tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public TableConditionPanel<Attribute<?>> create(TableConditionModel<Attribute<?>> tableConditionModel,
																										Map<Attribute<?>, ConditionPanel<?>> conditionPanels,
																										FilterTableColumnModel<Attribute<?>> columnModel,
																										Consumer<TableConditionPanel<Attribute<?>>> onPanelInitialized) {
			return new InvoiceConditionPanel(tableModel, conditionPanels, columnModel, onPanelInitialized);
		}
	}
}
