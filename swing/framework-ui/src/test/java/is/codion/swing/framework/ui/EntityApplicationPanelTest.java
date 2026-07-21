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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityApplicationPanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@AfterEach
	void tearDown() {
		Thread.setDefaultUncaughtExceptionHandler(null);
	}

	@Test
	void test() {
		EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
		// In-memory preferences for both the application level and the model, so the test never touches the real store.
		Preferences preferences = jsonPreferences();
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.user(UNIT_TEST_USER)
						.preferences(preferences)
						.model(connectionProvider -> new TestApplicationModel(connectionProvider, preferences))
						.uncaughtExceptionHandler(false)
						.saveDefaultUsername(false)
						.displayFrame(false)
						.mainMenu(true)
						.startupDialog(false)
						.start(false);
	}

	@Test
	void preferencesModelAndViewRoundTrip() {
		// The model walk and the UI walk share the entities/<key> node, writing disjoint model/ and view/ subtrees
		Preferences entities = jsonPreferences();

		SwingEntityModel model = new SwingEntityModel(Employee.TYPE, CONNECTION_PROVIDER);
		model.tableModel().query().condition().get(Employee.NAME).caseSensitive().set(true); // model state
		EntityPanel panel = new EntityPanel(model);
		panel.tablePanel().table().columnModel().visible(Employee.COMMISSION).set(false); // view state

		model.store(entities);
		panel.store(entities.node(panel.preferencesKey()));

		SwingEntityModel restoredModel = new SwingEntityModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityPanel restoredPanel = new EntityPanel(restoredModel);
		restoredModel.restore(entities);
		restoredPanel.restore(entities.node(restoredPanel.preferencesKey()));

		assertTrue(restoredModel.tableModel().query().condition().get(Employee.NAME).caseSensitive().is());
		assertFalse(restoredPanel.tablePanel().table().columnModel().visible(Employee.COMMISSION).is());
	}

	private static final class TestApplicationModel extends SwingEntityApplicationModel {

		public TestApplicationModel(EntityConnectionProvider connectionProvider, Preferences preferences) {
			super(connectionProvider, singletonList(new SwingEntityModel(Employee.TYPE, connectionProvider)), preferences);
		}
	}

	private static final class TestApplicationPanel extends EntityApplicationPanel<TestApplicationModel> {

		public TestApplicationPanel(TestApplicationModel applicationModel) {
			super(applicationModel, createPanels(applicationModel), emptyList());
		}

		private static List<EntityPanel> createPanels(TestApplicationModel applicationModel) {
			return singletonList(new EntityPanel(applicationModel.models().get(Employee.TYPE)));
		}
	}
}
