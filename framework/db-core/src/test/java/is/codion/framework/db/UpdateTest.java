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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class UpdateTest {

	@Test
	void updateDuplicate() {
		assertThrows(IllegalStateException.class, () -> Update.all(Employee.TYPE)
						.set(Employee.COMMISSION, 123d)
						.set(Employee.COMMISSION, 123d));
	}

	@Test
	void updateNoValues() {
		assertThrows(IllegalStateException.class, () -> Update.all(Employee.TYPE).build());
	}
}
