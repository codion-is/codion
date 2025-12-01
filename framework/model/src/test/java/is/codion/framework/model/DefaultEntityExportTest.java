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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.State;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityExport.Settings.Attributes;
import is.codion.framework.model.EntityExport.Settings.ForeignKeyExport;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DefaultEntityExportTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void test() {
		EntityExport.Settings employee = EntityExport.settings(Employee.TYPE, CONNECTION_PROVIDER.entities());
		// All direct attributes
		includeAll(employee.attributes());
		// Department
		ForeignKeyExport department = employee.attributes().get().stream()
						.filter(attribute -> attribute instanceof ForeignKeyExport)
						.map(ForeignKeyExport.class::cast)
						.filter(foreignKey -> foreignKey.attribute().referencedType().equals(Department.TYPE))
						.findFirst()
						.orElseThrow(IllegalStateException::new);
		includeAll(department.attributes());
		// Manager
		ForeignKeyExport manager = employee.attributes().get().stream()
						.filter(attribute -> attribute instanceof ForeignKeyExport)
						.map(ForeignKeyExport.class::cast)
						.peek(foreignKey -> System.out.println(foreignKey.attribute().referencedType()))
						.filter(foreignKey -> foreignKey.attribute().referencedType().equals(Employee.TYPE))
						.findFirst()
						.orElseThrow(IllegalStateException::new);
		assertTrue(manager.expandable());
		manager.expand();
		includeAll(manager.attributes());
		// Managers manager
		ForeignKeyExport managersManager = manager.attributes().get().stream()
						.filter(attribute -> attribute instanceof ForeignKeyExport)
						.map(ForeignKeyExport.class::cast)
						.filter(foreignKey -> foreignKey.attribute().referencedType().equals(Employee.TYPE))
						.findFirst()
						.orElseThrow(IllegalStateException::new);
		assertTrue(managersManager.expandable());
		managersManager.expand();
		includeAll(managersManager.attributes());

		StringBuilder output = new StringBuilder();
		AtomicInteger count = new AtomicInteger();
		EntityConnection connection = CONNECTION_PROVIDER.connection();
		EntityExport.builder()
						.entities(connection.iterator(all(Employee.TYPE)))
						.connectionProvider(CONNECTION_PROVIDER)
						.output(output::append)
						.settings(employee)
						.handler(entity -> count.getAndIncrement())
						.cancel(State.state())
						.export();

		Entity jones = CONNECTION_PROVIDER.connection().selectSingle(Employee.NAME.equalTo("JONES"));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.ID.equalTo(10));

		List<Entity> entities = asList(jones, accounting);

		assertThrows(IllegalArgumentException.class, () -> EntityExport.builder()
					.entities(entities.iterator())
					.connectionProvider(CONNECTION_PROVIDER)
					.output(line -> {})
					.settings(employee)
					.handler(entity -> {})
					.cancel(State.state())
					.export());
	}

	private static void includeAll(Attributes attributes) {
		attributes.get().forEach(attribute -> attribute.include().set(true));
	}
}
