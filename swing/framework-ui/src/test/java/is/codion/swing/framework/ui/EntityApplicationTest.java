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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class EntityApplicationTest {

	@Test
	void connection() {
		// In-memory preferences (application-level and model), so the test never touches the real user store.
		Preferences preferences = jsonPreferences();
		// Always the same super username, connection created eagerly, so must be an existing user, tests use assertSame()
		User user = User.user("sa");
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						.model(TestApplicationModel::new)
						.onStarted(panel -> assertSame(user, panel.applicationModel().connection().user()))
						.user(user)
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
		User user2 = User.user("sa");
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						.model(TestApplicationModel::new)
						.onStarted(panel -> assertSame(user2, panel.applicationModel().connection().user()))
						.user(() -> user2)
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
		User user3 = User.user("sa");
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						.model(TestApplicationModel::new)
						.onStarted(panel -> assertSame(user3, panel.applicationModel().connection().user()))
						.user(user3)
						.connection(usr -> LocalEntityConnection.builder()
										.domain(new TestDomain())
										.user(usr)
										.build())
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
		User user4 = User.user("sa");
		LocalEntityConnection connection = LocalEntityConnection.builder()
						.domain(new TestDomain())
						.user(user4)
						.build();
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						.model(TestApplicationModel::new)
						.onStarted(panel -> {
							assertSame(connection, panel.applicationModel().connection());
							assertSame(user4, panel.applicationModel().connection().user());
						})
						.connection(connection)
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
	}

	public static class TestApplicationModel extends SwingEntityApplicationModel {

		public TestApplicationModel(EntityConnection connection) {
			super(connection, emptyList());
		}
	}

	public static class TestApplicationPanel extends EntityApplicationPanel<TestApplicationModel> {

		public TestApplicationPanel(TestApplicationModel applicationModel) {
			super(applicationModel, emptyList(), emptyList());
		}
	}
}
