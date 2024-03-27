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
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.condition.Condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class SelectTest {

	@Test
	void select() {
		Select select = Select.where(Department.LOCATION.equalTo("New York"))
						.orderBy(OrderBy.ascending(Department.NAME))
						.build();
		assertFalse(select.limit().isPresent());

		select = Select.all(Department.TYPE)
						.limit(10)
						.build();
		assertEquals(10, select.limit().orElse(-1));

		assertThrows(IllegalArgumentException.class, () -> Select.all(Department.TYPE).queryTimeout(-1));
	}

	@Test
	void equals() {
		Condition condition1 = Employee.NAME.in("Luke", "John");
		Condition condition2 = Employee.NAME.in("Luke", "John");
		assertEquals(Select.where(condition1).build(), Select.where(condition2).build());
		assertEquals(Select.where(condition1)
										.orderBy(OrderBy.ascending(Employee.NAME))
										.build(),
						Select.where(condition2)
										.orderBy(OrderBy.ascending(Employee.NAME))
										.build());
		assertNotEquals(Select.where(condition1)
										.orderBy(OrderBy.ascending(Employee.NAME))
										.build(),
						Select.where(condition2)
										.build());

		assertEquals(Select.where(condition1)
										.attributes(Employee.NAME)
										.build(),
						Select.where(condition2)
										.attributes(Employee.NAME)
										.build());

		assertEquals(Select.where(condition1)
										.attributes(Employee.NAME)
										.offset(10)
										.build(),
						Select.where(condition2)
										.attributes(Employee.NAME)
										.offset(10)
										.build());

		assertNotEquals(Select.where(condition1)
										.attributes(Employee.NAME)
										.build(),
						Select.where(condition2)
										.attributes(Employee.NAME)
										.offset(10)
										.build());

		assertNotEquals(Select.where(condition1)
										.attributes(Employee.NAME)
										.build(),
						Select.where(condition2)
										.attributes(Employee.ID)
										.build());
	}
}
