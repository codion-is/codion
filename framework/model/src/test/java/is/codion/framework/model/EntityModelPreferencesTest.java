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
package is.codion.framework.model;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.DefaultEntityModelTest.TestEntityEditor;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static java.util.Arrays.asList;
import static java.util.Arrays.fill;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class EntityModelPreferencesTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private static final String NOTE = "note";
	private static final String EDIT = "model/edit";

	@Test
	void modelFailing() {
		// a model throwing from store() or restore(), an override of the application's,
		// cost every model after it its preferences, the exception reaching the caller
		TestModel failing = new TestModel(Department.TYPE, "failing");
		TestModel healthy = new TestModel(Employee.TYPE, "healthy");
		TestApplicationModel applicationModel = new TestApplicationModel(asList(failing, healthy));
		Preferences preferences = jsonPreferences();
		healthy.editModel().note = "stored";
		failing.fail = true;

		applicationModel.store(preferences);
		assertEquals("stored", preferences.node("healthy").node(EDIT).get(NOTE, null));

		healthy.editModel().note = null;
		applicationModel.restore(preferences);
		assertEquals("stored", healthy.editModel().note);
	}

	@Test
	void detailModelFailing() {
		// as for a detail model, its siblings left out with it
		TestModel master = new TestModel(Department.TYPE, "master");
		TestModel failing = new TestModel(Employee.TYPE, "failing");
		TestModel healthy = new TestModel(Employee.TYPE, "healthy");
		master.detail().add(failing, Employee.DEPARTMENT_FK);
		master.detail().add(healthy, Employee.DEPARTMENT_FK);
		Preferences preferences = jsonPreferences();
		healthy.editModel().note = "stored";
		failing.fail = true;

		master.store(preferences);
		assertEquals("stored", preferences.node("master").node("details").node("healthy").node(EDIT).get(NOTE, null));

		healthy.editModel().note = null;
		master.restore(preferences);
		assertEquals("stored", healthy.editModel().note);
	}

	@Test
	void editModelFailing() {
		// a model's own edit model failing left its detail models out
		TestModel master = new TestModel(Department.TYPE, "master");
		TestModel detail = new TestModel(Employee.TYPE, "detail");
		master.detail().add(detail, Employee.DEPARTMENT_FK);
		Preferences preferences = jsonPreferences();
		detail.editModel().note = "stored";
		master.editModel().fail = true;

		master.store(preferences);
		assertNull(preferences.node("master").node(EDIT).get(NOTE, null));
		assertEquals("stored", preferences.node("master").node("details").node("detail").node(EDIT).get(NOTE, null));

		detail.editModel().note = null;
		master.restore(preferences);
		assertEquals("stored", detail.editModel().note);
	}

	@Test
	void invalidPreferencesKey() {
		// a key no preferences node can be named by, more than 80 characters
		char[] characters = new char[81];
		fill(characters, 'k');
		TestModel invalid = new TestModel(Department.TYPE, new String(characters));
		TestModel healthy = new TestModel(Employee.TYPE, "healthy");
		TestApplicationModel applicationModel = new TestApplicationModel(asList(invalid, healthy));
		Preferences preferences = jsonPreferences();
		healthy.editModel().note = "stored";

		applicationModel.store(preferences);
		assertEquals("stored", preferences.node("healthy").node(EDIT).get(NOTE, null));

		healthy.editModel().note = null;
		applicationModel.restore(preferences);
		assertEquals("stored", healthy.editModel().note);
	}

	private static final class TestEditModel extends AbstractEntityEditModel<TestEntityEditor> {

		private String note;
		private boolean fail;

		private TestEditModel(EntityType entityType) {
			super(new TestEntityEditor(entityType, CONNECTION));
		}

		@Override
		public void store(Preferences preferences) {
			if (fail) {
				throw new IllegalStateException("store");
			}
			if (note != null) {
				preferences.put(NOTE, note);
			}
		}

		@Override
		public void restore(Preferences preferences) {
			if (fail) {
				throw new IllegalStateException("restore");
			}
			note = preferences.get(NOTE, null);
		}
	}

	private interface TestTableModel extends EntityTableModel<TestEditModel, TestEntityEditor> {}

	private static final class TestModel extends AbstractEntityModel<TestModel, TestEditModel, TestTableModel, TestEntityEditor> {

		private final String preferencesKey;

		private boolean fail;

		private TestModel(EntityType entityType, String preferencesKey) {
			super(new TestEditModel(entityType));
			this.preferencesKey = preferencesKey;
		}

		@Override
		public String preferencesKey() {
			return preferencesKey;
		}

		@Override
		public void store(Preferences preferences) {
			if (fail) {
				throw new IllegalStateException("store");
			}
			super.store(preferences);
		}

		@Override
		public void restore(Preferences preferences) {
			if (fail) {
				throw new IllegalStateException("restore");
			}
			super.restore(preferences);
		}
	}

	private static final class TestApplicationModel extends AbstractEntityApplicationModel<TestModel, TestEditModel, TestTableModel, TestEntityEditor> {

		private TestApplicationModel(Collection<TestModel> models) {
			super(CONNECTION, models);
		}
	}
}
