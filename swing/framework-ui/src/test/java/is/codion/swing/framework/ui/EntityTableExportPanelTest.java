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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableExportTreeModel.AttributeNode;
import is.codion.swing.framework.ui.EntityTableExportTreeModel.EntityNode;
import is.codion.swing.framework.ui.EntityTableExportTreeModel.MutableForeignKeyNode;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityTableExportPanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	@Test
	void exportPreferencesDefaults() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportPanel exportPanel = tablePanel.exportPanel();

		exportPanel.model().treeModel().includeNone();
		exportPanel.model().treeModel().includeAll();

		// Store preferences - export should be empty JSON since it matches defaults
		Preferences prefs = jsonPreferences();
		EntityTablePanelPreferences.store(prefs, tablePanel);

		// Should store empty JSON for export since it matches defaults
		String exportJson = prefs.get("export", "");
		assertEquals("{}", exportJson);
	}

	@Test
	void cyclicalForeignKeyExpansion() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportPanel exportPanel = tablePanel.exportPanel();

		// Find the MGR_FK node (cyclical self-reference)
		MutableForeignKeyNode mgrNode = null;
		Enumeration<TreeNode> children = exportPanel.model().treeModel().getRoot().children();
		while (children.hasMoreElements()) {
			AttributeNode node = (AttributeNode) children.nextElement();
			if (node.definition().attribute().equals(Employee.MGR_FK)) {
				mgrNode = (MutableForeignKeyNode) node;
				break;
			}
		}

		assertNotNull(mgrNode, "MGR_FK node should exist");
		assertEquals(0, mgrNode.getChildCount(), "Expandable stub should have no children initially");
		assertFalse(mgrNode.isLeaf(), "Expandable should not be a leaf (to show expand icon)");

		// Expand the cyclical stub
		mgrNode.populate();

		// After expansion, should have children
		assertTrue(mgrNode.getChildCount() > 0, "After expansion should have children");

		// Find the nested MGR_FK (manager's manager)
		MutableForeignKeyNode nestedMgrNode = null;
		Enumeration<TreeNode> mgrChildren = mgrNode.children();
		while (mgrChildren.hasMoreElements()) {
			AttributeNode child = (AttributeNode) mgrChildren.nextElement();
			if (child instanceof MutableForeignKeyNode && child.definition().attribute().equals(Employee.MGR_FK)) {
				nestedMgrNode = (MutableForeignKeyNode) child;
				break;
			}
		}

		assertNotNull(nestedMgrNode, "Nested MGR_FK should exist after expansion");
		assertEquals(0, nestedMgrNode.getChildCount(), "Nested expandable node should have no children initially");

		// Expand again (manager's manager's manager)
		nestedMgrNode.populate();
		assertTrue(nestedMgrNode.getChildCount() > 0, "After second expansion should have children");
	}

	@Test
	void moveOntoOwnPositionDoesNotThrow() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityNode root = tablePanel.exportPanel().model().treeModel().getRoot();

		List<AttributeNode> before = childNodes(root);
		assertTrue(before.size() > 3);
		AttributeNode third = before.get(2);
		//dropping a node exactly where it already sits (drop index == its own index) must not throw
		root.move(singletonList(third), 2);
		assertEquals(before, childNodes(root));
	}

	@Test
	void rowSelectionFallsBackToAll() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		tableModel.items().refresh();
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportModel model = tablePanel.exportPanel().model();

		tableModel.selection().index().set(0);
		assertTrue(model.selected().is());
		assertFalse(model.all().is());

		tableModel.selection().clear();
		//clearing the selection deactivates 'selected'; the group must fall back to 'all', not leave both off
		assertFalse(model.selected().is());
		assertTrue(model.all().is());
	}

	private static List<AttributeNode> childNodes(EntityNode node) {
		return Collections.list(node.children()).stream()
						.map(AttributeNode.class::cast)
						.collect(toList());
	}
}
