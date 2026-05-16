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

import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Genre;
import is.codion.demos.chinook.domain.api.Chinook.Preferences;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EntityEditor.EditorValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityEditor;

import org.junit.jupiter.api.Test;

import static is.codion.framework.db.EntityConnection.Count.where;
import static org.junit.jupiter.api.Assertions.*;

public final class CustomerEditModelTest {

	@Test
	void preferences() throws EntityValidationException {
		try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
			EntityConnection connection = connectionProvider.connection();
			connection.startTransaction();

			Entity metal = connection.selectSingle(Genre.NAME.equalTo("Metal"));

			SwingEntityEditModel editModel = new CustomerModel(connectionProvider).editModel();

			SwingEntityEditor customerEditor = editModel.editor();
			customerEditor.value(Customer.FIRSTNAME).set("John");
			customerEditor.value(Customer.LASTNAME).set("Silence");
			customerEditor.value(Customer.EMAIL).set("silence@email.com");

			SwingEntityEditor preferencesEditor = customerEditor.detail().get(Preferences.CUSTOMER_FK);

			EditorValue<Entity> preferredGenre = preferencesEditor.value(Preferences.PREFERRED_GENRE_FK);
			EditorValue<Boolean> newsletter = preferencesEditor.value(Preferences.NEWSLETTER);

			preferredGenre.set(metal);
			newsletter.set(true);

			Entity customer = customerEditor.insert();
			Long customerId = customer.primaryKey().value();
			Entity preferences = connection.selectSingle(Preferences.CUSTOMER_ID.equalTo(customerId));

			preferredGenre.clear(); // still present, should update

			customerEditor.update();

			preferences = connection.selectSingle(Preferences.CUSTOMER_ID.equalTo(customerId));
			assertTrue(preferences.isNull(Preferences.PREFERRED_GENRE_FK));

			customerEditor.modified().addConsumer(mod -> System.out.println("Modified: " + mod));

			newsletter.clear();// no longer present, should delete

			customerEditor.update();
			assertEquals(0, connection.count(where(Preferences.CUSTOMER_ID.equalTo(customerId))));

			newsletter.set(false);// present again, should insert
			customerEditor.update();

			preferences = connection.selectSingle(Preferences.CUSTOMER_ID.equalTo(customerId));
			assertTrue(preferences.isNull(Preferences.PREFERRED_GENRE_FK));
			assertFalse(preferences.get(Preferences.NEWSLETTER));

			customerEditor.delete();
			assertEquals(0, connection.count(where(Preferences.CUSTOMER_ID.equalTo(customerId))));

			connection.rollbackTransaction();
		}
	}

	private static EntityConnectionProvider createConnectionProvider() {
		return LocalEntityConnectionProvider.builder()
						.domain(new ChinookImpl())
						.user(User.parse("scott:tiger"))
						.build();
	}
}
