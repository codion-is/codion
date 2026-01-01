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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.domain;

import is.codion.demos.employees.domain.Employees.Department;
import is.codion.demos.employees.domain.Employees.Employee;
import is.codion.framework.domain.test.DomainTest;

import org.junit.jupiter.api.Test;

// tag::domainTest[]
public class EmployeesTest extends DomainTest {

	public EmployeesTest() {
		super(new Employees());
	}

	@Test
	void department() {
		test(Department.TYPE);
	}

	@Test
	void employee() {
		test(Employee.TYPE);
	}
}
// end::domainTest[]