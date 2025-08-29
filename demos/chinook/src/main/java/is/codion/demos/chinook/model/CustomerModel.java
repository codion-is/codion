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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.framework.model.SwingEntityModel;

public final class CustomerModel extends SwingEntityModel {

	public CustomerModel(EntityConnectionProvider connectionProvider) {
		super(Customer.TYPE, connectionProvider);
		editModel().initializeComboBoxModels(Customer.SUPPORTREP_FK);
		InvoiceModel invoiceModel = new InvoiceModel(connectionProvider);
		ForeignKeyConditionModel customerConditionModel =
						invoiceModel.tableModel().queryModel().condition().get(Invoice.CUSTOMER_FK);
		customerConditionModel.operands().in().value().link(customerConditionModel.operands().equal());
		detailModels().add(invoiceModel);
	}
}
