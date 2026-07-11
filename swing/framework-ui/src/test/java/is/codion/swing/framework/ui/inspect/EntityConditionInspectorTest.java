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
package is.codion.swing.framework.ui.inspect;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.utilities.Operator;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.common.ui.inspect.UiInspector;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public final class EntityConditionInspectorTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void locatedViaServiceLoader() {
		assertTrue(UiInspector.instances().stream().anyMatch(EntityConditionInspector.class::isInstance));
	}

	@Test
	void notApplicableWithoutConditionPanel() {
		assertFalse(new EntityConditionInspector().state(new JTextField()).isPresent());
	}

	@Test
	void queryCondition() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Department.TYPE, CONNECTION_PROVIDER);
		ConditionModel<String> condition = tableModel.query().condition().get(Department.NAME);
		condition.operator().set(Operator.EQUAL);
		condition.operands().equal().set("SALES");
		condition.enabled().set(true);

		Map<String, Object> state = EntityConditionInspector.state(tableModel, condition).orElseThrow();
		assertEquals("employees.department", state.get("entityType"));
		//the focused field belongs to the query condition, not the client-side filter
		assertEquals("condition", state.get("type"));
		assertEquals(Department.NAME.toString(), state.get("attribute"));
		assertEquals(Boolean.TRUE, state.get("enabled"));
		assertEquals("EQUAL", state.get("operator"));
		assertEquals("SALES", ((Map<?, ?>) state.get("operands")).get("equal"));
	}

	@Test
	void clientFilter() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Department.TYPE, CONNECTION_PROVIDER);
		ConditionModel<String> filter = tableModel.filters().get(Department.NAME);
		filter.operator().set(Operator.EQUAL);
		filter.operands().equal().set("ACCOUNTING");

		Map<String, Object> state = EntityConditionInspector.state(tableModel, filter).orElseThrow();
		//the same identity match distinguishes the two systems the user drives
		assertEquals("filter", state.get("type"));
		assertEquals(Department.NAME.toString(), state.get("attribute"));
		assertEquals("ACCOUNTING", ((Map<?, ?>) state.get("operands")).get("equal"));
	}

	@Test
	void unknownModelDoesNotApply() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Department.TYPE, CONNECTION_PROVIDER);
		//a condition model belonging to no column of this table matches neither system
		ConditionModel<String> orphan = ConditionModel.builder().valueClass(String.class).build();

		Optional<Map<String, Object>> state = EntityConditionInspector.state(tableModel, orphan);
		assertFalse(state.isPresent());
	}
}
