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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Preferences;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EditorLink;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityEditor;

import java.util.function.Predicate;

// tag::customerEditModel[]
public final class CustomerEditModel extends SwingEntityEditModel {

	public CustomerEditModel(EntityConnectionProvider connectionProvider) {
		super(Customer.TYPE, connectionProvider);
		editor().comboBoxModels().initialize(Customer.SUPPORTREP_FK);
		// Set a detail editor, in order to edit customer preferences alongside the customer
		SwingEntityEditor preferences = new SwingEntityEditor(Preferences.TYPE, connectionProvider);
		preferences.value(Preferences.PREFERRED_GENRE_FK).persist().set(false);
		preferences.comboBoxModels().initialize(Preferences.PREFERRED_GENRE_FK);
		editor().detail().add(EditorLink.builder()
						.editor(preferences)
						.foreignKey(Preferences.CUSTOMER_FK)
						.present(new PreferencesPresent())
						.build());
	}

	private static final class PreferencesPresent implements Predicate<Entity> {

		@Override
		public boolean test(Entity preferences) {
			// Preferences without both preferred genre and newsletter are deleted
			return !preferences.isNull(Preferences.PREFERRED_GENRE_FK) ||
							!preferences.isNull(Preferences.NEWSLETTER);
		}
	}
}
// end::customerEditModel[]