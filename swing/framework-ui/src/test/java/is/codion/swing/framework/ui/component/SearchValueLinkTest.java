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
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchValueLinkTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private final EntityEditModel model = new SwingEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
	private final EntityComponents inputComponents = entityComponents(model.entityDefinition());

	@Test
	void test() {
		ComponentValue<Entity, EntitySearchField> componentValue =
						inputComponents.searchField(Employee.DEPARTMENT_FK,
														model.searchModel(Employee.DEPARTMENT_FK))
										.singleSelection()
										.buildValue();
		componentValue.link(model.editor().value(Employee.DEPARTMENT_FK));
		EntitySearchModel searchModel = componentValue.component().model();
		assertTrue(searchModel.selection().empty().is());
		Entity department = model.connection().selectSingle(Department.NAME.equalTo("SALES"));
		model.editor().value(Employee.DEPARTMENT_FK).set(department);
		assertEquals(1, searchModel.selection().entities().get().size());
		assertEquals(department, searchModel.selection().entities().get().iterator().next());
		department = model.connection().selectSingle(Department.NAME.equalTo("OPERATIONS"));
		searchModel.selection().entity().set(department);
		assertEquals(department, model.editor().value(Employee.DEPARTMENT_FK).get());
	}
}