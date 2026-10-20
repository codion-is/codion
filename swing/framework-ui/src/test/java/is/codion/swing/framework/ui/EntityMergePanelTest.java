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

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityMerge;
import is.codion.framework.model.EntityMerge.AttributeMerge;
import is.codion.framework.model.EntityMerge.Side;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.framework.ui.EntityMergePanel.MergeColumn;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import java.util.List;

import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityMergePanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void twoWay() {
		Entity allen = allen();
		Entity mine = allen.copy().mutable();
		mine.set(Employee.NAME, "MINE");
		mine.set(Employee.SALARY, 2000d);
		Entity theirs = allen.copy().mutable();
		theirs.set(Employee.SALARY, 3000d);
		theirs.save();
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(theirs)
						.build();
		EntityMergePanel panel = EntityMergePanel.builder(merge).build();
		assertSame(merge, panel.merge());
		FilterTable<AttributeMerge<?>, MergeColumn> table = panel.table();
		// without a base, neither who changed a value nor its original
		assertEquals(asList(MergeColumn.ATTRIBUTE, MergeColumn.MINE, MergeColumn.THEIRS),
						table.model().columns().identifiers());
		assertEquals("Yours", table.model().columns().caption(MergeColumn.MINE));
		assertEquals("Theirs", table.model().columns().caption(MergeColumn.THEIRS));
		assertEquals(2, table.model().items().included().size());
		assertEquals(Employee.NAME.name(), table.model().getValueAt(0, 0));
		assertEquals("MINE", table.model().getValueAt(0, 1));
		assertEquals("ALLEN", table.model().getValueAt(0, 2));
		// the first row selected
		assertEquals(0, table.model().selection().index().get());

		action(table, VK_LEFT).actionPerformed(null);
		assertEquals(Side.MINE, merge.attributes().get(0).side().get());
		assertNull(merge.attributes().get(1).side().get());

		table.model().selection().selectAll();
		action(table, VK_RIGHT).actionPerformed(null);
		merge.attributes().forEach(attribute -> assertEquals(Side.THEIRS, attribute.side().get()));
		assertTrue(merge.complete().is());
	}

	@Test
	void threeWay() {
		Entity allen = allen();
		Entity mine = allen.copy().mutable();
		mine.set(Employee.NAME, "MINE");
		mine.set(Employee.COMMISSION, 100d);
		Entity theirs = allen.copy().mutable();
		theirs.set(Employee.SALARY, 3000d);
		theirs.set(Employee.COMMISSION, 200d);
		theirs.save();
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(theirs)
						.base(allen)
						.build();
		EntityMergePanel panel = EntityMergePanel.builder(merge)
						.baseCaption("Base")
						.mineCaption("This device")
						.theirsCaption("The server")
						.build();
		FilterTable<AttributeMerge<?>, MergeColumn> table = panel.table();
		assertEquals(asList(MergeColumn.ATTRIBUTE, MergeColumn.CHANGE, MergeColumn.BASE, MergeColumn.MINE, MergeColumn.THEIRS),
						table.model().columns().identifiers());
		assertEquals("Base", table.model().columns().caption(MergeColumn.BASE));
		assertEquals("This device", table.model().columns().caption(MergeColumn.MINE));
		assertEquals("The server", table.model().columns().caption(MergeColumn.THEIRS));
		// name, salary and commission, changed by mine, theirs and both
		assertEquals(asList("This device", "The server", "Both"), column(table, 1));
		assertEquals(asList("ALLEN", "1600", "300"), column(table, 2));
	}

	private static List<Object> column(FilterTable<AttributeMerge<?>, MergeColumn> table, int column) {
		return asList(table.model().getValueAt(0, column), table.model().getValueAt(1, column), table.model().getValueAt(2, column));
	}

	private static Action action(FilterTable<?, ?> table, int keyCode) {
		return table.getActionMap().get(table.getInputMap(JComponent.WHEN_FOCUSED).get(keyStroke(keyCode)));
	}

	private static Entity allen() {
		return CONNECTION.selectSingle(Employee.NAME.equalTo("ALLEN"));
	}
}
