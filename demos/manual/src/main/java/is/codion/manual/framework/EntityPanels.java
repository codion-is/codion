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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.manual.framework;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.manual.store.domain.Store.Address;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

public final class EntityPanels {

	private final EntityConnectionProvider connectionProvider = null;

	// tag::editPanel[]
	public class AddressEditPanel extends EntityEditPanel {

		public AddressEditPanel(SwingEntityEditModel addressEditModel) {
			super(addressEditModel);
		}

		@Override
		protected void initializeUI() {
			createTextField(Address.STREET);
			createTextField(Address.CITY);
			addInputPanel(Address.STREET);
			addInputPanel(Address.CITY);
		}
	}
	// end::editPanel[]

	// tag::extended[]
	public class AddressPanel extends EntityPanel {

		public AddressPanel(SwingEntityModel addressModel) {
			super(addressModel, new AddressEditPanel(addressModel.editModel()));
		}
	}
	// end::extended[]

	void setupEntityPanel() {
		// tag::setup[]
		SwingEntityModel addressModel =
						new SwingEntityModel(Address.TYPE, connectionProvider);

		EntityPanel addressPanel =
						new EntityPanel(addressModel,
										new AddressEditPanel(addressModel.editModel()));
		// end::setup[]
	}

	// tag::detailPanel[]
	public class CustomerPanel extends EntityPanel {

		public CustomerPanel(SwingEntityModel entityModel) {
			super(entityModel);
			SwingEntityModel addressModel =
							entityModel.detailModels().get(Address.TYPE);
			detailPanels().add(new AddressPanel(addressModel));
		}
	}
	// end::detailPanel[]
}
