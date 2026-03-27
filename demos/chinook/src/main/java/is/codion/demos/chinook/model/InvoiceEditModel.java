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
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityEditor.EditorValue;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.jspecify.annotations.Nullable;

import static is.codion.demos.chinook.domain.api.Chinook.Customer;
import static is.codion.demos.chinook.domain.api.Chinook.Invoice;

public final class InvoiceEditModel extends SwingEntityEditModel {

	public InvoiceEditModel(EntityConnectionProvider connectionProvider) {
		super(Invoice.TYPE, connectionProvider);
		EditorValue<Entity> customer = editor().value(Invoice.CUSTOMER_FK);
		// By default, foreign key values persist when the model
		// is cleared, here we disable that for CUSTOMER_FK
		customer.persist().set(false);
		// We populate the invoice address fields with
		// the customer address when the customer is edited
		customer.propagate(Invoice.BILLINGADDRESS, cust -> valueOrNull(cust, Customer.ADDRESS));
		customer.propagate(Invoice.BILLINGCITY, cust -> valueOrNull(cust, Customer.CITY));
		customer.propagate(Invoice.BILLINGPOSTALCODE, cust -> valueOrNull(cust, Customer.POSTALCODE));
		customer.propagate(Invoice.BILLINGSTATE, cust -> valueOrNull(cust, Customer.STATE));
		customer.propagate(Invoice.BILLINGCOUNTRY, cust -> valueOrNull(cust, Customer.COUNTRY));
	}

	private static @Nullable <T> T valueOrNull(Entity customer, Attribute<T> attribute) {
		return customer == null ? null : customer.get(attribute);
	}
}
