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
package is.codion.manual.store.ui;

import is.codion.common.user.User;
import is.codion.manual.store.domain.Store;
import is.codion.manual.store.domain.Store.Address;
import is.codion.manual.store.domain.Store.Customer;
import is.codion.manual.store.domain.Store.CustomerAddress;
import is.codion.manual.store.model.CustomerAddressModel;
import is.codion.manual.store.model.CustomerModel;
import is.codion.manual.store.model.StoreApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import java.util.List;
import java.util.Locale;

// tag::storeAppPanel[]
public class StoreApplicationPanel extends EntityApplicationPanel<StoreApplicationModel> {

	public StoreApplicationPanel(StoreApplicationModel applicationModel) {
		super(applicationModel);
	}

	@Override
	protected List<EntityPanel> createEntityPanels() {
		CustomerModel customerModel =
						applicationModel().entityModels().get(Customer.TYPE);
		CustomerAddressModel customerAddressModel =
						customerModel.detailModels().get(CustomerAddress.TYPE);

		EntityPanel customerPanel = new EntityPanel(customerModel,
						new CustomerEditPanel(customerModel.editModel()),
						new CustomerTablePanel(customerModel.tableModel()));
		EntityPanel customerAddressPanel = new EntityPanel(customerAddressModel,
						new CustomerAddressEditPanel(customerAddressModel.editModel()));

		customerPanel.detailPanels().add(customerAddressPanel);

		return List.of(customerPanel);
	}

	// tag::createSupportEntityPanelBuilders[]
	@Override
	protected List<EntityPanel.Builder> createSupportEntityPanelBuilders() {
		EntityPanel.Builder addressPanelBuilder =
						EntityPanel.builder(Address.TYPE)
										.editPanel(AddressEditPanel.class);

		return List.of(addressPanelBuilder);
	}
	// end::createSupportEntityPanelBuilders[]

	public static void main(String[] args) {
		Locale.setDefault(new Locale("en", "EN"));
		EntityPanel.Config.TOOLBAR_CONTROLS.set(true);
		ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
						.set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
		EntityApplicationPanel.builder(StoreApplicationModel.class, StoreApplicationPanel.class)
						.applicationName("Store")
						.domainType(Store.DOMAIN)
						.defaultLoginUser(User.parse("scott:tiger"))
						.start();
	}
}
// end::storeAppPanel[]