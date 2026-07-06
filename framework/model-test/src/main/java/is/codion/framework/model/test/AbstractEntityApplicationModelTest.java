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
package is.codion.framework.model.test;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.model.AbstractEntityApplicationModel;
import is.codion.framework.model.AbstractEntityEditModel;
import is.codion.framework.model.AbstractEntityModel;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityEditor;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityApplicationModel} subclasses.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @param <R> the {@link EntityEditor} type
 */
public abstract class AbstractEntityApplicationModelTest<M extends AbstractEntityModel<M, E, T, R>,
				E extends AbstractEntityEditModel<R>, T extends EntityTableModel<E, R>, R extends EntityEditor<R>> {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private final EntityConnectionProvider connectionProvider;

	protected AbstractEntityApplicationModelTest() {
		this.connectionProvider = CONNECTION_PROVIDER;
	}

	@Test
	public void applicationModel() {
		M deptModel = createDepartmentModel();
		EntityApplicationModel<M, E, T, R> model = new TestEntityApplicationModel<>(connectionProvider, singleton(deptModel));
		assertNotNull(model.models().get(Department.TYPE));
		assertEquals(1, model.models().get().size());
		assertEquals(UNIT_TEST_USER, model.user());

		assertThrows(IllegalArgumentException.class, () -> model.models().get(Employee.TYPE));
	}

	@Test
	public void constructorNullConnectionProvider() {
		assertThrows(NullPointerException.class, () -> new TestEntityApplicationModel<>(null, emptyList()));
	}

	@Test
	public void entityModelByEntityTypeNotFound() {
		EntityApplicationModel<M, E, T, R> model = new TestEntityApplicationModel<>(connectionProvider, emptyList());
		assertThrows(IllegalArgumentException.class, () -> model.models().get(Department.TYPE));
	}

	@Test
	public void entityModelByEntityType() {
		M departmentModel = createDepartmentModel();
		EntityApplicationModel<M, E, T, R> model = new TestEntityApplicationModel<>(connectionProvider, singleton(departmentModel));
		assertEquals(departmentModel, model.models().get(Department.TYPE));
	}

	@Test
	public void entityModelByClass() {
		M departmentModel = createDepartmentModel();
		EntityApplicationModel<M, E, T, R> model = new TestEntityApplicationModel<>(connectionProvider, singleton(departmentModel));
		assertEquals(departmentModel, model.models().get((Class<? extends M>) departmentModel.getClass()));
	}

	@Test
	public void containsEntityModel() {
		M departmentModel = createDepartmentModel();
		EntityApplicationModel<M, E, T, R> model = new TestEntityApplicationModel<>(connectionProvider, singleton(departmentModel));

		assertTrue(model.models().contains(Department.TYPE));
		assertTrue(model.models().contains((Class<? extends M>) departmentModel.getClass()));
		assertTrue(model.models().contains(departmentModel));

		assertFalse(model.models().contains(Employee.TYPE));
		M detailModel = departmentModel.detail().get(Employee.TYPE);
		assertFalse(model.models().contains(detailModel));
	}

	protected final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	/**
	 * @return a EntityModel based on the department entity
	 * @see Department#TYPE
	 */
	protected abstract M createDepartmentModel();

	private static final class TestEntityApplicationModel<
					M extends EntityModel<M, E, T, R>,
					E extends EntityEditModel<R>,
					T extends EntityTableModel<E, R>,
					R extends EntityEditor<R>>
					extends AbstractEntityApplicationModel<M, E, T, R> {

		private TestEntityApplicationModel(EntityConnectionProvider connectionProvider, Collection<? extends M> entityModels) {
			super(connectionProvider, entityModels);
		}
	}
}
