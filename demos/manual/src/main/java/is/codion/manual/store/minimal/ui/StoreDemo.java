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
package is.codion.manual.store.minimal.ui;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.dbms.h2.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.manual.store.minimal.domain.Store;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialDarker;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import static is.codion.manual.store.minimal.domain.Store.Address;
import static is.codion.manual.store.minimal.domain.Store.Customer;
import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.SIMPLE;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static javax.swing.BorderFactory.createEmptyBorder;

public class StoreDemo {

	private static class CustomerEditPanel extends EntityEditPanel {

		private CustomerEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			focus().initial().set(Customer.FIRST_NAME);
			createTextField(Customer.FIRST_NAME);
			createTextField(Customer.LAST_NAME);
			createTextField(Customer.EMAIL);
			createCheckBox(Customer.ACTIVE);
			setLayout(gridLayout(4, 1));
			addInputPanel(Customer.FIRST_NAME);
			addInputPanel(Customer.LAST_NAME);
			addInputPanel(Customer.EMAIL);
			addInputPanel(Customer.ACTIVE);
		}
	}

	private static class AddressEditPanel extends EntityEditPanel {

		private AddressEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			focus().initial().set(Address.STREET);
			createComboBox(Address.CUSTOMER_FK);
			createTextField(Address.STREET);
			createTextField(Address.CITY);
			setLayout(gridLayout(3, 1));
			addInputPanel(Address.CUSTOMER_FK);
			addInputPanel(Address.STREET);
			addInputPanel(Address.CITY);
		}
	}

	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(new MaterialDarker());

		Database database = H2DatabaseFactory
						.createDatabase("jdbc:h2:mem:h2db",
										"src/main/sql/create_schema_minimal.sql");

		EntityConnectionProvider connectionProvider =
						LocalEntityConnectionProvider.builder()
										.database(database)
										.domain(new Store())
										.user(User.parse("scott:tiger"))
										.build();

		SwingEntityModel customerModel =
						new SwingEntityModel(Customer.TYPE, connectionProvider);
		SwingEntityModel addressModel =
						new SwingEntityModel(Address.TYPE, connectionProvider);

		customerModel.detailModels().add(addressModel);

		EntityPanel customerPanel =
						new EntityPanel(customerModel,
										new CustomerEditPanel(customerModel.editModel()));
		EntityPanel addressPanel =
						new EntityPanel(addressModel,
										new AddressEditPanel(addressModel.editModel()));

		customerPanel.detailPanels().add(addressPanel);

		customerPanel.setBorder(createEmptyBorder(5, 5, 0, 5));
		addressPanel.tablePanel()
						.condition().view().set(SIMPLE);

		customerModel.tableModel().items().refresh();

		SwingUtilities.invokeLater(() ->
						Dialogs.componentDialog()
										.component(customerPanel.initialize())
										.title("Customers")
										.onClosed(e -> connectionProvider.close())
										.show());
	}
}
