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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntitySearchFieldTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void componentValue() {
		EntitySearchModel singleSelectionSearchModel = EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER)
						.singleSelection(true)
						.build();
		ComponentValue<Entity, EntitySearchField> singleSelectionValue = EntitySearchField.builder(singleSelectionSearchModel)
						.singleSelection()
						.buildValue();

		assertNull(singleSelectionValue.get());

		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));

		singleSelectionSearchModel.selection().entity().set(sales);
		assertEquals(sales, singleSelectionValue.get());
		singleSelectionSearchModel.selection().entity().clear();
		assertNull(singleSelectionValue.get());

		assertNull(singleSelectionValue.get());

		Entity research = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("RESEARCH"));

		assertThrows(IllegalArgumentException.class, () -> singleSelectionSearchModel.selection().entities().set(Arrays.asList(sales, research)));

		singleSelectionValue.clear();
		assertTrue(singleSelectionSearchModel.selection().empty().get());
		assertNull(singleSelectionValue.get());

		EntitySearchModel multiSelectionSearchModel = EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build();
		ComponentValue<Set<Entity>, EntitySearchField> multiSelectionValue = EntitySearchField.builder(multiSelectionSearchModel)
						.multiSelection()
						.buildValue();

		assertTrue(multiSelectionValue.getOrThrow().isEmpty());

		multiSelectionSearchModel.selection().entities().set(Arrays.asList(sales, research));

		assertTrue(multiSelectionValue.getOrThrow().containsAll(Arrays.asList(sales, research)));
	}

	@Test
	void text() {
		Entity jones = CONNECTION_PROVIDER.connection().selectSingle(Employee.NAME.equalTo("JONES"));
		EntitySearchModel searchModel = EntitySearchModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		searchModel.selection().entity().set(jones);

		EntitySearchField searchField = EntitySearchField.builder(searchModel)
						.multiSelection()
						.separator(";")
						.build();
		assertEquals("JONES", searchField.getText());

		Entity blake = CONNECTION_PROVIDER.connection().selectSingle(Employee.NAME.equalTo("BLAKE"));
		Entity allen = CONNECTION_PROVIDER.connection().selectSingle(Employee.NAME.equalTo("ALLEN"));

		searchModel.selection().entities().set(asList(jones, blake, allen));
		assertEquals("ALLEN;BLAKE;JONES", searchField.getText());

		List<Entity> result = new ArrayList<>(asList(jones, blake, allen));
		Collections.reverse(result);
		searchModel.selection().entities().set(result);
		assertEquals("ALLEN;BLAKE;JONES", searchField.getText());
	}

	@Test
	void stringFactory() {
		EntitySearchModel model = EntitySearchModel.builder(Employee.TYPE, CONNECTION_PROVIDER).build();
		EntitySearchField field = EntitySearchField.builder(model)
						.multiSelection()
						.stringFactory(entity -> entity.string(Employee.JOB))
						.build();
		Entity employee = CONNECTION_PROVIDER.entities().builder(Employee.TYPE)
						.with(Employee.NAME, "Darri")
						.with(Employee.JOB, "CLERK")
						.build();
		model.selection().entity().set(employee);
		assertEquals("CLERK", field.getText());
	}
}
