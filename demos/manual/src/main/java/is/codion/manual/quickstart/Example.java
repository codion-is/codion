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
package is.codion.manual.quickstart;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import java.util.List;

import static is.codion.manual.quickstart.Store.*;

public final class Example {

	// tag::customerEditPanel[]
	public class CustomerEditPanel extends EntityEditPanel {

		public CustomerEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			focus().initial().set(Customer.FIRST_NAME);
			createTextField(Customer.FIRST_NAME);
			createTextField(Customer.LAST_NAME);
			addInputPanel(Customer.FIRST_NAME);
			addInputPanel(Customer.LAST_NAME);
		}
	}
	// end::customerEditPanel[]

	// tag::addressEditPanel[]
	public class AddressEditPanel extends EntityEditPanel {

		public AddressEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			focus().initial().set(CustomerAddress.CUSTOMER_FK);
			createComboBox(CustomerAddress.CUSTOMER_FK);
			createComboBox(CustomerAddress.ADDRESS_FK);
			addInputPanel(CustomerAddress.CUSTOMER_FK);
			addInputPanel(CustomerAddress.ADDRESS_FK);
		}
	}
	// end::addressEditPanel[]

	void customerPanel() {
		// tag::customerPanel[]
		EntityConnectionProvider connectionProvider =
						LocalEntityConnectionProvider.builder()
										.domain(new Store())
										.user(User.parse("scott:tiger"))
										.build();

		SwingEntityModel customerModel =
						new SwingEntityModel(Customer.TYPE, connectionProvider);

		CustomerEditPanel customerEditPanel =
						new CustomerEditPanel(customerModel.editModel());

		EntityPanel customerPanel =
						new EntityPanel(customerModel, customerEditPanel);
		// end::customerPanel[]

		// tag::detailPanel[]
		SwingEntityModel addressModel =
						new SwingEntityModel(CustomerAddress.TYPE, connectionProvider);

		customerModel.detailModels().add(addressModel);

		AddressEditPanel addressEditPanel =
						new AddressEditPanel(addressModel.editModel());

		EntityPanel addressPanel =
						new EntityPanel(addressModel, addressEditPanel);

		customerPanel.detailPanels().add(addressPanel);

		//lazy initialization of UI components
		customerPanel.initialize();

		//populate the table model with data from the database
		customerModel.tableModel().items().refresh();

		Dialogs.componentDialog(customerPanel)
						.title("Customers")
						.show();
		// end::detailPanel[]
	}

	void selectEntities() {
		// tag::select[]
		Store domain = new Store();

		EntityConnection connection =
						LocalEntityConnection.localEntityConnection(
										Database.instance(), domain, User.parse("scott:tiger"));

		//select customer
		Entity johnDoe = // where last name = Doe
						connection.selectSingle(Customer.LAST_NAME.equalTo("Doe"));

		//select all customer addresses
		List<Entity> customerAddresses = //where customer = john doe
						connection.select(CustomerAddress.CUSTOMER_FK.equalTo(johnDoe));

		Entity customerAddress = customerAddresses.get(0);

		Entity address = customerAddress.entity(CustomerAddress.ADDRESS_FK);

		String lastName = johnDoe.get(Customer.LAST_NAME);
		String street = address.get(Address.STREET);
		String city = address.get(Address.CITY);
		// end::select[]
	}

	void persistEntities() {
		// tag::persist[]
		Store domain = new Store();

		EntityConnection connection =
						LocalEntityConnection.localEntityConnection(
										Database.instance(), domain, User.parse("scott:tiger"));

		Entities entities = domain.entities();

		Entity customer = entities.builder(Customer.TYPE)
						.with(Customer.FIRST_NAME, "John")
						.with(Customer.LAST_NAME, "Doe")
						.build();

		customer = connection.insertSelect(customer);

		Entity address = entities.builder(Address.TYPE)
						.with(Address.STREET, "Elm Street 321")
						.with(Address.CITY, "Syracuse")
						.build();

		address = connection.insertSelect(address);

		Entity customerAddress = entities.builder(CustomerAddress.TYPE)
						.with(CustomerAddress.CUSTOMER_FK, customer)
						.with(CustomerAddress.ADDRESS_FK, address)
						.build();

		customerAddress = connection.insertSelect(customerAddress);

		customer.put(Customer.FIRST_NAME, "Jonathan");

		connection.update(customer);

		connection.delete(customerAddress.primaryKey());
		// end::persist[]
	}
}
