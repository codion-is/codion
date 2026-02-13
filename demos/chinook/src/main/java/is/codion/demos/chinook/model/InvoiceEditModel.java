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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityEditor.EditorValue.Propagator;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.jspecify.annotations.Nullable;

import static is.codion.demos.chinook.domain.api.Chinook.Customer;
import static is.codion.demos.chinook.domain.api.Chinook.Invoice;

public final class InvoiceEditModel extends SwingEntityEditModel {

	public InvoiceEditModel(EntityConnectionProvider connectionProvider) {
		super(Invoice.TYPE, connectionProvider);
		// By default, foreign key values persist when the model
		// is cleared, here we disable that for CUSTOMER_FK
		editor().value(Invoice.CUSTOMER_FK).persist().set(false);
		// We populate the invoice address fields with
		// the customer address when the customer is edited
		editor().value(Invoice.CUSTOMER_FK).propagate(new SetBillingAddress());
	}

	// Implement a Propagator, which updates the billing address when the
	// invoice customer is changed. The value propagation is applied each time
	// editing happens via the underlying editor, or when triggered externally,
	// such as in a table, via an editable table cell or multi item editing
	private static final class SetBillingAddress implements Propagator<Entity> {

		@Override
		public void propagate(@Nullable Entity customer, Setter setter) {
			setter.set(Invoice.BILLINGADDRESS, customer == null ? null : customer.get(Customer.ADDRESS));
			setter.set(Invoice.BILLINGCITY, customer == null ? null : customer.get(Customer.CITY));
			setter.set(Invoice.BILLINGPOSTALCODE, customer == null ? null : customer.get(Customer.POSTALCODE));
			setter.set(Invoice.BILLINGSTATE, customer == null ? null : customer.get(Customer.STATE));
			setter.set(Invoice.BILLINGCOUNTRY, customer == null ? null : customer.get(Customer.COUNTRY));
		}
	}
}
