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
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

public final class EntityPopupMenuTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void test() throws DatabaseException {
		try (EntityConnectionProvider connectionProvider = LocalEntityConnectionProvider.builder()
						.domain(new TestDomain())
						.user(UNIT_TEST_USER)
						.build()) {
			Entity blake = connectionProvider.connection().selectSingle(Employee.NAME.equalTo("BLAKE"));
			blake.put(Employee.NAME, "a really long name aaaaaaaaaaaaaaaaaaaaaaaaaa");
			blake.put(Employee.SALARY, 100d);

			new EntityPopupMenu(blake, connectionProvider.connection());
		}
	}
}
