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

import is.codion.common.i18n.Messages;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableExport.AttributeNode;
import is.codion.swing.framework.ui.EntityTableExport.ExportTask;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ToggleButtonType.RADIO_BUTTON;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingConstants.CENTER;

final class EntityTableExportPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String ATTRIBUTES_KEY = "attributes";
	private static final String FOREIGN_KEYS_KEY = "foreignKeys";

	private final EntityTableExport tableExport;
	private final FilterTableColumnModel<Attribute<?>> columnModel;
	private final JTree exportTree;
	private final DefaultTreeModel treeModel;

	private final CommandControl selectDefaultsControl = Control.builder()
					.command(this::selectDefaults)
					.caption(MESSAGES.getString("default_columns"))
					.mnemonic(MESSAGES.getString("default_columns_mnemonic").charAt(0))
					.build();
	private final CommandControl selectAllControl = Control.builder()
					.command(() -> select(true))
					.caption(MESSAGES.getString("columns_all"))
					.mnemonic(MESSAGES.getString("columns_all_mnemonic").charAt(0))
					.build();
	private final CommandControl selectNoneControl = Control.builder()
					.command(() -> select(false))
					.caption(MESSAGES.getString("columns_none"))
					.mnemonic(MESSAGES.getString("columns_none_mnemonic").charAt(0))
					.build();
	private final ToggleControl allRowsControl;
	private final ToggleControl selectedRowsControl;

	EntityTableExportPanel(SwingEntityTableModel tableModel, FilterTableColumnModel<Attribute<?>> columnModel) {
		super(borderLayout());
		this.tableExport = new EntityTableExport(tableModel);
		this.columnModel = columnModel;
		this.treeModel = new DefaultTreeModel(tableExport.entityNode());
		this.exportTree = createTree();
		this.selectedRowsControl = Control.builder()
						.toggle(tableExport.selected())
						.caption(MESSAGES.getString("rows_selected"))
						.mnemonic(MESSAGES.getString("rows_selected_mnemonic").charAt(0))
						.enabled(tableModel.selection().empty().not())
						.build();
		this.allRowsControl = Control.builder()
						.toggle(State.state(!tableExport.selected().is()))
						.caption(MESSAGES.getString("rows_all"))
						.mnemonic(MESSAGES.getString("rows_all_mnemonic").charAt(0))
						.build();
		add(borderLayoutPanel()
						.border(emptyBorder())
						.center(borderLayoutPanel()
										.border(createTitledBorder(MESSAGES.getString("columns")))
										.center(scrollPane()
														.view(exportTree))
										.south(borderLayoutPanel()
														.center(stringField()
																		.horizontalAlignment(CENTER)
																		.value(MESSAGES.getString("help"))
																		.enabled(false))
														.south(borderLayoutPanel()
																		.east(buttonPanel()
																						.controls(Controls.builder()
																										.actions(selectDefaultsControl, selectAllControl, selectNoneControl))
																						.transferFocusOnEnter(true)))))
						.south(borderLayoutPanel()
										.border(createTitledBorder(MESSAGES.getString("rows")))
										.center(borderLayoutPanel()
														.east(buttonPanel()
																		.controls(Controls.builder()
																						.actions(allRowsControl, selectedRowsControl))
																		.toggleButtonType(RADIO_BUTTON)
																		.buttonGroup(new ButtonGroup())
																		.fixedButtonSize(false)
																		.transferFocusOnEnter(true))))
						.build(), BorderLayout.CENTER);
	}

	void export(JComponent dialogOwner) {
		Dialogs.action()
						.component(this)
						.owner(dialogOwner)
						.title(Messages.copy())
						.escapeAction(Control.builder()
										.command(() -> parentWindow(this).dispose())
										.caption(MESSAGES.getString("close"))
										.mnemonic(MESSAGES.getString("close_mnemonic").charAt(0))
										.build())
						.action(Control.builder()
										.command(this::exportToClipboard)
										.caption(MESSAGES.getString("to_clipboard"))
										.mnemonic(MESSAGES.getString("to_clipboard_mnemonic").charAt(0))
										.build())
						.action(Control.builder()
										.command(this::exportToFile)
										.caption(MESSAGES.getString("to_file") + "...")
										.mnemonic(MESSAGES.getString("to_file_mnemonic").charAt(0))
										.build())
						.show();
	}

	EntityTableExport tableExport() {
		return tableExport;
	}

	void selectDefaults() {
		select(false);
		Enumeration<TreeNode> children = tableExport.entityNode().children();
		while (children.hasMoreElements()) {
			TreeNode child = children.nextElement();
			if (child instanceof AttributeNode) {
				Attribute<?> attribute = ((AttributeNode) child).definition().attribute();
				if (columnModel.contains(attribute) && columnModel.visible(attribute).is()) {
					((AttributeNode) child).selected().set(true);
				}
			}
		}
	}

	private void exportToFile() {
		ExportTask task = tableExport.exportToFile(Dialogs.select()
						.files()
						.owner(this)
						.selectFileToSave(tableExport.defaultFileName())
						.toPath());
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title(MESSAGES.getString("copying_data"))
						.control(createCancelControl(task.cancelled()))
						.onResult(MESSAGES.getString("data_copied"), task.successMessage())
						.execute();
	}

	private void exportToClipboard() {
		ExportTask task = tableExport.exportToClipboard();
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title(MESSAGES.getString("copying_data"))
						.control(createCancelControl(task.cancelled()))
						.onResult(MESSAGES.getString("data_copied"), task.successMessage())
						.execute();
	}

	private static Control createCancelControl(State cancelled) {
		return Control.builder()
						.toggle(cancelled)
						.caption(Messages.cancel())
						.mnemonic(Messages.cancelMnemonic())
						.enabled(cancelled.not())
						.build();
	}

	private JTree createTree() {
		JTree tree = new JTree(treeModel);
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		tree.addTreeWillExpandListener(new ExpandListener());
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isAltDown()) {
					toggleSelected();
				}
			}
		});
		KeyEvents.builder()
						.keyCode(VK_SPACE)
						.action(command(this::toggleSelected))
						.enable(tree);

		return tree;
	}

	private void select(boolean select) {
		Enumeration<TreeNode> enumeration = tableExport.entityNode().breadthFirstEnumeration();
		enumeration.nextElement();// root
		while (enumeration.hasMoreElements()) {
			((AttributeNode) enumeration.nextElement()).selected().set(select);
		}
		exportTree.repaint();
	}

	private void toggleSelected() {
		TreePath[] selectionPaths = exportTree.getSelectionPaths();
		if (selectionPaths != null) {
			Stream.of(selectionPaths)
							.filter(exportTree::isPathSelected)
							.map(TreePath::getLastPathComponent)
							.map(AttributeNode.class::cast)
							.forEach(node -> node.selected().toggle());
			exportTree.repaint();
		}
	}

	private void expandToShowSelections() {
		expandNodeIfHasSelectedChildren(tableExport.entityNode());
	}

	private boolean expandNodeIfHasSelectedChildren(TreeNode node) {
		boolean hasSelectedDescendants = false;
		for (int i = 0; i < node.getChildCount(); i++) {
			TreeNode child = node.getChildAt(i);
			if (child instanceof AttributeNode) {
				AttributeNode attrNode = (AttributeNode) child;
				if (attrNode.selected().is()) {
					hasSelectedDescendants = true;
				}
				if (expandNodeIfHasSelectedChildren(child)) {
					hasSelectedDescendants = true;
					exportTree.expandPath(new TreePath(attrNode.getPath()));
				}
			}
		}

		return hasSelectedDescendants;
	}

	private JSONObject createPreferences() {
		return attributesToJson(tableExport.entityNode().children());
	}

	private void applyPreferences(JSONObject preferences) {
		if (preferences.isEmpty()) {
			selectDefaults();
		}
		else {
			applyAttributesAndForeignKeys(preferences, tableExport.entityNode().children());
			exportTree.repaint();
			expandToShowSelections();
		}
	}

	private static JSONObject attributesToJson(Enumeration<TreeNode> nodes) {
		JSONArray attributes = new JSONArray();
		JSONObject foreignKeys = new JSONObject();

		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String attributeName = node.definition().attribute().name();
			boolean isForeignKey = node.getChildCount() > 0;

			if (isForeignKey) {
				JSONObject fkChildren = attributesToJson(node.children());
				if (!node.selected().is() && fkChildren.isEmpty()) {
					continue;
				}
				if (node.selected().is()) {
					attributes.put(attributeName);
				}
				if (!fkChildren.isEmpty()) {
					foreignKeys.put(attributeName, fkChildren);
				}
			}
			else if (node.selected().is()) {
				attributes.put(attributeName);
			}
		}

		JSONObject result = new JSONObject();
		if (!attributes.isEmpty()) {
			result.put(ATTRIBUTES_KEY, attributes);
		}
		if (!foreignKeys.isEmpty()) {
			result.put(FOREIGN_KEYS_KEY, foreignKeys);
		}

		return result;
	}

	private void applyAttributesAndForeignKeys(JSONObject json, Enumeration<TreeNode> nodes) {
		Set<String> selectedAttributes = new HashSet<>();
		if (json.has(ATTRIBUTES_KEY)) {
			JSONArray attributes = json.getJSONArray(ATTRIBUTES_KEY);
			for (int i = 0; i < attributes.length(); i++) {
				selectedAttributes.add(attributes.getString(i));
			}
		}

		JSONObject foreignKeys = json.has(FOREIGN_KEYS_KEY) ? json.getJSONObject(FOREIGN_KEYS_KEY) : new JSONObject();
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String attributeName = node.definition().attribute().name();
			boolean isForeignKey = node.getChildCount() > 0 || node.isCyclicalStub();

			node.selected().set(selectedAttributes.contains(attributeName));

			if (isForeignKey && foreignKeys.has(attributeName)) {
				if (node.isCyclicalStub()) {
					node.expand();
					treeModel.nodeStructureChanged(node);
				}
				applyAttributesAndForeignKeys(foreignKeys.getJSONObject(attributeName), node.children());
			}
			else if (isForeignKey) {
				deselectAll(node.children());
			}
		}
	}

	private static void deselectAll(Enumeration<TreeNode> nodes) {
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			node.selected().set(false);
			if (node.getChildCount() > 0) {
				deselectAll(node.children());
			}
		}
	}

	private boolean isDefaultConfiguration() {
		Enumeration<TreeNode> children = tableExport.entityNode().children();

		while (children.hasMoreElements()) {
			TreeNode child = children.nextElement();
			if (child instanceof AttributeNode) {
				AttributeNode node = (AttributeNode) child;
				Attribute<?> attribute = node.definition().attribute();
				// Default: first-level visible columns are selected, hidden are not
				boolean shouldBeSelected = columnModel.contains(attribute) && columnModel.visible(attribute).is();
				if (node.selected().is() != shouldBeSelected) {
					return false;
				}
				if (hasSelectedChildren(node)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean hasSelectedChildren(AttributeNode node) {
		Enumeration<TreeNode> children = node.children();
		while (children.hasMoreElements()) {
			TreeNode child = children.nextElement();
			if (child instanceof AttributeNode) {
				AttributeNode childNode = (AttributeNode) child;
				if (childNode.selected().is()) {
					return true;
				}
				if (hasSelectedChildren(childNode)) {
					return true;
				}
			}
		}

		return false;
	}

	private final class ExpandListener implements TreeWillExpandListener {
		@Override
		public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
			Object node = event.getPath().getLastPathComponent();
			if (node instanceof AttributeNode) {
				AttributeNode attributeNode = (AttributeNode) node;
				if (attributeNode.isCyclicalStub() && attributeNode.getChildCount() == 0) {
					attributeNode.expand();
					treeModel.nodeStructureChanged(attributeNode);
				}
			}
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent event) {}
	}

	static final class ExportPreferences {

		private final JSONObject preferences;

		ExportPreferences(String preferencesString) {
			this(new JSONObject(preferencesString));
		}

		private ExportPreferences(JSONObject preferences) {
			this.preferences = preferences;
		}

		ExportPreferences(EntityTableExportPanel exportPanel) {
			if (exportPanel.isDefaultConfiguration()) {
				this.preferences = new JSONObject("{}");
			}
			else {
				this.preferences = exportPanel.createPreferences();
			}
		}

		void apply(EntityTableExportPanel exportPanel) {
			exportPanel.applyPreferences(preferences);
		}

		JSONObject preferences() {
			return preferences;
		}
	}
}
