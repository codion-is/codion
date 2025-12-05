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
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableExportModel.AttributeNode;
import is.codion.swing.framework.ui.EntityTableExportModel.MutableForeignKeyNode;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.prefs.Preferences;

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

		exportPanel.model().includeDefault();

		// Save preferences - should be empty JSON since it matches defaults
		EntityTablePanelPreferences defaultPreferences = new EntityTablePanelPreferences(tablePanel);
		Preferences prefs = Preferences.userNodeForPackage(getClass()).node("exportPreferencesDefaults");
		defaultPreferences.save(prefs);

		// Should save empty JSON for export since it matches defaults
		String exportJson = prefs.get(tablePanel.preferencesKey() + "-export", "");
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
			if (node.attribute().equals(Employee.MGR_FK)) {
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
			if (child instanceof MutableForeignKeyNode && child.attribute().equals(Employee.MGR_FK)) {
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
}
