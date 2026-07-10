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
package is.codion.swing.framework.ui;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.EntityQueries;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;

public final class SelectQueryInspectorTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void rendersTheQueryTheModelRuns() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		//the wide table optimization, the very case one opens the inspector to see
		tableModel.query().attributes().defaults().set(singleton(Employee.NAME));

		String rendered = new SelectQueryInspector(tableModel.query()).createSelectQuery();

		EntityQueries queries = EntityQueries.factory().orElseThrow()
						.create(Database.instance(), CONNECTION_PROVIDER.entities());
		assertEquals(queries.select(tableModel.query().select()), rendered);
		//the defaults belong in Select.attributes(), an inspector rebuilding the select
		//via include() rendered every column while the model selected the subset
		assertTrue(rendered.contains("ename"), rendered);
		assertFalse(rendered.contains("sal"), rendered);
	}
}
