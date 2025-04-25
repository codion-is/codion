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
package is.codion.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Collection;

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
		editor().value(Invoice.CUSTOMER_FK).edited().addConsumer(this::setAddress);
	}

	// Override to update the billing address when the invoice customer is changed.
	// This method is called when editing happens outside of the edit model,
	// such as in a table, via an editable table cell or multi item editing
	@Override
	public <T> void applyEdit(Collection<Entity> invoices, Attribute<T> attribute, T value) {
		super.applyEdit(invoices, attribute, value);
		if (attribute.equals(Invoice.CUSTOMER_FK)) {
			Entity customer = (Entity) value;
			invoices.forEach(invoice -> {
				// Set the billing address
				invoice.set(Invoice.BILLINGADDRESS, customer.get(Customer.ADDRESS));
				invoice.set(Invoice.BILLINGCITY, customer.get(Customer.CITY));
				invoice.set(Invoice.BILLINGPOSTALCODE, customer.get(Customer.POSTALCODE));
				invoice.set(Invoice.BILLINGSTATE, customer.get(Customer.STATE));
				invoice.set(Invoice.BILLINGCOUNTRY, customer.get(Customer.COUNTRY));
			});
		}
	}

	private void setAddress(Entity customer) {
		editor().value(Invoice.BILLINGADDRESS).set(customer == null ? null : customer.get(Customer.ADDRESS));
		editor().value(Invoice.BILLINGCITY).set(customer == null ? null : customer.get(Customer.CITY));
		editor().value(Invoice.BILLINGPOSTALCODE).set(customer == null ? null : customer.get(Customer.POSTALCODE));
		editor().value(Invoice.BILLINGSTATE).set(customer == null ? null : customer.get(Customer.STATE));
		editor().value(Invoice.BILLINGCOUNTRY).set(customer == null ? null : customer.get(Customer.COUNTRY));
	}
}
