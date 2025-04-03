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

	// Override to update the billing address when the invoice customer is changed
	@Override
	public <T> void apply(Collection<Entity> entities, Attribute<T> attribute, T newValue) {
		super.apply(entities, attribute, newValue);
		if (attribute.equals(Invoice.CUSTOMER_FK)) {
			Entity customer = (Entity) newValue;
			entities.forEach(invoice -> {
				// Set the billing address
				invoice.put(Invoice.BILLINGADDRESS, customer.get(Customer.ADDRESS));
				invoice.put(Invoice.BILLINGCITY, customer.get(Customer.CITY));
				invoice.put(Invoice.BILLINGPOSTALCODE, customer.get(Customer.POSTALCODE));
				invoice.put(Invoice.BILLINGSTATE, customer.get(Customer.STATE));
				invoice.put(Invoice.BILLINGCOUNTRY, customer.get(Customer.COUNTRY));
			});
		}
	}

	private void setAddress(Entity customer) {
		// We only populate the address fields
		// when we are editing a new invoice
		if (editor().exists().not().get()) {
			if (customer == null) {
				editor().value(Invoice.BILLINGADDRESS).clear();
				editor().value(Invoice.BILLINGCITY).clear();
				editor().value(Invoice.BILLINGPOSTALCODE).clear();
				editor().value(Invoice.BILLINGSTATE).clear();
				editor().value(Invoice.BILLINGCOUNTRY).clear();
			}
			else {
				editor().value(Invoice.BILLINGADDRESS).set(customer.get(Customer.ADDRESS));
				editor().value(Invoice.BILLINGCITY).set(customer.get(Customer.CITY));
				editor().value(Invoice.BILLINGPOSTALCODE).set(customer.get(Customer.POSTALCODE));
				editor().value(Invoice.BILLINGSTATE).set(customer.get(Customer.STATE));
				editor().value(Invoice.BILLINGCOUNTRY).set(customer.get(Customer.COUNTRY));
			}
		}
	}
}
