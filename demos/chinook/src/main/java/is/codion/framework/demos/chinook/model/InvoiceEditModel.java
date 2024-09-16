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
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;

import static is.codion.framework.demos.chinook.domain.Chinook.Customer;
import static is.codion.framework.demos.chinook.domain.Chinook.Invoice;

public final class InvoiceEditModel extends SwingEntityEditModel {

	public InvoiceEditModel(EntityConnectionProvider connectionProvider) {
		super(Invoice.TYPE, connectionProvider);
		persist(Invoice.CUSTOMER_FK).set(false);
		edited(Invoice.CUSTOMER_FK).addConsumer(this::setAddress);
	}

	private void setAddress(Entity customer) {
		if (exists().not().get()) {
			if (customer == null) {
				value(Invoice.BILLINGADDRESS).clear();
				value(Invoice.BILLINGCITY).clear();
				value(Invoice.BILLINGPOSTALCODE).clear();
				value(Invoice.BILLINGSTATE).clear();
				value(Invoice.BILLINGCOUNTRY).clear();
			}
			else {
				value(Invoice.BILLINGADDRESS).set(customer.get(Customer.ADDRESS));
				value(Invoice.BILLINGCITY).set(customer.get(Customer.CITY));
				value(Invoice.BILLINGPOSTALCODE).set(customer.get(Customer.POSTALCODE));
				value(Invoice.BILLINGSTATE).set(customer.get(Customer.STATE));
				value(Invoice.BILLINGCOUNTRY).set(customer.get(Customer.COUNTRY));
			}
		}
	}
}
