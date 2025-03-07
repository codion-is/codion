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
package is.codion.manual.store.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.manual.store.domain.Store.Customer;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::customerModel[]
public class CustomerModel extends SwingEntityModel {

	public CustomerModel(EntityConnectionProvider connectionProvider) {
		super(new CustomerTableModel(connectionProvider));
		bindEvents();
	}

	// tag::bindEvents[]
	private void bindEvents() {
		CustomerTableModel tableModel = (CustomerTableModel) tableModel();

		tableModel.selection().items()
						.addConsumer(selected ->
										System.out.println("Items selected: " + selected));

		tableModel.items().refresher().result()
						.addListener(() -> System.out.println("Refresh successful"));

		CustomerEditModel editModel = (CustomerEditModel) editModel();

		editModel.afterInsert()
						.addConsumer(inserted ->
														System.out.println("Entities inserted" + inserted));

		editModel.editor().value(Customer.FIRST_NAME).edited()
						.addConsumer(firstName ->
										System.out.println("First name changed to " + firstName));
	}
	// end::bindEvents[]
}
// end::customerModel[]