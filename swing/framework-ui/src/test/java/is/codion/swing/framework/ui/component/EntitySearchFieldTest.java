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
package is.codion.swing.framework.ui.component;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.ListSelectionModel;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntitySearchFieldTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void componentValue() {
		EntitySearchModel searchModel = EntitySearchModel.builder()
						.entityType(Department.TYPE)
						.connection(CONNECTION)
						.build();
		ComponentValue<EntitySearchField, Entity> value = EntitySearchField.builder()
						.model(searchModel)
						.buildValue();

		assertNull(value.get());

		Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));

		searchModel.selection().entity().set(sales);
		assertEquals(sales, value.get());
		searchModel.selection().entity().clear();
		assertNull(value.get());

		value.set(sales);
		assertEquals(sales, searchModel.selection().entity().get());
		value.clear();
		assertFalse(searchModel.selection().present().is());
		assertNull(value.get());
	}

	@Test
	void text() {
		Entity jones = CONNECTION.selectSingle(Employee.NAME.equalTo("JONES"));
		EntitySearchModel searchModel = EntitySearchModel.builder()
						.entityType(Employee.TYPE)
						.connection(CONNECTION)
						.build();
		searchModel.selection().entity().set(jones);

		EntitySearchField searchField = EntitySearchField.builder()
						.model(searchModel)
						.selectionToolTip(true)
						.build();
		assertEquals("JONES", searchField.getText());
		assertEquals("JONES", searchField.getToolTipText());

		Entity blake = CONNECTION.selectSingle(Employee.NAME.equalTo("BLAKE"));
		searchModel.selection().entity().set(blake);
		assertEquals("BLAKE", searchField.getText());
		assertEquals("BLAKE", searchField.getToolTipText());

		searchModel.selection().clear();
		assertEquals("", searchField.getText());
		assertNull(searchField.getToolTipText());
	}

	@Test
	void theTextIsASingleSearchString() {
		EntitySearchModel searchModel = EntitySearchModel.builder()
						.entityType(Employee.TYPE)
						.connection(CONNECTION)
						.build();
		EntitySearchField field = EntitySearchField.builder()
						.model(searchModel)
						.build();
		field.setText("foo, bar");
		assertEquals(Set.of("foo, bar"), searchModel.search().strings().get());
	}

	@Test
	void selectorsSelectASingleEntity() {
		EntitySearchField field = EntitySearchField.builder()
						.model(EntitySearchModel.builder()
										.entityType(Employee.TYPE)
										.connection(CONNECTION)
										.build())
						.build();
		assertEquals(ListSelectionModel.SINGLE_SELECTION, EntitySearchField.listSelector(field).list().getSelectionMode());
		assertEquals(ListSelectionModel.SINGLE_SELECTION,
						EntitySearchField.tableSelector(field).table().getSelectionModel().getSelectionMode());
	}

	@Test
	void formatter() {
		EntitySearchModel model = EntitySearchModel.builder()
						.entityType(Employee.TYPE)
						.connection(CONNECTION)
						.build();
		EntitySearchField field = EntitySearchField.builder()
						.model(model)
						.formatter(entity -> entity.formatted(Employee.JOB))
						.build();
		Entity employee = CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.NAME, "Darri")
						.with(Employee.JOB, "CLERK")
						.build();
		model.selection().entity().set(employee);
		assertEquals("CLERK", field.getText());
	}
}
