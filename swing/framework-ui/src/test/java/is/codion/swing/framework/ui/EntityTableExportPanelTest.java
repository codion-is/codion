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

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableExport.AttributeNode;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Detail;
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

		// Set up the default configuration (what selectDefaults() would produce)
		exportPanel.selectDefaults();

		// Save preferences - should be empty JSON since it matches defaults
		EntityTablePanelPreferences defaultPreferences = new EntityTablePanelPreferences(tablePanel);
		Preferences prefs = Preferences.userNodeForPackage(getClass()).node("exportPreferencesDefaults");
		defaultPreferences.save(prefs);

		// Should save empty JSON for export since it matches defaults
		String exportJson = prefs.get(tablePanel.preferencesKey() + "-export", "");
		assertEquals("{}", exportJson);
	}

	@Test
	void exportPreferencesCustom() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportPanel exportPanel = tablePanel.exportPanel();

		// Customize: select only ID and DEPARTMENT foreign key with NAME
		Enumeration<TreeNode> children = exportPanel.tableExport().entityNode().children();
		while (children.hasMoreElements()) {
			AttributeNode node = (AttributeNode) children.nextElement();
			node.selected().set(false);
			if (node.definition().attribute().equals(Employee.ID)) {
				node.selected().set(true);
			}
			else if (node.definition().attribute().equals(Employee.DEPARTMENT_FK)) {
				node.selected().set(true);
				// Select NAME from Department
				Enumeration<TreeNode> deptChildren = node.children();
				while (deptChildren.hasMoreElements()) {
					AttributeNode deptNode = (AttributeNode) deptChildren.nextElement();
					if (deptNode.definition().attribute().equals(Department.NAME)) {
						deptNode.selected().set(true);
					}
				}
			}
		}

		// Save custom preferences
		EntityTablePanelPreferences customPreferences = new EntityTablePanelPreferences(tablePanel);
		Preferences prefs = Preferences.userNodeForPackage(getClass()).node("exportPreferencesCustom");
		customPreferences.save(prefs);

		// Should save non-empty JSON
		String exportJson = prefs.get(tablePanel.preferencesKey() + "-export", "");
		assertNotEquals("{}", exportJson);
		// Attributes array contains selected attribute names
		assertTrue(exportJson.contains("\"attributes\""));
		assertTrue(exportJson.contains("\"" + Employee.ID.name() + "\""));
		assertTrue(exportJson.contains("\"" + Employee.DEPARTMENT_FK.name() + "\""));
		// ForeignKeys object contains FK structure
		assertTrue(exportJson.contains("\"foreignKeys\""));
		assertTrue(exportJson.contains("\"" + Department.NAME.name() + "\""));
	}

	@Test
	void exportPreferencesApply() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportPanel exportPanel = tablePanel.exportPanel();

		// Customize configuration
		Enumeration<TreeNode> children = exportPanel.tableExport().entityNode().children();
		while (children.hasMoreElements()) {
			AttributeNode node = (AttributeNode) children.nextElement();
			node.selected().set(false);
			if (node.definition().attribute().equals(Employee.COMMISSION)) {
				node.selected().set(true);
			}
		}

		// Save
		EntityTablePanelPreferences preferences = new EntityTablePanelPreferences(tablePanel);
		Preferences prefs = Preferences.userNodeForPackage(getClass()).node("exportPreferencesApply");
		preferences.save(prefs);

		// Create new panel and apply preferences
		SwingEntityTableModel newTableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel newTablePanel = new EntityTablePanel(newTableModel, config ->  config.includeExport(true));
		EntityTableExportPanel newExportPanel = newTablePanel.exportPanel();

		// Before apply, nothing is selected (or defaults are selected)
		// Apply preferences
		EntityTablePanelPreferences loadedPreferences = new EntityTablePanelPreferences(newTablePanel, prefs);
		loadedPreferences.apply(newTablePanel);

		// Verify only COMMISSION is selected
		Enumeration<TreeNode> newChildren = newExportPanel.tableExport().entityNode().children();
		while (newChildren.hasMoreElements()) {
			AttributeNode node = (AttributeNode) newChildren.nextElement();
			if (node.definition().attribute().equals(Employee.COMMISSION)) {
				assertTrue(node.selected().is(), "COMMISSION should be selected");
			}
			else {
				assertFalse(node.selected().is(), node.definition().attribute().name() + " should not be selected");
			}
		}
	}

	@Test
	void exportPreferencesForeignKeyChildren() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportPanel exportPanel = tablePanel.exportPanel();

		// Select only the master foreign key and some of its children
		Enumeration<TreeNode> children = exportPanel.tableExport().entityNode().children();
		while (children.hasMoreElements()) {
			AttributeNode node = (AttributeNode) children.nextElement();
			node.selected().set(false);
			if (node.definition().attribute().equals(Detail.MASTER_FK)) {
				node.selected().set(true);
				// Select only DEPARTMENT_FK from Employee
				Enumeration<TreeNode> empChildren = node.children();
				while (empChildren.hasMoreElements()) {
					AttributeNode empNode = (AttributeNode) empChildren.nextElement();
					if (empNode.definition().attribute().equals(Employee.DEPARTMENT_FK)) {
						empNode.selected().set(true);
						// Select NAME from Department
						Enumeration<TreeNode> deptChildren = empNode.children();
						while (deptChildren.hasMoreElements()) {
							AttributeNode deptNode = (AttributeNode) deptChildren.nextElement();
							if (deptNode.definition().attribute().equals(Department.NAME)) {
								deptNode.selected().set(true);
							}
						}
					}
				}
			}
		}

		// Save and reload
		EntityTablePanelPreferences preferences = new EntityTablePanelPreferences(tablePanel);
		Preferences prefs = Preferences.userNodeForPackage(getClass()).node("exportPreferencesForeignKeyChildren");
		preferences.save(prefs);

		// Create new panel and apply
		SwingEntityTableModel newTableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel newTablePanel = new EntityTablePanel(newTableModel, config ->  config.includeExport(true));
		EntityTablePanelPreferences loadedPreferences = new EntityTablePanelPreferences(newTablePanel, prefs);
		loadedPreferences.apply(newTablePanel);

		// Verify the nested structure
		EntityTableExportPanel newExportPanel = newTablePanel.exportPanel();
		Enumeration<TreeNode> newChildren = newExportPanel.tableExport().entityNode().children();
		while (newChildren.hasMoreElements()) {
			AttributeNode node = (AttributeNode) newChildren.nextElement();
			if (node.definition().attribute().equals(Detail.MASTER_FK)) {
				assertTrue(node.selected().is(), "MASTER_FK should be selected");
				Enumeration<TreeNode> empChildren = node.children();
				while (empChildren.hasMoreElements()) {
					AttributeNode empNode = (AttributeNode) empChildren.nextElement();
					if (empNode.definition().attribute().equals(Employee.DEPARTMENT_FK)) {
						assertTrue(empNode.selected().is(), "DEPARTMENT_FK should be selected");
						Enumeration<TreeNode> deptChildren = empNode.children();
						while (deptChildren.hasMoreElements()) {
							AttributeNode deptNode = (AttributeNode) deptChildren.nextElement();
							if (deptNode.definition().attribute().equals(Department.NAME)) {
								assertTrue(deptNode.selected().is(), "Department NAME should be selected");
							}
							else {
								assertFalse(deptNode.selected().is(), deptNode.definition().attribute().name() + " should not be selected");
							}
						}
					}
					else {
						assertFalse(empNode.selected().is(), empNode.definition().attribute().name() + " should not be selected");
					}
				}
			}
			else {
				assertFalse(node.selected().is(), node.definition().attribute().name() + " should not be selected");
			}
		}
	}

	@Test
	void cyclicalForeignKeyExpansion() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportPanel exportPanel = tablePanel.exportPanel();

		// Find the MGR_FK node (cyclical self-reference)
		AttributeNode mgrNode = null;
		Enumeration<TreeNode> children = exportPanel.tableExport().entityNode().children();
		while (children.hasMoreElements()) {
			AttributeNode node = (AttributeNode) children.nextElement();
			if (node.definition().attribute().equals(Employee.MGR_FK)) {
				mgrNode = node;
				break;
			}
		}

		assertNotNull(mgrNode, "MGR_FK node should exist");
		assertTrue(mgrNode.isCyclicalStub(), "MGR_FK should be marked as cyclical stub");
		assertEquals(0, mgrNode.getChildCount(), "Cyclical stub should have no children initially");
		assertFalse(mgrNode.isLeaf(), "Cyclical stub should not be a leaf (to show expand icon)");

		// Expand the cyclical stub
		mgrNode.expand();

		// After expansion, should have children
		assertTrue(mgrNode.getChildCount() > 0, "After expansion should have children");

		// Find the nested MGR_FK (manager's manager)
		AttributeNode nestedMgrNode = null;
		Enumeration<TreeNode> mgrChildren = mgrNode.children();
		while (mgrChildren.hasMoreElements()) {
			TreeNode child = mgrChildren.nextElement();
			if (child instanceof AttributeNode) {
				AttributeNode childNode = (AttributeNode) child;
				if (childNode.definition().attribute().equals(Employee.MGR_FK)) {
					nestedMgrNode = childNode;
					break;
				}
			}
		}

		assertNotNull(nestedMgrNode, "Nested MGR_FK should exist after expansion");
		assertTrue(nestedMgrNode.isCyclicalStub(), "Nested MGR_FK should also be cyclical stub");
		assertEquals(0, nestedMgrNode.getChildCount(), "Nested cyclical stub should have no children initially");

		// Expand again (manager's manager's manager)
		nestedMgrNode.expand();
		assertTrue(nestedMgrNode.getChildCount() > 0, "After second expansion should have children");
	}

	@Test
	void cyclicalForeignKeyPreferences() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportPanel exportPanel = tablePanel.exportPanel();

		// Deselect all
		Enumeration<TreeNode> children = exportPanel.tableExport().entityNode().children();
		while (children.hasMoreElements()) {
			AttributeNode node = (AttributeNode) children.nextElement();
			node.selected().set(false);
		}

		// Find and expand MGR_FK, select NAME from both levels
		children = exportPanel.tableExport().entityNode().children();
		while (children.hasMoreElements()) {
			AttributeNode node = (AttributeNode) children.nextElement();
			if (node.definition().attribute().equals(Employee.MGR_FK)) {
				node.selected().set(true);
				// Expand
				node.expand();
				// Select NAME from manager
				Enumeration<TreeNode> mgrChildren = node.children();
				while (mgrChildren.hasMoreElements()) {
					TreeNode child = mgrChildren.nextElement();
					if (child instanceof AttributeNode) {
						AttributeNode childNode = (AttributeNode) child;
						if (childNode.definition().attribute().equals(Employee.NAME)) {
							childNode.selected().set(true);
						}
						// Expand nested MGR_FK
						if (childNode.definition().attribute().equals(Employee.MGR_FK)) {
							childNode.selected().set(true);
							childNode.expand();
							// Select NAME from manager's manager
							Enumeration<TreeNode> nestedChildren = childNode.children();
							while (nestedChildren.hasMoreElements()) {
								TreeNode nestedChild = nestedChildren.nextElement();
								if (nestedChild instanceof AttributeNode) {
									AttributeNode nestedNode = (AttributeNode) nestedChild;
									if (nestedNode.definition().attribute().equals(Employee.NAME)) {
										nestedNode.selected().set(true);
									}
								}
							}
						}
					}
				}
				break;
			}
		}

		// Save preferences
		EntityTablePanelPreferences preferences = new EntityTablePanelPreferences(tablePanel);
		Preferences prefs = Preferences.userNodeForPackage(getClass()).node("cyclicalForeignKeyPreferences");
		preferences.save(prefs);

		String exportJson = prefs.get(tablePanel.preferencesKey() + "-export", "");
		// Should have nested mgr_fk structure
		assertTrue(exportJson.contains("\"mgr_fk\""), "Should contain mgr_fk");
		assertTrue(exportJson.contains("\"ename\""), "Should contain ename (NAME)");

		// Create new panel and apply preferences
		SwingEntityTableModel newTableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityTablePanel newTablePanel = new EntityTablePanel(newTableModel, config -> config.includeExport(true));
		EntityTablePanelPreferences loadedPreferences = new EntityTablePanelPreferences(newTablePanel, prefs);
		loadedPreferences.apply(newTablePanel);

		// Verify the cyclical FK was expanded and selections applied
		EntityTableExportPanel newExportPanel = newTablePanel.exportPanel();
		Enumeration<TreeNode> newChildren = newExportPanel.tableExport().entityNode().children();
		AttributeNode newMgrNode = null;
		while (newChildren.hasMoreElements()) {
			AttributeNode node = (AttributeNode) newChildren.nextElement();
			if (node.definition().attribute().equals(Employee.MGR_FK)) {
				newMgrNode = node;
				break;
			}
		}

		assertNotNull(newMgrNode, "MGR_FK should exist");
		assertTrue(newMgrNode.selected().is(), "MGR_FK should be selected");
		assertTrue(newMgrNode.getChildCount() > 0, "MGR_FK should have been expanded");

		// Check nested structure
		AttributeNode nameNode = null;
		AttributeNode nestedMgrNode = null;
		Enumeration<TreeNode> mgrChildren = newMgrNode.children();
		while (mgrChildren.hasMoreElements()) {
			TreeNode child = mgrChildren.nextElement();
			if (child instanceof AttributeNode) {
				AttributeNode childNode = (AttributeNode) child;
				if (childNode.definition().attribute().equals(Employee.NAME)) {
					nameNode = childNode;
				}
				if (childNode.definition().attribute().equals(Employee.MGR_FK)) {
					nestedMgrNode = childNode;
				}
			}
		}

		assertNotNull(nameNode, "NAME should exist in expanded MGR_FK");
		assertTrue(nameNode.selected().is(), "NAME should be selected");
		assertNotNull(nestedMgrNode, "Nested MGR_FK should exist");
		assertTrue(nestedMgrNode.selected().is(), "Nested MGR_FK should be selected");
		assertTrue(nestedMgrNode.getChildCount() > 0, "Nested MGR_FK should have been expanded");
	}
}
